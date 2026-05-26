## 1. pgvector extension
- [ ] 1.1 Liquibase changeset `db.changelog-genba-semantic-001-extensions.xml` running BEFORE any other changeset that uses `VECTOR`. Idempotent: `CREATE EXTENSION IF NOT EXISTS vector;`.
- [ ] 1.2 Reorder `db.changelog-master.xml` so the extensions changeset is the FIRST include (before all auth-foundation and feature-specific changesets).
- [ ] 1.3 docker-compose image swap from `postgres:15-alpine` to `pgvector/pgvector:pg15`. Verify with `psql -c "SELECT extversion FROM pg_extension WHERE extname='vector';"`.

## 2. EmbeddingProvider interface + impls
- [ ] 2.1 `eu.px.genba.semantic.provider.EmbeddingProvider` — interface methods: `float[] embed(String text)`, `List<float[]> embedBatch(List<String> texts)`, `int dimensions()`, `String modelId()`, `boolean isHealthy()`.
- [ ] 2.2 `OllamaEmbeddingProvider` — `@ConditionalOnProperty(prefix="genba.embeddings", name="provider", havingValue="local", matchIfMissing=true)`. HTTP client via Spring's `RestClient`; URL from `genba.embeddings.local.url`, model from `genba.embeddings.local.model` (default `bge-m3`). `isHealthy()` = HTTP HEAD on Ollama `/api/tags` returns 200.
- [ ] 2.3 `VoyageEmbeddingProvider` — `@ConditionalOnProperty(...,  havingValue="voyage")`. URL `https://api.voyageai.com/v1/embeddings`, model `voyage-multilingual-2`, auth header from `VOYAGE_API_KEY`. `isHealthy()` = key present + 200 from a 1-token probe call (cached for 5 min).
- [ ] 2.4 `OpenAIEmbeddingProvider` — `@ConditionalOnProperty(..., havingValue="openai")`. URL `https://api.openai.com/v1/embeddings`, model `text-embedding-3-large`, request body includes `"dimensions": 1024` so we get 1024-d native (Matryoshka truncation). Auth from `OPENAI_API_KEY`.
- [ ] 2.5 Fallback `UnavailableEmbeddingProvider` — registered when the active provider fails initialization at startup. Throws `EmbeddingsInactiveException` on any call; `isHealthy()` returns false. Logged on startup with the reason.
- [ ] 2.6 Integration tests: MockMvc-style WireMock server for Voyage/OpenAI; Ollama integration test gated by `@EnabledIfEnvironmentVariable(named="OLLAMA_AVAILABLE", matches="true")`.

## 3. embedding_model_log + worker
- [ ] 3.1 Liquibase changeset `db.changelog-genba-semantic-002-embedding-model-log.xml`: `embedding_model_log` table — `{id UUID PK, table_name VARCHAR(64) NOT NULL, row_id UUID NOT NULL, model_id VARCHAR(128) NOT NULL, embedded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()}`, UNIQUE (table_name, row_id) so we keep only the latest provenance.
- [ ] 3.2 `eu.px.genba.semantic.worker.EmbeddingWorker` — `@Scheduled(fixedDelayString="${genba.embeddings.worker.interval-ms:60000}")` Spring bean. Each tick:
  - Short-circuit if provider not healthy.
  - For each embeddable table {attachment, offer_line, vendor, wbs_item, wbs_template_item}, find up to `genba.embeddings.worker.batch-size` (default 50) rows where `embedding_source_hash IS NULL` OR `embedding_source_hash != sha256(source_text(row))` OR the row's `embedding_model_log.model_id` differs from the current provider's `modelId()`.
  - Compute `source_text` per table (e.g., `attachment.raw_text || ''`; `offer_line.label || ' ' || coalesce(description,'')`; `vendor.name || ' ' || coalesce(notes,'')`; `wbs_item.name`; `wbs_template_item.name_en || ' / ' || name_ro`).
  - Call `provider.embedBatch(sourceTexts)`.
  - Update `embedding` + `embedding_source_hash` in one transactional batch; upsert `embedding_model_log` per row.
  - Log throughput, errors, and skipped-because-unhealthy events.
- [ ] 3.3 `SourceTextResolver` strategy interface — one impl per embeddable table — used by the worker AND search to compute the canonical source text.
- [ ] 3.4 Configurable `genba.embeddings.worker.enabled` (default true; setting false stops the worker for maintenance).
- [ ] 3.5 Manual `POST /api/admin/embeddings/reembed?table=...&dryRun=...` endpoint that re-marks all rows in the table as dirty by clearing their `embedding_source_hash` (org-admin only; protected).

## 4. SemanticSearchService
- [ ] 4.1 `eu.px.genba.semantic.search.SemanticSearchService` exposing:
  - `searchAttachments(query, orgId, projectId?, kind?, topK)` → `List<AttachmentMatch>` where `AttachmentMatch = { attachment, cosineSimilarity, snippet }`.
  - `findSimilarOfferLines(query, orgId, projectId?, topK)` → `List<OfferLineMatch>`.
  - `findSimilarVendors(query, orgId, topK)` → `List<VendorMatch>`.
  - `findSimilarWbsItems(query, projectId, topK)` → `List<WbsItemMatch>` (works against per-project WBSItem only — templates are searched via a separate method).
  - `findSimilarWbsTemplateItems(query, orgId, topK)` → similar; also searches the system-managed templates.
- [ ] 4.2 Each method: embed the query → run `SELECT ... FROM <table> WHERE org_id = ? AND embedding IS NOT NULL ORDER BY embedding <=> ?::vector LIMIT ?`. Org-scope strictly enforced.
- [ ] 4.3 If provider is unhealthy or query returns zero rows with embeddings, return `{ status: "EMBEDDINGS_INACTIVE" or "EMPTY", results: [] }`.
- [ ] 4.4 Snippet generation for attachments: truncate `raw_text` to 240 chars around the most relevant span (simple: first 240 chars; LLM-driven highlighting deferred).
- [ ] 4.5 Integration tests with seeded embeddings (compute via a test-mode `RandomEmbeddingProvider` for deterministic vectors) covering: org-scope isolation, project-scope filter, topK truncation, unhealthy fallback.

## 5. Provider-switch migration
- [ ] 5.1 On startup, log the active provider's `modelId()` and warn if `embedding_model_log` contains rows with a different `model_id`.
- [ ] 5.2 `POST /api/admin/embeddings/switch-provider?to=<provider>` (org-admin only) — updates the property dynamically (Spring `@RefreshScope`), kicks off worker, returns expected ETA based on row count and provider throughput.
- [ ] 5.3 Document the "shadow-column" advanced migration path in design.md (add `embedding_v2` column, double-read during transition) for L1 deployments where downtime is unacceptable.

## 6. HNSW index management
- [ ] 6.1 `IndexAdminService.buildHnswIndex(table)` runs `CREATE INDEX CONCURRENTLY <name> ON <table> USING hnsw (embedding vector_cosine_ops)` if not already present. Idempotent; logged.
- [ ] 6.2 `IndexAdminService.indexStats(table)` returns `{ totalRows, embeddedRows, dirtyRows, hnswIndexPresent, hnswIndexSize }`.
- [ ] 6.3 Settings page surfaces a "Build HNSW index" button per table once `embeddedRows >= genba.embeddings.index-threshold` (default 1000).
- [ ] 6.4 Endpoint `GET /api/admin/embeddings/stats` returns per-table indexStats.

## 7. Admin UI
- [ ] 7.1 `app/(app)/settings/embeddings/page.tsx` — shows provider config (read-only env-based), health, per-table indexing stats, "Re-embed all" + "Build HNSW index" buttons (with confirmation modal).
- [ ] 7.2 GDPR warning banner when active provider is `voyage` or `openai` ("Embeddings are sent to <provider> for processing. Data leaves EU. See compliance docs.").
- [ ] 7.3 Real-time status via TanStack Query polling `/api/admin/embeddings/stats` every 5s while page is open.

## 8. Documentation
- [ ] 8.1 `docs/embeddings.md` — provider setup (Ollama install, Voyage signup, OpenAI key), GDPR considerations, throughput expectations.
- [ ] 8.2 `docs/ollama-l1-deployment.md` — running Ollama on Hetzner (CPU vs L4 GPU instance options, cost comparison, performance characteristics).
- [ ] 8.3 README quick-start mentions optional `ollama pull bge-m3` step.

## 9. Verification + spec migration
- [ ] 9.1 TestContainers integration: bring up `pgvector/pgvector:pg15`, run migrations, write Attachment with raw_text, run worker tick (forced), verify embedding populated, run SemanticSearchService.searchAttachments, verify result.
- [ ] 9.2 Manual smoke test on local Mac with Ollama running.
- [ ] 9.3 Manual smoke test with provider deliberately misconfigured → app boots → MCP search tool returns EMBEDDINGS_INACTIVE.
- [ ] 9.4 Move spec deltas to `openspec/specs/semantic-search/spec.md` after merge.
- [ ] 9.5 `openspec validate add-semantic-search --strict` passes.
- [ ] 9.6 Archive after merge.
