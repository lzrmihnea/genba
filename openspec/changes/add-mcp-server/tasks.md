## 1. Library / transport selection
- [ ] 1.1 Evaluate available Spring MCP integrations (e.g., `spring-ai-mcp`, `mcp-spring-boot-starter`); if none is mature enough by implementation time, hand-roll the JSON-RPC + streamable-HTTP layer (the MCP wire protocol is small — ~few hundred lines of Java).
- [ ] 1.2 Document the chosen library/approach in design.md.

## 2. Personal Access Tokens
- [ ] 2.1 Liquibase changeset `db.changelog-genba-mcp-001-pat.xml`: `personal_access_token` table — `id UUID PK`, `user_id UUID FK NOT NULL`, `org_id UUID FK NOT NULL`, `name VARCHAR(128) NOT NULL`, `token_hash VARCHAR(255) NOT NULL` (bcrypt), `last_used_at TIMESTAMP WITH TIME ZONE NULL`, `expires_at TIMESTAMP WITH TIME ZONE NULL`, `revoked_at TIMESTAMP WITH TIME ZONE NULL`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()`. Indexes `(user_id, revoked_at)`, `(token_hash)`.
- [ ] 2.2 `PatService` — generate (returns plaintext ONCE — never stored), list (for the owner only), revoke, validate-bearer (returns the resolved User + Organization or 401). Plaintext PATs prefixed `genba_pat_` for identifiability.
- [ ] 2.3 Endpoints: `GET /api/auth/tokens` (list mine), `POST /api/auth/tokens` (create — returns `{ id, plaintext, ...}` ONCE), `DELETE /api/auth/tokens/{id}` (revoke).
- [ ] 2.4 Frontend `/settings/tokens` page: list view (name + last_used + expires_at + revoke button); "Generate token" modal with copy-once display + warning ("This is the only time you'll see the token. Store it in your password manager."); per-client snippets ("Add to your Claude Desktop config", "Add to your Cursor config", etc.).
- [ ] 2.5 Integration tests: create PAT, use it against /mcp, revoke, attempt to use again → 401.

## 3. MCP transport over streamable HTTP
- [ ] 3.1 `McpController` mapped at `POST /mcp` accepting `Content-Type: application/json` requests with the MCP JSON-RPC envelope. Supports both single-response and SSE-streamed responses (Spring's `SseEmitter` or `Flux<ServerSentEvent>`).
- [ ] 3.2 PAT auth filter: extracts `Authorization: Bearer <pat>`; validates via `PatService`; sets Spring Security context; updates `last_used_at`.
- [ ] 3.3 JSON-RPC dispatch: route methods like `tools/list`, `tools/call`, `initialize`, `ping` per MCP spec; tools/call dispatches to the registered tool by name.
- [ ] 3.4 Error envelope per MCP spec (typed error codes for auth failure, rate limit, tool not found, tool error).
- [ ] 3.5 CORS configured for direct-browser MCP clients (not the typical case — most MCP clients are desktop apps — but small surface to add).

## 4. Tool surface (read-only)
- [ ] 4.1 `Tool` registry with metadata: `name`, `description` (LLM-facing), `inputSchema` (JSON Schema for args), `outputSchema`, `acl` (which roles can call), `requiresEmbeddings` (boolean — false for non-semantic tools).
- [ ] 4.2 Implement tools (each ~30-80 LOC; reuse existing services for data access):
  - **Projects**: `list_projects(filter?: { status?, limit?, offset? })`, `get_project(id)`
  - **Vendors**: `list_vendors(project_id?)`, `get_vendor(id)`, `find_similar_vendors(query, top_k=10)` (semantic; respects EMBEDDINGS_INACTIVE)
  - **Offers**: `list_offers(project_id, status?)`, `get_offer(id, include_lines=true)`, `list_offer_lines(offer_id)`, `find_similar_offer_lines(query, project_id?, top_k=10)` (semantic)
  - **Comparison**: `compare_offers(project_id, offer_ids?)`, `list_missing_from_other_offers(current_offer_id)`
  - **Attachments**: `list_attachments(attachable_type, attachable_id)`, `get_attachment_text(id)` (returns raw_text or null), `search_attachments(query, project_id?, top_k=10)` (semantic)
  - **WBS (Phase 2-aware)**: `list_wbs_items(project_id)`, `find_similar_wbs_items(query, project_id, top_k=10)` (semantic; both no-op until Phase 2 data exists)
  - **Permits/Phases (Phase 3-aware)**: `list_permits(project_id)`, `list_phases(project_id)` — stubbed empty until Phase 3
  - **Diagnostics**: `genba_status()` returns `{ version, current_user, current_org, embeddings_status, indexing_stats_summary, available_capabilities }`
- [ ] 4.3 Each tool description (for LLMs) follows a fixed template: `<purpose> | when to use | when not to use | returns | scope | failure modes`.
- [ ] 4.4 Tool input/output validation via existing Zod-equivalent or Jackson + JSR-303. Reject malformed args with typed error.

## 5. Per-tool ACL
- [ ] 5.1 `McpAccessControl` evaluates tool calls against the resolved user's org_role + tier. Guests (Pro tier external) get a restricted tool set (only their assigned projects' read tools).
- [ ] 5.2 Test matrix: OWNER/ADMIN/MEMBER/GUEST × full tool list → expected allow/deny.
- [ ] 5.3 Returned tool list from `tools/list` is FILTERED per caller (LLMs only see tools they can call).

## 6. Rate limiting
- [ ] 6.1 In-memory rate limiter (Bucket4j or simple Caffeine-backed sliding window). Default `genba.mcp.rate-limit.per-pat-per-minute=60`. Configurable per-tool override.
- [ ] 6.2 Exceeded → JSON-RPC error `-32099 RateLimited` with `retry_after_ms` in `data`.
- [ ] 6.3 Integration test: hammer a PAT, observe rate limit kick in.

## 7. Audit log
- [ ] 7.1 Liquibase changeset `db.changelog-genba-mcp-002-mcp-call-log.xml`: `mcp_call_log` table — `id UUID PK`, `user_id UUID FK`, `pat_id UUID FK NULL` (NULL if JWT-authenticated), `tool_name VARCHAR(64) NOT NULL`, `args_json JSONB`, `status VARCHAR(32) NOT NULL`, `duration_ms INT NOT NULL`, `called_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()`. Index `(user_id, called_at DESC)`.
- [ ] 7.2 `@Async` log writer so audit logging never blocks the MCP response.
- [ ] 7.3 Retention job: scheduled daily delete of `mcp_call_log` rows older than 30 days (configurable).
- [ ] 7.4 `GET /api/auth/mcp-activity` (current user only) returns the last 100 calls — surfaces in /settings/tokens as "Recent activity".

## 8. Documentation
- [ ] 8.1 `docs/mcp-clients.md` — step-by-step setup for Claude Desktop, Cursor, Cline, Zed; example questions to ask; common pitfalls.
- [ ] 8.2 `docs/mcp-protocol.md` — what tools genba exposes, their schemas, ACL.
- [ ] 8.3 README "AI integrations" section with quick-start.

## 9. Verification
- [ ] 9.1 Integration test using a real MCP client SDK (or hand-rolled JSON-RPC client) against a TestContainers-backed Spring Boot instance: initialize → tools/list → tools/call list_offers → tools/call find_similar_offer_lines (with EMBEDDINGS_INACTIVE behavior verified).
- [ ] 9.2 Manual smoke test connecting Claude Desktop on Mihnea's Mac to localhost:8086/mcp using a generated PAT; ask "compare my offers in project X"; verify Claude calls the tool and renders the comparison.
- [ ] 9.3 Move spec deltas to `openspec/specs/mcp-server/spec.md` after merge.
- [ ] 9.4 `openspec validate add-mcp-server --strict` passes.
- [ ] 9.5 Archive after merge.
