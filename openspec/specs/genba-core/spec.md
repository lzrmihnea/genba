# genba-core

Canonical specification. Last updated 2026-05-27 via the archived
[`add-genba-core`](../../changes/archive/2026-05-27-add-genba-core/) change.

## Requirements

### Requirement: Project Entity
The system SHALL provide a `Project` entity representing a single house build, scoped to exactly one Organization, with name, description, address, base currency, status, soft-delete support, and standard audit fields.

#### Scenario: Create a project under an Organization
- **WHEN** an authenticated user with org_role ADMIN/OWNER/MEMBER in Organization X submits POST /api/projects with `{name, base_currency}`
- **THEN** the system SHALL create a Project with `org_id = X`, `status = PLANNING`, and `base_currency` either as provided or defaulting to the Organization's `currency_code` if omitted

#### Scenario: List projects within Organization
- **WHEN** an authenticated user requests GET /api/projects
- **THEN** the system SHALL return only Projects belonging to the user's active Organization, excluding soft-deleted entries

#### Scenario: Soft-delete a project
- **WHEN** an authenticated ADMIN/OWNER issues DELETE /api/projects/{id}
- **THEN** the system SHALL set `deleted_at` on the project; subsequent list and read operations MUST exclude it; child Offers and Attachments remain in the database for audit

#### Scenario: Cannot access another Organization's project
- **WHEN** an authenticated user from Organization X requests GET /api/projects/{id} where the project belongs to Organization Y
- **THEN** the system SHALL respond 404 (not 403) to avoid leaking existence

### Requirement: Attachment Source-Channel Lookup
The system SHALL provide an `attachment_source_channel` lookup table containing both immutable system-managed rows (global, `org_id IS NULL`, `system_managed=true`) and user-extensible org-scoped custom rows. Org admins can add custom channels (cap of 32 per Organization) and delete only their own custom rows; system rows are read-only.

#### Scenario: List channels combines system and custom rows
- **WHEN** an authenticated user GETs /api/attachment-channels
- **THEN** the response SHALL include all system rows plus all non-deleted custom rows for the user's active Organization, ordered by `sort_order` ASC then `code` ASC

#### Scenario: Org admin creates a custom channel
- **WHEN** an authenticated ADMIN/OWNER POSTs `{code: "site-visit", label_en: "Site visit notes", label_ro: "Note vizită șantier"}`
- **THEN** the system SHALL create a new row with `org_id` = active org, `system_managed=false`

#### Scenario: Cannot delete a system channel
- **WHEN** any user DELETEs /api/attachment-channels/{id} where the row has `system_managed=true`
- **THEN** the system SHALL respond 403 (Forbidden) with i18n key `attachment.error.systemChannelImmutable`

#### Scenario: Custom channel cap enforced
- **WHEN** an Org has 32 custom (non-deleted) channels and an admin POSTs a 33rd
- **THEN** the system SHALL respond 422 with i18n key `attachment.error.channelCapExceeded`

### Requirement: Polymorphic Attachment
The system SHALL provide a polymorphic `Attachment` entity that attaches to any domain entity via `{attachable_type, attachable_id}`, supports three kinds (FILE, TEXT, LINK), references a `source_channel` from the lookup table, and treats files as optional rather than required to persist the parent record.

#### Scenario: Attach a PDF to a Project
- **WHEN** an authenticated user POSTs a multipart form to /api/attachments with `attachable_type=PROJECT`, `attachable_id=<uuid>`, a PDF file, and `source_channel_id=<pdf-channel-uuid>`
- **THEN** the system SHALL persist the file to local storage at the configured path, create an Attachment row with `kind=FILE`, `mime_type=application/pdf`, and return the Attachment metadata

#### Scenario: Attach pasted WhatsApp text (no file)
- **WHEN** an authenticated user POSTs JSON `{attachable_type, attachable_id, source_channel_id: <whatsapp-uuid>, raw_text: "..."}`
- **THEN** the system SHALL create an Attachment row with `kind=TEXT`, `file_url=null`, preserving `raw_text` verbatim

#### Scenario: Attach an external link
- **WHEN** an authenticated user POSTs JSON `{attachable_type, attachable_id, source_channel_id: <link-uuid>, file_url: "https://drive.google.com/..."}`
- **THEN** the system SHALL create an Attachment row with `kind=LINK`, `raw_text=null`

#### Scenario: List attachments for an entity
- **WHEN** GET /api/attachments?type=OFFER&id=<uuid> is called
- **THEN** the system SHALL return all Attachments matching that (type, id) AND the user's active org, ordered by `uploaded_at` descending

#### Scenario: Files are optional for entity persistence
- **WHEN** any parent entity is created with zero attachments
- **THEN** the entity SHALL be created successfully; the system MUST NOT require any attachment to persist structured data

#### Scenario: Download a file attachment
- **WHEN** an authenticated user requests GET /api/attachments/{id}/download where `kind=FILE`
- **THEN** the system SHALL stream the file with the original `mime_type` and `original_filename` in Content-Disposition; access requires the attachment's `org_id` match the user's active org

#### Scenario: Cross-organization download blocked
- **WHEN** a user from Organization X requests download of an Attachment whose `org_id=Y`
- **THEN** the system SHALL respond 404
