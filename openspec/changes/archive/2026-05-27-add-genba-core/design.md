## Context
With `add-auth-foundation` providing the scaffolding (Spring Boot + Next.js + Postgres + Liquibase + JWT + User/Organization + i18n) referenced from kaisha-03 patterns but built independently, this change introduces the construction-domain primitives. Two design decisions deserve documentation because every later capability depends on them: (1) the polymorphic `Attachment` pattern, and (2) the `attachment_source_channel` lookup table.

## Goals / Non-Goals

**Goals**
- Add a single `Project` entity representing one house build, org-scoped.
- Extend the existing `Organization` with locale fields so internationalization is data-driven.
- Establish polymorphic `Attachment` as the universal "evidence" pattern — uniform backend, uniform UI components.
- Allow user-extensible attachment source channels while preserving a stable system-managed set.

**Non-Goals**
- Production deployment (Hetzner, Traefik, CI/CD).
- Subscription / billing (no `tier` column yet).
- File-content extraction (OCR, LLM parsing).
- WBS, Permits, Phases, Budget — separate proposals.
- Mobile (Capacitor) wiring.

## Decisions

**Decision: Polymorphic `Attachment` via `{attachable_type, attachable_id}` rather than per-parent join tables.**
- Alternative: per-parent join tables (`project_attachment`, `offer_attachment`, ...), type-safe and indexed per FK.
- Rationale: Attachments will adorn ~15+ entity types by Phase 4 (Project, Offer, OfferLine, Permit, Phase, ChangeOrder, Document, BudgetLine, ActualSpend, PhotoLog, Vendor, ...). Per-parent join tables = boilerplate explosion. Polymorphic with a compound index `(attachable_type, attachable_id)` provides acceptable query performance for the expected scale. Downside: no DB-level FK integrity. Mitigated by: parent rows use soft-delete (not hard-delete) so dangling references are bounded; a future scheduled job sweeps orphans.

**Decision: `Attachment.kind` ∈ {FILE, TEXT, LINK} orthogonal to `source_channel`.**
- Rationale: `kind` describes storage shape (file on disk vs. inline text vs. external URL); `source_channel` describes provenance (whatsapp, email, voice, pdf, ...). Independent: a PDF is `(FILE, pdf)`, a pasted WhatsApp message is `(TEXT, whatsapp)`, a Google Drive link is `(LINK, link)`. Future channels (e.g., `whatsapp-api`, `email-forward`) can be added by editing the lookup table.

**Decision: `attachment_source_channel` as a lookup table with both system-managed and org-scoped custom rows.**
- Alternative: hardcoded enum in code.
- Rationale (added per user request): Users may invent their own channels ("site visit photo notes", "subcontractor referral", "supplier catalog"). Hardcoded enums require code changes. Lookup-table approach allows org admins to extend without engineering work. System rows (`org_id IS NULL`, `system_managed=true`) are immutable and global; custom rows are org-scoped and editable by org admins, capped at 32 per org to prevent UI degradation.

**Decision: Files stored on local disk in Layer 0; S3-compatible abstraction left for Layer 1.**
- Rationale: Layer 0 is local-only; S3/MinIO is premature. Encapsulate file IO behind `AttachmentStorageService` so the swap is a single-class change later.

**Decision: Locale fields denormalized onto Organization (vs. a separate `locale` table).**
- Rationale: <50 locales total; locale is queried with every Organization read. Inlining 4 columns is cheaper than a join. Permit workflows ARE normalized into a separate template table (Phase 3) because they have substantial structure.

**Decision: `permit_workflow_template_id` is a *default pointer*, not a hard binding.**
- Rationale: per the modular-templates principle, new Projects use this template as a starting structure but own a cloned copy they can edit freely. The Org-level default reduces friction in the common case (same workflow for all projects under one org).

## Risks / Trade-offs
- **Risk:** Polymorphic Attachment has no DB-level FK integrity to parent rows. → Soft-delete everywhere; future sweep job.
- **Risk:** Custom source channels could fragment data analysis later. → System set covers ~95% of cases; custom rows are tagged and easy to filter.
- **Risk:** `source_channel_id FK NOT NULL` means we can't insert an attachment before resolving the channel. → Frontend always passes a channel; system `other` is the catch-all.
- **Trade-off:** Adding 4 columns to Organization is a backward-compatible ALTER with DEFAULTs. Safe for existing single seed row.

## Migration Plan
- All changesets run against the post-auth-foundation database state.
- Each Liquibase changeset is atomic and reversible via `rollback` blocks.
- Rollback path: `liquibase rollbackToTag` to the tag set just before this change's first changeset.

## Open Questions
1. Cap on file size — proposal says 25 MB. Confirm or adjust based on real attachment sizes (typical PDFs from RO contractors are 1–8 MB; site photos can hit 12 MB).
2. Should `Attachment.uploaded_by` cascade or set-null when the user is soft-deleted? Recommend SET NULL (rare event, preserves audit chain).
3. Should the channel selector default to `other` or to the channel last used by this user? Recommend last-used (small productivity gain).
