## 1. Locale fields on Organization
- [ ] 1.1 Liquibase changeset `db.changelog-genba-core-001-org-locale.xml`: ALTER TABLE `organization` ADD `country_code VARCHAR(2) NOT NULL DEFAULT 'RO'`, `currency_code VARCHAR(3) NOT NULL DEFAULT 'RON'`, `vat_regime VARCHAR(32) NOT NULL DEFAULT 'RO_STANDARD'`, `permit_workflow_template_id UUID NULL` (FK constraint added in Phase 3 referencing `permit_workflow_template`).
- [ ] 1.2 Update `Organization` entity, DTO, mapper, validation. Backfill the seed Org row (from add-auth-foundation) with defaults; idempotent.
- [ ] 1.3 Frontend: organization settings page exposes country/currency/vat_regime fields. Read-only in Layer 0 (write capability arrives in Phase 3 when templates are user-pickable).
- [ ] 1.4 Integration test: GET /api/auth/me on a fresh seed → response includes locale fields with RO defaults.

## 2. Attachment source-channel lookup
- [ ] 2.1 Liquibase changeset `db.changelog-genba-core-003-attachment-source-channel.xml`: `attachment_source_channel` table — `id UUID PK`, `org_id UUID FK NULL` (NULL = system-managed global row), `code VARCHAR(32) NOT NULL`, `label_en VARCHAR(64) NOT NULL`, `label_ro VARCHAR(64) NOT NULL`, `system_managed BOOLEAN NOT NULL DEFAULT FALSE`, `sort_order INT NOT NULL DEFAULT 100`, `deleted_at TIMESTAMPTZ NULL`. UNIQUE (`org_id`, `code`) where `org_id` IS NOT NULL; UNIQUE (`code`) WHERE `org_id` IS NULL (system).
- [ ] 2.2 Seed system rows: `(pdf, PDF, PDF)`, `(image, Image, Imagine)`, `(email, Email, Email)`, `(whatsapp, WhatsApp, WhatsApp)`, `(voice, Voice note, Mesaj vocal)`, `(verbal, Verbal/Phone, Verbal/Telefon)`, `(link, Link, Link)`, `(other, Other, Altul)`.
- [ ] 2.3 Entity + repository + service + DTO under `pxro.genba.attachment.channel.*`.
- [ ] 2.4 Endpoints: `GET /api/attachment-channels` (list system + org's custom, deleted_at IS NULL), `POST /api/attachment-channels` (org-admin only, creates org-scoped custom channel), `DELETE /api/attachment-channels/{id}` (org-admin only; reject if `system_managed=true`).
- [ ] 2.5 Validation: org_id is enforced to match the requesting user's active org; max 32 custom channels per org.

## 3. Polymorphic Attachment
- [ ] 3.1 Liquibase changeset `db.changelog-genba-core-004-attachment.xml`: `attachment` table — `id UUID PK`, `org_id UUID FK NOT NULL` (denormalized for fast filter), `attachable_type VARCHAR(64) NOT NULL`, `attachable_id UUID NOT NULL`, `kind VARCHAR(16) NOT NULL` ∈ {FILE, TEXT, LINK}, `source_channel_id UUID FK NOT NULL`, `file_url VARCHAR(1024) NULL`, `raw_text TEXT NULL`, `original_filename VARCHAR(512) NULL`, `mime_type VARCHAR(128) NULL`, `byte_size BIGINT NULL`, `uploaded_by UUID FK app_user`, `uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`. Index `(attachable_type, attachable_id)`, `(org_id, uploaded_at DESC)`.
- [ ] 3.2 `Attachment` entity + repository + service + DTO under `pxro.genba.attachment.*`. Repository method `findByAttachable(type, id, orgId)` returning ordered by uploaded_at DESC.
- [ ] 3.3 `AttachmentStorageService` writing files to local disk at configurable `genba.uploads.path` (default `./uploads/{org_id}/{yyyy}/{MM}/{uuid}-{filename}`). Max file size 25 MB (configurable).
- [ ] 3.4 Endpoints: `POST /api/attachments` (multipart for FILE; JSON for TEXT/LINK), `GET /api/attachments?type=X&id=Y`, `GET /api/attachments/{id}/download` (FILE only, streams with Content-Disposition), `DELETE /api/attachments/{id}`. All org-scoped.
- [ ] 3.5 Frontend: `<AttachmentList type id />` (lists existing, lets user delete), `<AttachmentUpload type id channels={?} />` (channel picker + file/text/link input — UI mode driven by selected channel), `<ChannelSelector />` (reads channels from API, separates system vs custom).
- [ ] 3.6 Integration test: attach PDF, attach WhatsApp text, attach link, list all three on a Project, download the PDF, delete one, verify list updates.

## 4. Project entity
- [ ] 4.1 Liquibase changeset `db.changelog-genba-core-002-project.xml`: `project` table — `id UUID PK DEFAULT gen_random_uuid()`, `org_id UUID FK NOT NULL`, `name VARCHAR(255) NOT NULL`, `description TEXT`, `address VARCHAR(512)`, `base_currency VARCHAR(3) NOT NULL`, `status VARCHAR(32) NOT NULL DEFAULT 'PLANNING'`, `created_at`, `updated_at`, `created_by UUID FK app_user`, `deleted_at TIMESTAMPTZ NULL`. Index `(org_id, deleted_at)`.
- [ ] 4.2 Entity + repository + service + controller + DTO under `pxro.genba.project.*`. Service applies org scope from active org.
- [ ] 4.3 Endpoints: `GET /api/projects` (list, org-scoped), `GET /api/projects/{id}` (org-scoped + 404 on cross-org), `POST /api/projects`, `PUT /api/projects/{id}`, `DELETE /api/projects/{id}` (soft delete).
- [ ] 4.4 Frontend: `app/(app)/projects/page.tsx` index, `app/(app)/projects/[id]/page.tsx` detail (shell), `<ProjectCreateDrawer />`, project switcher in main nav.
- [ ] 4.5 Integration test: create project under seed org, list, soft-delete, verify list excludes; attempt cross-org access returns 404.

## 5. Verification + spec migration
- [ ] 5.1 Full TestContainers integration test covering Project + Attachment + AttachmentSourceChannel happy path.
- [ ] 5.2 Manual smoke test on local Mac: log in → create a project → attach a PDF → attach pasted WhatsApp text → attach a Google Drive link → custom channel "site visit photo" → verify all attached, downloadable, deletable.
- [ ] 5.3 Move spec deltas from `openspec/changes/add-genba-core/specs/genba-core/spec.md` to `openspec/specs/genba-core/spec.md` after merge.
- [ ] 5.4 Update `openspec/specs/auth-foundation/spec.md` to include the new Organization locale fields.
- [ ] 5.5 `openspec validate add-genba-core --strict` passes.
- [ ] 5.6 Archive after merge to develop.
