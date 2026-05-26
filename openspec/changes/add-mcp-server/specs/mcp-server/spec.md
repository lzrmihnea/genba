## ADDED Requirements

### Requirement: MCP Server Endpoint
The system SHALL expose an MCP server at `POST /mcp` on the existing Spring Boot backend, speaking the MCP protocol over streamable HTTP. The endpoint MUST accept JSON-RPC envelopes per the MCP specification and respond either with a single JSON response or an SSE stream depending on the tool semantics.

#### Scenario: MCP initialize handshake
- **WHEN** an MCP client POSTs `{"jsonrpc":"2.0","id":1,"method":"initialize","params":{...}}` to /mcp with a valid PAT
- **THEN** the system SHALL respond with the server's capability advertisement (supported MCP version, server name "genba", server version, available tool categories)

#### Scenario: tools/list returns scoped tools
- **WHEN** an authenticated client calls `tools/list`
- **THEN** the response SHALL include only tools the caller's role + tier + project memberships permit; tool descriptions, input schemas, and output schemas are included

### Requirement: Personal Access Token Authentication
Every `/mcp` request MUST carry `Authorization: Bearer <pat>` where the PAT was issued from `/settings/tokens` and is not revoked or expired. PATs SHALL be stored only as bcrypt hashes; the plaintext is shown to the user exactly once at generation.

#### Scenario: Valid PAT authenticates
- **WHEN** an MCP request includes a valid, non-expired, non-revoked PAT
- **THEN** the request SHALL be authenticated as the PAT's owning User + Organization; the standard Spring Security context applies; `last_used_at` is updated asynchronously

#### Scenario: Revoked PAT rejected
- **WHEN** an MCP request includes a PAT whose `revoked_at IS NOT NULL`
- **THEN** the system SHALL respond with HTTP 401 + JSON-RPC error `Unauthorized`

#### Scenario: Expired PAT rejected
- **WHEN** an MCP request includes a PAT whose `expires_at < NOW()`
- **THEN** the system SHALL respond with HTTP 401 + error message indicating expiry

#### Scenario: Generate, copy-once, revoke
- **WHEN** an authenticated user POSTs `/api/auth/tokens` with `{name: "Claude Desktop"}`
- **THEN** the system SHALL create a row, return `{ id, name, plaintext_token, created_at, expires_at }` in the response (plaintext shown ONCE), store only the bcrypt hash, and provide a DELETE endpoint to revoke

### Requirement: Read-Only Tool Surface
The MCP server SHALL provide read-only tools covering Projects, Vendors, Offers, OfferLines, Attachments, comparison, missing-from-others, WBS, Permits, Phases (Phase 3+ aware), plus a `genba_status` diagnostic tool. Write tools are out of scope and added in a separate change (`add-mcp-write-tools`).

#### Scenario: list_offers returns org-scoped data
- **WHEN** a Member of Organization X calls `tools/call list_offers {project_id: P}` where P belongs to Org X
- **THEN** the response SHALL include offers in project P only, excluding soft-deleted; ordered by `received_at DESC`

#### Scenario: Cross-org access blocked
- **WHEN** a Member of Org X calls a tool referencing a project_id belonging to Org Y
- **THEN** the tool SHALL return a JSON-RPC error `Forbidden` (treated like a 404 to avoid leaking existence)

#### Scenario: Semantic tool degrades when embeddings inactive
- **WHEN** a Member calls `tools/call search_attachments {query: "..."}` and the active embedding provider is unhealthy
- **THEN** the response SHALL be `{ status: "EMBEDDINGS_INACTIVE", reason: "...", results: [] }` rather than an empty `results: []`, so the LLM can choose to fall back

#### Scenario: genba_status describes current capabilities
- **WHEN** a client calls `tools/call genba_status`
- **THEN** the response SHALL include `{ version, current_user_email, current_org_name, embeddings: {provider, status}, indexing_summary, available_capabilities: ["projects", "offers", ...] }` so the LLM can self-orient

### Requirement: Per-Tool ACL by Role and Tier
The system SHALL enforce per-tool authorization based on the caller's `org_role` (OWNER/ADMIN/MEMBER/GUEST) and tier (Standard/Pro). Guests SHALL see only tools matching projects they've been invited to; cross-project semantic tools called by a guest SHALL be scoped to those projects.

#### Scenario: Guest cannot list other-project offers
- **WHEN** a GUEST member, invited to project P, calls `list_offers {project_id: Q}` where Q is a different project in the same org
- **THEN** the system SHALL return Forbidden

#### Scenario: Guest's tools/list is filtered
- **WHEN** a GUEST calls `tools/list`
- **THEN** the response SHALL exclude tools the guest cannot use; tools the guest CAN use are documented as such (e.g., `list_offers` description mentions "scoped to projects you have access to")

### Requirement: Rate Limiting
Each PAT SHALL have a per-minute rate limit (default 60 calls/min), configurable globally and per-tool. Exceeded calls SHALL return a typed `RateLimited` error with `retry_after_ms` so the LLM can wait and retry.

#### Scenario: Rate limit kicks in
- **WHEN** a single PAT issues more than 60 `tools/call` requests within 60 seconds
- **THEN** the 61st+ requests SHALL return `RateLimited` until the window slides; `last_used_at` is still updated to reflect activity

### Requirement: MCP Call Audit Log
The system SHALL persist an audit log of all `/mcp` `tools/call` invocations to `mcp_call_log` with user, PAT, tool name, redacted args, status, duration, and timestamp. Records SHALL be retained for at least 30 days (configurable) and exposed to the owning user via `/api/auth/mcp-activity`.

#### Scenario: Successful tool call logged
- **WHEN** any `tools/call` completes (success or failure)
- **THEN** an `mcp_call_log` row SHALL be inserted asynchronously with `{user_id, pat_id, tool_name, args_json (redacted), status: 'OK'|'ERROR'|'RATE_LIMITED'|'FORBIDDEN', duration_ms, called_at}`

#### Scenario: User views own recent activity
- **WHEN** the authenticated user GETs `/api/auth/mcp-activity`
- **THEN** the response SHALL return the last 100 entries belonging to that user, including which PAT was used and the high-level outcome

#### Scenario: Sensitive args are redacted
- **WHEN** a tool's call args contain sensitive fields (per a per-tool redaction allowlist)
- **THEN** the stored `args_json` SHALL omit or truncate those fields before persistence

### Requirement: Client Documentation
The system SHALL ship documentation enabling a user to configure each of the major MCP clients (Claude Desktop, Cursor, Cline, Zed) against their genba account in under 5 minutes from a generated PAT.

#### Scenario: Documentation includes paste-ready snippets
- **WHEN** a user generates a PAT in `/settings/tokens`
- **THEN** the UI SHALL display per-client snippets they can copy into their MCP client config (server URL + bearer token placeholder); the `docs/mcp-clients.md` file SHALL contain the same snippets plus screenshots
