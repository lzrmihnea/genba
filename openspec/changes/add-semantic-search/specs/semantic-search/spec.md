## ADDED Requirements

### Requirement: Pluggable EmbeddingProvider
The system SHALL provide a Spring-managed `EmbeddingProvider` abstraction with three implementations gated by `genba.embeddings.provider` ∈ {`local`, `voyage`, `openai`}; default `local`. Each provider SHALL produce 1024-dimensional vectors regardless of the chosen backend so that the `embedding VECTOR(1024)` columns are populated uniformly.

#### Scenario: Local Ollama provider initializes when reachable
- **WHEN** the system boots with `genba.embeddings.provider=local`, `genba.embeddings.local.url=http://localhost:11434`, `genba.embeddings.local.model=bge-m3`, AND Ollama is reachable and the model is available
- **THEN** the `OllamaEmbeddingProvider` bean SHALL register as healthy; subsequent `embed(text)` calls return 1024-d vectors; `modelId()` returns `bge-m3@<ollama-version>`

#### Scenario: Voyage provider with voyage-multilingual-2
- **WHEN** the system boots with `genba.embeddings.provider=voyage` and `VOYAGE_API_KEY` set
- **THEN** the `VoyageEmbeddingProvider` SHALL initialize; calls produce 1024-d vectors from model `voyage-multilingual-2`

#### Scenario: OpenAI provider with Matryoshka truncation
- **WHEN** the system boots with `genba.embeddings.provider=openai` and `OPENAI_API_KEY` set
- **THEN** the `OpenAIEmbeddingProvider` SHALL request `text-embedding-3-large` with `dimensions: 1024`; calls produce 1024-d vectors (Matryoshka-truncated from the model's native 3072)

### Requirement: Graceful Unavailable
If the configured provider is misconfigured, unreachable, or its model is missing at startup, the system MUST NOT crash. Instead, the registered bean SHALL self-report `isHealthy() = false`, the background worker SHALL no-op, and any caller that requires embeddings SHALL receive a typed unavailable response rather than an exception propagating to the user.

#### Scenario: Local provider missing Ollama
- **WHEN** `genba.embeddings.provider=local` (default) but Ollama is not running on the configured URL
- **THEN** the system SHALL log a single startup warning ("local embedding provider unhealthy: Ollama unreachable at <url>"); the application SHALL continue to boot; the worker SHALL NOT throw on its `@Scheduled` ticks

#### Scenario: MCP search tools observe inactive state
- **WHEN** an MCP semantic-search tool (`search_attachments`, `find_similar_offer_lines`, ...) is called while the active provider is unhealthy
- **THEN** the tool SHALL return `{ status: "EMBEDDINGS_INACTIVE", reason: "<short-explanation>", results: [] }` rather than an empty `results: []`, so the LLM can choose to fall back to non-semantic searches

#### Scenario: Recovery on provider becoming healthy
- **WHEN** an admin runs `ollama serve` (or sets a missing API key) after startup
- **THEN** the next worker tick SHALL detect health, populate `embedding_source_hash IS NULL` rows in batches, and embedding-dependent features become live without restart

### Requirement: Dirty-Row Detection via Source Hash
Each embeddable row SHALL carry `embedding_source_hash VARCHAR(64) NULL`. The background worker SHALL re-embed a row when its current SHA-256(source_text) differs from `embedding_source_hash`, OR when the row's last-known provider `model_id` (from `embedding_model_log`) differs from the active provider's `modelId()`.

#### Scenario: New row is embedded on next worker tick
- **WHEN** a new Attachment is inserted with `raw_text != null` and `embedding_source_hash IS NULL`
- **THEN** the next worker tick SHALL compute the embedding, update `embedding` + `embedding_source_hash`, and upsert an `embedding_model_log` row

#### Scenario: Editing source text re-embeds the row
- **WHEN** an existing OfferLine's `label` is edited (changing `source_text = label || ' ' || description`)
- **THEN** the next worker tick SHALL detect the hash mismatch, recompute the embedding, and update the row

#### Scenario: Switching providers re-embeds all rows
- **WHEN** an admin changes `genba.embeddings.provider` from `local` to `voyage` and restarts
- **THEN** the next worker tick SHALL detect that `embedding_model_log.model_id` no longer matches `provider.modelId()` for all rows; SHALL mark them dirty (NULL out `embedding_source_hash`) and re-embed in batches

### Requirement: Semantic Search Service
The system SHALL provide hybrid SQL+vector search methods scoped strictly by Organization, returning typed match results with similarity scores. When embeddings are inactive or zero rows have embeddings, search methods SHALL return a typed unavailable response rather than silently returning empty.

#### Scenario: Search attachments by query
- **WHEN** an authenticated user calls `semanticSearch.searchAttachments(query="grounding for the new socket", orgId=X, projectId=P, topK=10)`
- **THEN** the system SHALL embed the query via the active provider, then SELECT FROM attachment WHERE org_id=X AND projectId via parent-resolution AND embedding IS NOT NULL ORDER BY embedding <=> queryVector LIMIT 10; return `{ status: "OK", results: [{attachment, cosineSimilarity, snippet}, ...] }`

#### Scenario: Cross-organization isolation enforced
- **WHEN** a user from Org A calls `searchAttachments(query, orgId=B, ...)`
- **THEN** the system SHALL reject with 403 (the orgId in calls must come from the active session)

#### Scenario: Search returns inactive when worker hasn't embedded
- **WHEN** a search is called against a project whose attachments are all `embedding IS NULL`
- **THEN** the system SHALL return `{ status: "EMPTY", results: [], hint: "Attachments are not yet indexed. Worker last ran at <ts>." }`

### Requirement: Embedding Model Provenance Log
The system SHALL maintain an `embedding_model_log` side table with one row per embedded data row, recording the `model_id` and `embedded_at`, so the worker can detect provider/model changes and trigger re-embeds.

#### Scenario: Successful embed inserts provenance
- **WHEN** the worker successfully embeds an OfferLine
- **THEN** an `embedding_model_log` row SHALL be upserted with `(table_name='offer_line', row_id=<lineId>, model_id=<provider.modelId()>, embedded_at=NOW())`

#### Scenario: Model change detected
- **WHEN** a row's `embedding_model_log.model_id` is `bge-m3@0.5.7` and the active provider's `modelId()` returns `voyage-multilingual-2@2024-09`
- **THEN** the worker SHALL include that row in the next dirty batch and re-embed

### Requirement: Manual HNSW Index Management
The system SHALL allow org admins to opt-in to HNSW indexing per embeddable table via an admin endpoint. Indexes MUST NOT be created automatically — admin decides when to incur the build cost.

#### Scenario: Build HNSW index on a populated table
- **WHEN** an org admin POSTs `/api/admin/embeddings/index?table=attachment`
- **THEN** the system SHALL run `CREATE INDEX CONCURRENTLY ix_attachment_embedding_hnsw ON attachment USING hnsw (embedding vector_cosine_ops)` if not already present; idempotent

#### Scenario: Index stats reported
- **WHEN** GET `/api/admin/embeddings/stats` is called
- **THEN** the response SHALL include per-embeddable-table aggregates `{ totalRows, embeddedRows, dirtyRows, hnswIndexPresent, hnswIndexSize }`

### Requirement: Admin Settings UI
The system SHALL provide an admin-only settings page at `/settings/embeddings` displaying the active provider, its health, per-table indexing stats, GDPR data-flow warnings when a hosted provider is active, and actions to re-embed or build indexes.

#### Scenario: Hosted provider triggers GDPR warning
- **WHEN** the active provider is `voyage` or `openai`
- **THEN** the settings page SHALL render a prominent banner: "Embeddings are sent to <Voyage AI | OpenAI> for processing. Data leaves the EU. Review the data-processing agreement before continuing."

#### Scenario: Re-embed all action
- **WHEN** an org admin clicks "Re-embed all" with confirmation
- **THEN** the system SHALL clear `embedding_source_hash` on all rows in the chosen tables; the worker picks them up on next tick; the page displays live progress
