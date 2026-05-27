## Why
With `add-auth-foundation` in place (Spring Boot + Next.js scaffolding, User, Organization, JWT, i18n, seeded admin), this change introduces the construction-domain foundation that the rest of Layer 1 depends on: a `Project` entity (one house build), locale fields on the existing `Organization`, and the polymorphic `Attachment` mechanism that underpins the editable-first design. Attachments are universal: every later capability (Offers, Permits, Documents, BudgetLines, PhotoLog) attaches to this same primitive.

## What Changes
- **ADD**: `Project` entity (one house build) — `{id, org_id, name, description, address, base_currency, status enum (PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, ARCHIVED), audit fields, soft-delete}`. Org-scoped.
- **ADD**: locale fields on the existing `Organization` entity from `add-auth-foundation` — `country_code` (ISO 3166-1 alpha-2, default `RO`), `currency_code` (ISO 4217, default `RON`), `vat_regime` (string key, default `RO_STANDARD`), `permit_workflow_template_id` UUID nullable (default template pointer; FK populated in Phase 3; semantics: starting template for new Projects per the modular-templates principle, not a hard constraint).
- **ADD**: polymorphic `Attachment` table — `{id, attachable_type, attachable_id, kind enum (FILE, TEXT, LINK), source_channel_id FK NOT NULL, file_url, raw_text, original_filename, mime_type, byte_size, uploaded_by, uploaded_at, org_id, embedding VECTOR(1024) NULL, embedding_source_hash VARCHAR(64) NULL}`. Files stored on local disk in Layer 0 at configurable `genba.uploads.path`. Embedding column reserved for semantic search (populated by `add-semantic-search` Phase 5; stays NULL until a provider is configured).
- **ADD**: `attachment_source_channel` lookup table — `{id, code VARCHAR(32) UNIQUE, label_en, label_ro, system_managed BOOLEAN, sort_order, deleted_at NULL}`. System-managed seed rows: `pdf`, `image`, `email`, `whatsapp`, `voice`, `verbal`, `link`, `other`. Org admins can CREATE additional channels but cannot edit/delete system-managed ones.
- **ADD**: REST endpoints — Projects CRUD, Attachments polymorphic CRUD + download, AttachmentSourceChannel list/create/delete (org-admin only).
- **ADD**: Frontend — projects index page, project create/edit drawer, project detail page (shell — populated by later changes), reusable `<AttachmentList />` and `<AttachmentUpload />` components bound to any (type, id) pair with channel selector.
- **NO BILLING / SUBSCRIPTION LOGIC** in this change. No `tier` column on Organization yet (deferred until subscription work).
- **NO PRODUCTION DEPLOYMENT** code; docker-compose remains local-only.

## Impact
- **Affected specs**: NEW capability `genba-core`. MODIFIED capability `auth-foundation` (Organization gains locale fields).
- **Affected code**:
  - New backend packages: `eu.px.genba.project.*`, `eu.px.genba.attachment.*`.
  - New Liquibase changesets: `db.changelog-genba-core-001-org-locale.xml`, `-002-project.xml`, `-003-attachment-source-channel.xml`, `-004-attachment.xml`.
  - New Next.js routes: `app/(app)/projects/*`; reusable `<AttachmentList />`, `<AttachmentUpload />`, `<ChannelSelector />` components.
  - Modified: `Organization` entity (added 4 fields) — backward-compatible via DEFAULTs.
- **Risk**: The polymorphic Attachment pattern has no DB-level FK to parent rows. Mitigation: all parent entities use soft-delete; a future scheduled job sweeps orphan attachments.
- **Risk**: Allowing user-created `attachment_source_channel` rows complicates filter UIs (a user could add 50 custom channels). Mitigation: a UI cap of e.g. 32 custom channels per org; clear surfacing of "system" vs "custom" in the channel picker.
