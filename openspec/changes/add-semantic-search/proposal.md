## Why
The Phase 1+2 schema reserves `embedding VECTOR(1024) NULL` + `embedding_source_hash` columns on Attachment, OfferLine, Vendor, WBSItem, WBSTemplateItem — but those columns stay NULL until this change activates them. This proposal introduces the embedding pipeline: a pluggable provider abstraction (local Ollama / Voyage / OpenAI behind a feature flag, default local), a graceful-unavailable mode (if the configured provider can't reach its model, the system stays healthy and embeddings remain NULL), a background worker that detects dirty rows via `embedding_source_hash` and (re)embeds them in batches, and a search service exposing hybrid SQL+vector queries. The MCP server (`add-mcp-server`) consumes this layer for semantic-search tools; the existing while-entering recommendations in `add-bid-comparison-mvp` gain optional embedding-based ordering.

## What Changes
- **ADD**: `EmbeddingProvider` interface with three Spring-managed implementations gated by `genba.embeddings.provider` ∈ {`local`, `voyage`, `openai`}; default `local`. Each impl exposes `embed(text)`, `embedBatch(texts)`, `dimensions()`, `modelId()`, `isHealthy()`.
  - `OllamaEmbeddingProvider` → HTTP to `${OLLAMA_URL:http://localhost:11434}/api/embeddings`, model default `bge-m3`.
  - `VoyageEmbeddingProvider` → HTTPS to `https://api.voyageai.com/v1/embeddings`, model `voyage-multilingual-2` (recommended for RO + multilingual workloads).
  - `OpenAIEmbeddingProvider` → HTTPS to `https://api.openai.com/v1/embeddings`, model `text-embedding-3-large` requested with `dimensions: 1024` truncation parameter.
- **ADD**: Graceful unavailable. If the configured provider is unreachable or misconfigured at startup, the provider bean self-registers as `unavailable`. The system does NOT crash. The background worker no-ops. MCP semantic-search tools return `{ status: "EMBEDDINGS_INACTIVE", reason: "..." }`. All non-semantic features (CRUD, bid-compare via MatchGroup, label-equality recommendations) work untouched.
- **ADD**: `EmbeddingWorker` — Spring `@Scheduled(fixedDelayString = "${genba.embeddings.worker.interval-ms:60000}")` bean. Each tick:
  1. Skip if provider not healthy.
  2. Query up to N rows across embeddable tables where `embedding_source_hash IS NULL OR embedding_source_hash != sha256(currentSourceText)`.
  3. Batch them per table (max batch per provider call), call `embedBatch`.
  4. Update `embedding` + `embedding_source_hash` in a single UPDATE.
  5. Log throughput + errors.
- **ADD**: `SemanticSearchService` exposing:
  - `searchAttachments(query: String, orgId, projectId?, topK: int): List<AttachmentMatch>`
  - `findSimilarOfferLines(query: String, orgId, projectId?, topK: int): List<OfferLineMatch>`
  - `findSimilarVendors(query, orgId, topK)`
  - `findSimilarWbsItems(query, projectId, topK)` (Phase 2-aware)
  Each method embeds the query via the configured provider, then runs a SQL using pgvector's `<=>` (cosine distance) operator scoped by `org_id`, with `LIMIT topK`. Falls back to label-equality search when embeddings are inactive.
- **ADD**: Hybrid index strategy. Tables stay scan-only until row count exceeds `genba.embeddings.index-threshold` (default 1000); above that, an admin endpoint creates an HNSW index via online `CREATE INDEX ... CONCURRENTLY USING hnsw (embedding vector_cosine_ops)`. Index creation is rare and triggered manually to avoid disruptive automatic DDL.
- **ADD**: Provider-switch migration playbook. When `genba.embeddings.provider` changes between deployments: the worker detects that existing rows' `embedding_model_id` (cached in a side table) no longer matches the active provider's `modelId()`, marks them dirty, and re-embeds. Optionally during transition, a temporary `embedding_v2 VECTOR(1024) NULL` column lets searches double-read until backfill completes — but the simpler approach (downtime for the dirty-mark + slow re-embed) is acceptable for L0 and small L1 deployments.
- **ADD**: `embedding_model_log` table — `{id, table_name, row_id, model_id, embedded_at}` — last-seen embedding provenance per row. Used by the worker to detect model changes and trigger re-embed.
- **ADD**: Admin UI under `/settings/embeddings` showing: configured provider, health status, per-table indexing stats (rows total / embedded / dirty), manual "Re-embed all" action, manual "Build HNSW index" action.
- **ADD**: Liquibase changeset `db.changelog-genba-semantic-001-extensions.xml` enabling `CREATE EXTENSION IF NOT EXISTS vector` (idempotent; runs FIRST in the master changelog).
- **NO**: any change to the existing `add-bid-comparison-mvp` while-entering recommendations behavior. They continue to use label-equality + MatchGroup as primary sources; the SemanticSearchService becomes available as an *optional* tie-breaker.

## Impact
- **Affected specs**: NEW capability `semantic-search`. The other capabilities (`genba-core`, `vendors-and-offers`, `bid-comparison`, `wbs-catalog`) are NOT modified — they reserved the columns; this change fills them.
- **Affected code**:
  - New backend package: `eu.px.genba.semantic.{provider, worker, search, admin}`.
  - New Liquibase: `db.changelog-genba-semantic-001-extensions.xml` (first thing in master), `-002-embedding-model-log.xml`.
  - New Settings page in frontend: `app/(app)/settings/embeddings/page.tsx`.
- **Risk**: provider switches require re-embedding all rows. Worker throughput limits time-to-consistency. Mitigation: documented migration playbook; admin status page shows progress.
- **Risk**: HTTP latency to Ollama on first cold start (~1-3s for model load). Mitigation: worker warms it on first cycle; user-facing search waits at most one batch.
- **Risk**: secret-management for `VOYAGE_API_KEY` / `OPENAI_API_KEY` — stored in env vars, never in DB; admin UI shows only provider+model+health (not the key).
- **GDPR**: when `voyage` or `openai` is selected, contractor PII in attachments crosses into the chosen vendor's processing. Admin UI displays an explicit "Data leaves EU" warning when a hosted provider is chosen. Default `local` keeps PII on-prem.
