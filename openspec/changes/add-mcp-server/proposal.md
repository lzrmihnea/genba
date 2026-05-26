## Why
Make Genba a first-class data source for any LLM-equipped client. Mihnea (and future SaaS customers) connect Claude Desktop, Cursor, Cline, Zed, or any MCP-aware app to their Genba account and ask natural-language questions — "compare my GC offers", "which lines is electrician B missing", "find offers similar to last year's bathroom remodel" — without leaving their AI tool. The genba-backend exposes an HTTP streamable MCP server on `/mcp` with PAT (Personal Access Token) authentication; tools are read-only in this change; write tools come later in `add-mcp-write-tools`. The semantic-search tools degrade gracefully when embeddings are inactive (per `add-semantic-search`), so this proposal is shippable independently of whether you ever run Ollama.

## What Changes
- **ADD**: MCP server endpoint at `POST /mcp` on the existing Spring Boot app — implements the MCP protocol over **streamable HTTP** (the modern MCP spec transport): a single HTTP endpoint that accepts JSON-RPC requests and returns either single JSON responses or server-sent events for streaming tool results.
- **ADD**: PAT auth — `Authorization: Bearer <pat>` on `/mcp` requests. PATs are issued from `/settings/tokens`, stored as bcrypt hashes (never plaintext), revocable per token, with optional expiry (default 90 days, configurable).
- **ADD**: `personal_access_token` table — `{id, user_id FK, org_id FK (the org this PAT is scoped to), name, token_hash, last_used_at NULL, expires_at NULL, revoked_at NULL, created_at}`.
- **ADD**: PAT auth filter that resolves the bearer token to a User + active Org, populating the same Spring Security context the JWT filter does — so every existing org-scoping mechanism (services, controllers, RLS-aspect) applies unchanged.
- **ADD**: read-only tool surface (Phase 5 — write tools deferred to `add-mcp-write-tools`):
  - **Projects**: `list_projects`, `get_project`
  - **Vendors**: `list_vendors`, `get_vendor`, `find_similar_vendors` (semantic; degrades gracefully)
  - **Offers**: `list_offers`, `get_offer`, `list_offer_lines`, `find_similar_offer_lines` (semantic)
  - **Comparison**: `compare_offers`, `list_missing_from_other_offers`
  - **Attachments**: `list_attachments`, `get_attachment_text`, `search_attachments` (semantic)
  - **WBS** (Phase 2 awareness — tools register but return empty until Phase 2 data exists): `list_wbs_items`, `find_similar_wbs_items`
  - **Permits/Phases** (Phase 3 awareness): `list_permits`, `list_phases`
  - **Diagnostics**: `genba_status` (returns current provider health, indexing stats summary, schema version — useful for the LLM to know what features are available)
- **ADD**: per-tool ACL bound to the existing role/tier model:
  - `OWNER`/`ADMIN`/`MEMBER`: all read tools on own org's data.
  - `GUEST` (Pro tier external user): only tools matching projects the guest has been invited to; `find_similar_*` cross-project tools are scoped to those projects.
- **ADD**: tool descriptions written so an LLM can pick correctly — each tool's docstring includes when to use, when NOT to use, what it returns, scope boundaries, and the "EMBEDDINGS_INACTIVE" possibility for semantic tools.
- **ADD**: `/settings/tokens` frontend page — list / generate / revoke PATs; show last-used; show usage hints ("Add this to your Claude Desktop config:" with a copy-paste snippet).
- **ADD**: rate limiting per PAT (default 60 requests/min) to protect from runaway LLM loops.
- **ADD**: audit log of MCP calls (last 1000 calls per user, kept 30 days) for debugging — table `mcp_call_log {id, user_id, pat_id, tool_name, args_json, status, duration_ms, called_at}`.
- **ADD**: docs and README setup snippets for the three most common clients (Claude Desktop, Cursor, Cline).
- **NO**: write tools in this change (defer to `add-mcp-write-tools`).
- **NO**: OAuth 2.1 flow (defer; PAT covers the realistic L0+L1 use cases).
- **NO**: stdio transport (defer; HTTP-only covers both local and remote).

## Impact
- **Affected specs**: NEW capability `mcp-server`.
- **Affected code**:
  - New backend package: `eu.px.genba.mcp.{transport, tools, auth, audit, ratelimit}`.
  - New Liquibase: `db.changelog-genba-mcp-001-pat.xml`, `-002-mcp-call-log.xml`.
  - New frontend page: `app/(app)/settings/tokens/page.tsx`, components `<PatList />`, `<PatCreateModal />`.
- **Risk**: PAT theft = full read access to that org's data. Mitigation: 90-day expiry default, last-used tracking with email alert on first use from a new IP (deferred to a follow-up), revocation in one click. Document storing PATs in OS-keychain not plaintext config files.
- **Risk**: an LLM client could iterate through `list_offers` + `get_offer` in a loop. Mitigation: rate-limit + audit log + clear tool descriptions that emphasize bulk results from list_*.
- **Risk**: tool descriptions are the contract between Genba and the LLM. Bad descriptions → bad tool selection → confused user. Mitigation: docstring conventions documented; reviewed during code review.
- **Risk**: schema drift between MCP tool signatures and underlying entities. Mitigation: tools return DTOs that mirror existing REST DTOs; one source of truth.
