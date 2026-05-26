## Context
This change makes Genba consumable by any LLM client that speaks MCP — primarily Claude Desktop, Cursor, Cline, Zed, Sourcegraph Cody, Continue, and the OpenAI Responses API. The choice of streamable-HTTP transport + PAT auth + read-only tools is deliberate: minimum viable surface that ships value (turn natural-language questions into structured retrievals over your build) while leaving the path open to OAuth and write tools in later changes.

## Goals / Non-Goals

**Goals**
- Allow any MCP-aware LLM client to connect to a user's genba data with a single token-paste setup.
- Strict per-org and per-user-role scoping — an LLM session sees only what its user can see in the web UI.
- Graceful degradation for embedding-dependent tools when `add-semantic-search` provider is inactive.
- Distribution: turn Genba into a value-add for any user already paying for Claude / Cursor / Cline.
- Defensive posture: PATs are revocable, rate-limited, audited.

**Non-Goals**
- Write/mutation tools (deferred to `add-mcp-write-tools`).
- OAuth 2.1 device flow (deferred — PATs cover realistic L0/L1 needs).
- stdio transport (deferred — HTTP covers local and remote).
- Multi-org per-token (deferred — a single PAT is scoped to one org for clarity; users with multiple orgs generate one PAT per org).
- Server push of events (e.g., "new offer received") — read-only polling/pull only in this phase.

## Decisions

**Decision: HTTP streamable, not stdio.**
- Alternative: stdio for local clients (Claude Desktop launches a JVM subprocess).
- Rationale: one transport covers both L0 local (`http://localhost:8086/mcp`) and L1 SaaS (`https://api.genba.pantopix.ro/mcp`). The user's chosen path. stdio adds a packaging/launcher problem we don't need.

**Decision: PAT auth, not OAuth 2.1.**
- Alternative: OAuth 2.1 (the MCP spec's recommended auth for remote servers).
- Rationale: OAuth requires implementing /authorize, /token, /register endpoints + client registration + consent UI. ~2 weeks of work for marginal UX gain over PAT-paste in MCP client config. PAT is acceptable security for L0 + early L1; OAuth can layer later when consumer-grade usability matters more than time-to-ship.

**Decision: PATs scoped to one Organization.**
- Alternative: per-user PAT that can act in any of the user's orgs (org chosen per-call).
- Rationale: simpler mental model; safer (token leak limits blast radius to one org); aligns with the "active org" abstraction the rest of the app uses. Users with multiple orgs generate one PAT per org.

**Decision: Read-only tools in this change.**
- Alternative: ship both read and write tools at once.
- Rationale: read tools validate the integration end-to-end without write-side risks (e.g., LLM hallucinating a vendor name and creating it). Real users get value immediately. Write tools come in `add-mcp-write-tools` after L0 usage proves the patterns.

**Decision: Tool descriptions are the contract.**
- Rationale: in MCP, the LLM reads each tool's description to decide which to call. Bad descriptions → bad tool selection → broken UX. We document a fixed template (purpose / when to use / when NOT to use / returns / scope / failure modes) so all tools read consistently. Code review enforces.

**Decision: `genba_status` tool exposes capability discovery to the LLM.**
- Rationale: when an LLM session starts, calling `genba_status` tells it: "embeddings are inactive — don't bother with `search_attachments`; Phase 3 hasn't shipped — don't ask about permits." Saves a lot of failed tool calls and confused responses. ~30 LOC for the tool, huge UX win.

**Decision: Per-PAT rate limit, not per-user.**
- Rationale: a user might have multiple PATs (one per LLM client). Limiting per-PAT lets each client get its own quota. Per-user rate-limiting can be layered if abuse surfaces.

**Decision: Audit log retained 30 days.**
- Rationale: long enough for security investigation and self-debugging ("what did my LLM do yesterday?"). Short enough to avoid GDPR retention concerns. Configurable.

**Decision: Embedding-dependent tools return typed `EMBEDDINGS_INACTIVE` rather than empty results.**
- Rationale: empty results are ambiguous (no matches vs system not available). A typed status lets the LLM react ("the user's semantic search isn't set up — I should explain and fall back to non-semantic tools").

**Decision: MCP server embedded in the existing Spring Boot JVM, not a separate microservice.**
- Rationale: shares auth, DB pool, services, and deployment. No extra ops. At L0/L1 scale (low-thousands req/day), no contention concerns. If MCP traffic ever dwarfs the web UI, splitting is trivial.

**Decision: Streamable-HTTP supports both single-response and SSE.**
- Rationale: most read tools return a single JSON response. SSE is useful for `compare_offers` over many offers (stream partial results) and for future write tools that could stream progress. Spring's `SseEmitter` / WebFlux makes this cheap.

## Risks / Trade-offs
- **PAT theft → full read access**: documented severity; mitigated by 90-day default expiry, easy revocation, last-used tracking. Future enhancement: email on first use from a new IP (deferred to a follow-up).
- **LLM hallucination as a denial-of-service vector**: an over-zealous LLM looping `list_offers` thousands of times. Mitigated by rate limit + tool descriptions encouraging batch use of list_* tools.
- **Tool description churn**: changing a tool's description changes how LLMs decide whether to call it; small wording changes can have outsized effects. Mitigated by treating tool descriptions as semi-public API; version notes in CHANGELOG.
- **Schema drift**: MCP tool DTOs vs REST DTOs vs entity changes — three places to update. Mitigated by deriving MCP DTOs from the same source as REST DTOs (MapStruct mappers reused).

## Migration Plan
- New tables `personal_access_token` + `mcp_call_log` added by Liquibase; no existing data to migrate.
- Rollback: drop the two tables; remove the `/mcp` controller; users lose their PATs (need to re-issue post-rollforward). Documented but unlikely.

## Open Questions
1. Should each PAT carry a per-token rate limit override (so a user can have a "slow" PAT for casual use and a "fast" one for power use)? Recommend NO for L0; defer.
2. Should `mcp_call_log` argument JSON be redacted for sensitive tools (e.g., `search_attachments` query strings might be sensitive)? Recommend YES — `args_json` truncated to 256 chars + sensitive fields stripped via per-tool serializer. Add as a sub-task here.
3. Should the `/settings/tokens` page show per-PAT call counts? Recommend YES — small query against `mcp_call_log`, surfaces usage at a glance.
4. Should we expose tool versions (so an LLM that depends on a specific tool shape can pin)? Recommend deferring — implicit assumption is forward-compatibility within the MCP spec's negotiation.
5. Sniff out which Spring MCP library to use at implementation time — spring-ai's MCP support has been moving fast as of 2026. Document the choice in this design.md when we get there.
