## ADDED Requirements

### Requirement: WBSTemplate Library
The system SHALL provide a `WBSTemplate` library entity holding both system-managed global templates (`org_id IS NULL`, `system_managed=TRUE`, immutable) and Organization-managed custom templates. Templates are bilingual (`name_en`, `name_ro`), categorizable by `project_type_code`, and form the source-of-truth library from which per-Project WBS structures are cloned.

#### Scenario: List templates combines system and org-managed
- **WHEN** an authenticated user GETs /api/wbs-templates
- **THEN** the response SHALL include all non-deleted system rows plus all non-deleted templates for the user's active Organization

#### Scenario: Org admin creates a custom template
- **WHEN** an authenticated ADMIN/OWNER POSTs `{code: "developer-X-standard", name_en: "...", name_ro: "...", project_type_code: "house-sf"}`
- **THEN** the system SHALL create a row with `org_id` = active org, `system_managed=FALSE`

#### Scenario: System template is immutable
- **WHEN** any user PUTs or DELETEs a system-managed template
- **THEN** the system SHALL respond 403 with i18n key `wbs.error.systemTemplateImmutable`

#### Scenario: Romanian Single-Family House ships by default
- **WHEN** the database has been migrated through `db.changelog-genba-wbs-005-ro-default-seed.xml`
- **THEN** GET /api/wbs-templates SHALL include a row with `code='ro-house-sf-default'`, `name_ro='Casă unifamilială (RO)'`, `name_en='Romanian Single-Family House'`, `system_managed=TRUE`, populated WBSTemplateItem hierarchy

### Requirement: WBSTemplateItem Hierarchy
Each `WBSTemplate` SHALL contain a hierarchical structure of `WBSTemplateItem` nodes (parent-child via `parent_id`), each carrying a `code` (e.g., "2.4"), bilingual names, optional `default_unit`, and `sort_order` for sibling ordering.

#### Scenario: Fetch template tree
- **WHEN** an authenticated user GETs /api/wbs-templates/{id}/items?as=tree
- **THEN** the response SHALL return the full hierarchy nested by `parent_id`, sorted by `sort_order` at each level

#### Scenario: Add item to a custom template
- **WHEN** an org admin POSTs /api/wbs-templates/{customTemplateId}/items with `{parent_id, code, name_en, name_ro, default_unit}`
- **THEN** the system SHALL create the item; `depth` is computed from parent's depth + 1; rejecting if the template is system-managed

### Requirement: Per-Project WBSItem (Cloned, Editable)
The system SHALL provide a `WBSItem` entity scoped to a single Project, representing the Project's own WBS tree — cloned from a chosen template at Project creation OR built from scratch — and fully editable by the project's users without affecting the source template.

#### Scenario: Create Project with WBS template
- **WHEN** an authenticated user POSTs /api/projects with `{name, base_currency, wbs_template_id}`
- **THEN** the system SHALL create the Project AND clone the entire WBSTemplateItem tree into per-Project WBSItem rows, copying `code`, `default_unit`, `sort_order`, `depth`, populating `source_template_item_id` for traceability, and selecting `name_en` OR `name_ro` based on the Organization's `country_code` (RO → RO name; otherwise EN)

#### Scenario: Create Project blank (no template)
- **WHEN** the request omits `wbs_template_id` AND the system has no Organization-default
- **THEN** the Project SHALL be created with zero WBSItems; the user can add nodes manually via the WBS editor

#### Scenario: Edit a WBSItem freely
- **WHEN** an authenticated user PUTs /api/wbs/{id} with new `name` or PATCHes its `parent_id` or `sort_order`
- **THEN** the system SHALL persist the edit; the source template MUST NOT change; `source_template_item_id` remains for traceability

#### Scenario: Delete a WBSItem cascades to subtree
- **WHEN** an authenticated user DELETEs /api/wbs/{id}
- **THEN** the system SHALL soft-delete that node and all descendants; any OfferLines whose `wbs_item_id` falls in the deleted subtree SHALL have their `wbs_item_id` set to NULL (they revert to MatchGroup or unmatched)

#### Scenario: Re-seed WBS on an existing Project
- **WHEN** an authenticated user with project access POSTs /api/projects/{id}/wbs/seed-from-template/{templateId} with `{mode: "replace", confirmReplace: true}`
- **THEN** the system SHALL delete the existing WBSItems (setting OfferLine.wbs_item_id to NULL on affected lines) and clone the new template tree

#### Scenario: Append to existing WBS
- **WHEN** the request body is `{mode: "append"}`
- **THEN** the system SHALL keep existing WBSItems and add the template's items as siblings at the root level (or under a designated parent if provided)

### Requirement: OfferLine ↔ WBSItem Linkage
The system SHALL allow each `OfferLine` to optionally reference one `WBSItem` (via `wbs_item_id`) belonging to the same Project as the OfferLine's parent Offer.

#### Scenario: Assign a WBSItem to an OfferLine
- **WHEN** an authenticated user PATCHes /api/lines/{lineId}/wbs `{wbs_item_id}` where the target WBSItem belongs to the same Project as the line's Offer
- **THEN** the system SHALL set the FK and return the updated OfferLine

#### Scenario: Reject cross-project WBS assignment
- **WHEN** the target WBSItem belongs to a different Project
- **THEN** the system SHALL respond 422 with i18n key `wbs.error.crossProjectAssignment`

#### Scenario: Clear WBS assignment
- **WHEN** PATCHing with `{wbs_item_id: null}`
- **THEN** the system SHALL clear the assignment; the line reverts to MatchGroup or unmatched in the comparison view

### Requirement: Migration Helper from MatchGroups to WBS
The system SHALL provide an opt-in migration that suggests mapping existing MatchGroups in a Project to its WBSItems based on label equality (case-insensitive, whitespace-trimmed) between `MatchGroup.label` and `WBSItem.name`. Suggestions are NOT applied without user confirmation.

#### Scenario: Generate migration suggestions
- **WHEN** an authenticated user POSTs /api/projects/{id}/wbs/adopt-from-match-groups
- **THEN** the system SHALL return `[{match_group_id, match_group_label, suggested_wbs_item_id|null, confidence: 'EXACT'|'NONE'}]` for every MatchGroup in the project

#### Scenario: Apply selected migrations
- **WHEN** the user POSTs /api/projects/{id}/wbs/adopt-from-match-groups/apply `{accepted: [{match_group_id, wbs_item_id}, ...]}`
- **THEN** for each accepted pair, the system SHALL set `OfferLine.wbs_item_id = wbs_item_id` on every OfferLine in that MatchGroup; the MatchGroups themselves remain (so the user can revert) unless `deleteMigratedGroups: true` is set
