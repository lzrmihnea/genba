## Context
The Phase 1+2 schema scaffolded `embedding VECTOR(1024) NULL` columns on five entities (Attachment, OfferLine, Vendor, WBSItem, WBSTemplateItem). This change makes them live. Three decisions deserve documentation because they have long-term consequences: (1) the provider abstraction and the graceful-unavailable design; (2) the dirty-detection mechanism; (3) the provider-switch migration story.

## Goals / Non-Goals

**Goals**
- Pluggable embedding provider, default to local Ollama for L0 + GDPR-safe L1.
- Zero-coupling between embeddings and the rest of the system: if no provider is configured/healthy, nothing breaks; everything that doesn't need embeddings continues to work.
- Crowd-pleasing for the LLM downstream (`add-mcp-server`) — the search tools want consistent ordering + status reporting.
- Provider switches must be possible without code changes — just config + a background re-embed.

**Non-Goals**
- RAG-as-a-service (no chunking strategy beyond row-level granularity in P5).
- Cross-encoder re-ranking (defer; simple cosine-distance over rich-context embeddings is enough for the user's scale).
- Sub-row chunking for long Attachments (deferred to a future `add-attachment-chunking` if PDF OCR makes this necessary).
- LLM-driven snippet highlighting (deferred; use first-N-chars heuristic).
- Multi-vector models (ColBERT-style) — too much complexity for the marginal gain at L0/L1 scale.

## Decisions

**Decision: 1024-dim vectors across all providers (user-confirmed).**
- Voyage `voyage-multilingual-2`: native 1024. Match.
- OpenAI `text-embedding-3-large` with `dimensions: 1024` parameter: native 3072 truncated by Matryoshka to 1024. Match.
- Ollama `bge-m3`: native 1024. Match.
- Consequence: any provider can populate the column natively; no padding/up-projection needed.

**Decision: Default provider is `local`, with `matchIfMissing=true` on the conditional.**
- Rationale: if no `EMBEDDINGS_PROVIDER` env is set, the local provider initializes and self-checks Ollama health. If Ollama isn't running, the provider registers as `unavailable` and the system continues. Zero configuration cost for users who don't care about LLM features.

**Decision: Graceful unavailable instead of crashing.**
- Alternative: hard-fail at startup if provider is misconfigured.
- Rationale: this is an *optional* feature layered on a complete app. The user explicitly stated: "Allow the local provider to be missing, so we have nothing running on LLM side, for now." Embeddings are dormant by default; activation is a single env-var or `ollama serve`.
- Implementation: `UnavailableEmbeddingProvider` is the registered bean if the active provider's `isHealthy()` is false at startup. All call sites use the bean; the bean throws `EmbeddingsInactiveException` or returns `unhealthy` so callers (worker, search service, MCP tools) can branch gracefully.

**Decision: Dirty detection via `embedding_source_hash = SHA-256(source_text)`.**
- Alternative: per-row `embedded_at` timestamp compared to `updated_at`.
- Rationale: hash-based detection survives clock skew, batch restores, and column-changes-that-don't-affect-source-text (e.g., updating `vendor.notes` doesn't dirty-mark the embedding if `name` is the embedded column). It also survives provider switches: when the provider changes, we wipe the hash to force re-embed.
- Alternative considered: store the embedding-model-id with the hash (`embedding_source_hash + embedding_model_id`); chose to separate model_id into the `embedding_model_log` side table to keep main table widths small.

**Decision: `embedding_model_log` is a side table, not a column.**
- Alternative: an `embedding_model_id VARCHAR(128)` column on every embedded table.
- Rationale: separating provenance from data keeps main tables narrow (Attachment already has many columns), and querying "which model embedded row X" is a rare admin operation; co-locating doesn't save anything. Trade-off: an extra UPDATE per re-embed. Acceptable.

**Decision: Background worker on a fixed delay; no event-driven queueing.**
- Alternative: Spring `@TransactionalEventListener` triggering re-embed on every entity create/update.
- Rationale: simplicity. The worker batches better, smooths bursty edits, and isolates embedding failures from user-facing requests. Polling every 60s introduces at most 60s of staleness — acceptable for a search index. Event-driven could be added later as an optimization.

**Decision: HNSW index built manually, not automatically.**
- Alternative: automatic `CREATE INDEX` when row count crosses 1000.
- Rationale: index creation on a large table holds locks (even with CONCURRENTLY). Letting an admin decide *when* to take that hit is safer. Settings UI surfaces the threshold + index status; one click builds it.

**Decision: Provider switches are explicit + slow, not blue/green.**
- Alternative: shadow-column (`embedding_v2`) double-read during migration.
- Rationale: at L0/L1 scale, a slow background re-embed during a provider switch is fine — searches degrade to "no results" only for rows already wiped of their hash before they're re-embedded. Few users, transient. Shadow-column adds complexity. Documented in tasks but not implemented in this change; can be added later if SaaS scale demands it.

**Decision: Search is SQL-only, no application-layer re-ranking.**
- Alternative: post-fetch re-ranking via an LLM call ("which of these 20 results is most relevant?").
- Rationale: at the user's data scale, cosine distance over a well-trained multilingual embedding is plenty. Adding a re-rank step doubles latency and ties search latency to LLM availability. Can be layered as an MCP tool annotation later if needed.

**Decision: Per-row embedding granularity — not chunked.**
- Alternative: split long Attachment text into 512-token chunks, embed each.
- Rationale: WhatsApp pastes + email bodies + OfferLines are typically short (well under 1024 tokens for our cases). Attachment OCR-of-PDF could exceed this, but PDF OCR is a future feature; we'll add chunking when we actually have long inputs. Premature.

## Risks / Trade-offs
- **Provider lock-in (mitigated):** all three providers produce 1024-d output. Switching = re-embed only. No schema or code changes.
- **Search staleness (acceptable):** up to `worker.interval-ms` (60s default) before edits show up in semantic results. Documented; tunable.
- **Cost of hosted providers (operational):** Voyage/OpenAI charge per token. For a typical L1 user with ~5k attachments × 200 tokens avg, that's ~$0.10–0.20 per full re-embed pass. Cheap, but real. Document in pricing-tier work.
- **Local provider GPU cost (operational):** TEI / GPU instance at Hetzner ~€175/mo for L4 if interactive search is needed. CPU-only Ollama works for background indexing but search latency suffers. Mitigation: start CPU-only; upgrade only if user growth demands.
- **GDPR (legal):** hosted providers process EU personal data. Admin UI warns. DPA must be signed before L1 launch with any hosted provider option enabled. Default `local` sidesteps.

## Migration Plan
- This change can run independently of Phase 1/2 deployment status — it only ALTERs nothing; it just turns NULL columns into populated ones via the worker.
- On first deploy:
  1. Master changelog now includes extensions changeset FIRST. Liquibase runs it on next boot.
  2. `embedding_model_log` table created.
  3. Worker bean activates; if provider is local + Ollama not installed → worker logs unavailable and sleeps.
  4. No data loss possible — only NULL columns become populated.
- Rollback:
  1. Stop the worker (`genba.embeddings.worker.enabled=false`).
  2. Disable extension changeset removal is destructive (would drop the type for the column); instead, leave columns NULL.
  3. Hard rollback to before this change: `liquibase rollback-count 2` removes `embedding_model_log` table but leaves the `embedding` columns; that's fine, they're just NULL again.

## Open Questions
1. Should we add a `disable_embedding` boolean per row so an admin can explicitly opt out of embedding a sensitive Attachment? Recommend YES for L1 GDPR posture (allows per-row exclusion from any LLM-touching pipeline). Cheap to add now (one BOOLEAN column on Attachment).
2. Should `embedding_model_log` be partitioned by table_name once it grows? Recommend NO — it stays small (one row per data row) and is rarely queried.
3. Future: should we add a "reembed on save" trigger for hot-edits where the user wants instant search visibility? Recommend deferring — the 60s window is acceptable and event-driven adds complexity.
4. Should the worker run in a separate thread pool from the app's main pool? Recommend YES — `@EnableAsync` with a dedicated executor sized 2 threads.
