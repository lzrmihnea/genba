# Project Context

## Purpose
Genba is a construction-management product for homeowners who hired a general contractor and need oversight of their build — bid comparison, permits, phase tracking, document vault, budget actuals. Designed for the Romanian market first (RO permits, RON pricing, TVA), architected to scale internationally via locale-keyed data (currency, VAT regime, permit-workflow defaults per Organization).

The name comes from 現場 — Japanese for "the actual place where work happens." Pairs with the kaisha (会社, "company") sibling in the pxro / Pantopix portfolio.

## Tech Stack
- **Backend**: Spring Boot 3.1.5 (Java 17) — **independent codebase**, conventions and patterns referenced from pxro-kaisha-03 (NOT a git fork; no `cp -R`)
- **Database**: PostgreSQL 15 with **pgvector** extension (image `pgvector/pgvector:pg15`), Liquibase migrations
- **ORM**: JPA/Hibernate with Spring Data JPA
- **Authentication**: JWT-based with Spring Security 6 (multi-org RBAC built fresh in genba; pattern referenced from kaisha-03); PATs for MCP access (Phase 5)
- **Documentation**: Swagger/OpenAPI 3 (SpringDoc)
- **Build**: Maven (root pom + `genba-backend` module)
- **Frontend**: Next.js 15 + React 19 + TypeScript, Tailwind CSS, Ant Design 5, TanStack Query, Axios, next-intl for i18n
- **LLM access layer** (Phase 5, dormant until activated): MCP server on `/mcp` (HTTP streamable transport), PAT auth from `/settings/tokens`, embedding-backed semantic search via `add-semantic-search` with pluggable provider (default local Ollama; alternatives Voyage `voyage-multilingual-2` and OpenAI `text-embedding-3-large` with Matryoshka-1024)
- **Containerization**: Docker & Docker Compose (local-only for Layer 0; no Hetzner / Traefik / staging / production)

## Port Assignments (next available after pxro-kaisha 3000/8085/5434/8081)
- Frontend Next.js: **3001**
- Backend Spring Boot: **8086**
- PostgreSQL: **5435**
- No Adminer service — user runs DBeaver as the DB client.

## Internationalization
- **Code identifiers**: English only (class names, methods, REST paths, table/column names, enum values).
- **UI**: i18n via `next-intl`; supports EN + RO from day 1; both bundled in the build; user picks language in their profile.
- **Backend response messages**: externalized via Spring `MessageSource` (`messages_en.properties`, `messages_ro.properties`); response headers/JSON return both the i18n key and a localized string resolved from the `Accept-Language` header.
- **Domain seed data** (WBS templates, permit names, document checklists): authored bilingually (`name_en`, `name_ro` columns where applicable; or a `translation` join table for richer cases).
- **User-entered data** (Vendor names, Offer labels, OfferLine descriptions): single-string, not translated.

## Project Conventions

### Code Style (pattern from kaisha-03)
- Lombok annotations (@Data, @NoArgsConstructor, @AllArgsConstructor, @Slf4j)
- Explicit imports (no wildcards)
- Package by layer: `eu.px.genba.<feature>.{controller, service, repository, entity, dto, mapper}` + cross-cutting `security`, `config`, `enums`, `i18n`, `common`
- ALL identifiers in English

### Architecture Patterns (referenced from kaisha-03)
- **Service Layer Pattern**: business logic in services with @Transactional
- **Repository Pattern**: Spring Data JPA repositories
- **DTO Pattern**: Request/Response DTOs separate from entities
- **MapStruct** for entity ↔ DTO mapping
- **Polymorphic Attachment**: any domain entity can carry zero-or-more attachments via {attachable_type, attachable_id} — files are OPTIONAL evidence, never the input path

### Editable-First Design Principle (CRITICAL)
- Every entity (Offer, Permit, Phase, Document) has a UI form as the canonical input.
- Files are optional Attachments — NOT the primary input path.
- Reason: Romanian contractor offers arrive over WhatsApp text, email body, photos of handwritten quotes, and verbal phone calls. PDF is one of many formats.
- Consequence: zero OCR/LLM blockers in Layer 0. AI-assisted extraction is a future layer that *suggests* filling fields from attached files — never autopopulates without user confirmation.

### Modular-Templates Design Principle (CRITICAL)
- Every recommended structure (WBS taxonomy, permit workflow, phase plan, payment-milestone schedule, document checklist) is a **template** that the user can clone, fully modify, or replace per Project.
- "Romanian Single-Family House" is the seeded **default** template, but the data model MUST NOT couple any of these concepts to a specific locale or project type at the entity level.
- Pattern: `<X>Template` (library, org- or system-level) versus the per-Project cloned entities owned by that Project. Example: `PermitWorkflowTemplate` is the library; `PermitInstance` rows on a Project are the user-owned, fully-editable copies.
- A Project SHALL be creatable from a template (clone-on-create), from another existing Project (clone-on-fork), or from blank.
- Locale fields on Organization (`country_code`, `currency_code`, `vat_regime`, `permit_workflow_template_id`) provide **defaults** for new Projects — never a hard constraint on what a Project's structure looks like.
- Consequence for future Phase 2/3/4 drafts: WBS, Permits, Phases, and Payment Milestones are all template-driven. Phase 1 entities (Project, Vendor, Offer, OfferLine, Attachment, MatchGroup) are inherently per-project and template-neutral.

### Testing Strategy
- Unit tests for service layer
- Integration tests with TestContainers for database
- Convention: `*Test` (unit) and `*IT` (integration) suffixes

### Git Workflow
- Main integration branch: `develop`
- Feature branches for new work, rebased from `develop` before push
- Commit messages: max 9 lines, NO mentions of AI, Claude, Co-Authored-By
- Remote: https://github.com/lzrmihnea/genba

## Domain Context
- **Organization**: tenant boundary; multi-org from day 1 even though Layer 0 is single-tenant single-user
- **Project**: ONE HOUSE BUILD (semantic shift from kaisha where Project=client engagement)
- **Vendor**: constructor, electric contractor, HVAC installer, supplier, etc.
- **Offer**: structured bid from a Vendor for a Project; contains OfferLines; carries optional Attachments
- **OfferLine**: single line item in an Offer (qty, unit, unit_price, currency, vat_rate, label, optional wbs_item FK)
- **Attachment**: polymorphic — attaches to any entity. Carries {kind, source_channel, file_url, raw_text}
- **MatchGroup** (Phase 1): project-scoped grouping of OfferLines across Offers that the user declares equivalent in scope, enabling side-by-side comparison without WBS
- **WBSTemplate / WBSItem** (Phase 2): hierarchical work-breakdown structure — template (library, RO-seeded) cloned per Project
- **PermitWorkflowTemplate / PermitInstance** (Phase 3): regulatory milestones; template (library, RO-seeded with CU, AC, ISC, dirigent, recepție, intabulare) cloned per Project
- **Phase / Subphase / Task** (Phase 3): construction phases with simple Gantt; phase template cloned per Project
- **BudgetLine / ActualSpend / PaymentMilestone** (Phase 4): financial tracking with retention awareness
- **PhotoLog** (Phase 4): geotagged photos per phase
- **ChangeOrder / Variation** (Phase 4): scope deltas with reason tracking

## Important Constraints
- **Locale-keyed from day 1**: `country_code`, `currency_code`, `vat_regime`, `permit_workflow_template_id` on Organization. RO is the first locale; scaling internationally = adding rows, not refactor.
- **Multi-org scaffolding present** even though Layer 0 is single-org single-user. Retrofitting auth/RBAC later is expensive.
- **GDPR**: contractor PII in Offers → EU hosting whenever deployment happens. Layer 0 is local-only.
- **No production deployment in Layer 0**: docker-compose for local Mac dev only.
- **No niroai / MCP dependencies in genba** (per user decision).

## External Dependencies
- PostgreSQL 15+ with **pgvector** extension (UUID generation, 1024-d VECTOR columns for embeddings)
- JWT (JJWT library v0.12.3, pattern from kaisha-03)
- next-intl (frontend i18n)
- **Optional, dormant by default**:
  - Ollama (local embedding provider; `ollama pull bge-m3`); if missing, semantic-search stays inactive but the rest of the app runs untouched
  - Voyage AI API key (hosted embeddings via `voyage-multilingual-2`)
  - OpenAI API key (hosted embeddings via `text-embedding-3-large` with `dimensions: 1024` Matryoshka truncation)

## Product Tiers (Layer 1+ — NOT in Layer 0 scope; data model must accommodate)
- **Standard**: €9/mo or €99/yr — single user
- **Pro**: €19/mo or €198/yr — up to 10 external guest users (VIEW + COMMENT permissions; no edit, no upload)
