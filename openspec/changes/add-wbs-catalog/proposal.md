## Why
Phase 1's bid comparison uses user-driven MatchGroup pairing — fine for the first 2–3 offers but fragile across projects and not extensible. WBS (Work Breakdown Structure) is the standard primitive of construction estimating: a hierarchical taxonomy of work items (e.g., "2.4 Turnare beton fundație") that every offer can be aligned against. With WBS in place, comparison becomes structural (group by WBS code) and reusable across projects. Per the modular-templates principle, WBS is a TEMPLATE library — Mihnea (and future SaaS users) pick a template at project creation, get a starting tree, and freely edit, extend, or rebuild it for their specific build. The seed template is "Romanian Single-Family House"; users can create custom templates per Organization.

## What Changes
- **ADD**: `WBSTemplate` entity (library) — `{id, org_id NULL (system-managed global if NULL), code VARCHAR(64), name_en, name_ro, description, system_managed BOOLEAN, project_type_code VARCHAR(64) NULL (e.g., 'house-sf', 'apartment-reno', 'office-buildout'), audit fields}`. System ships with one global template: code `ro-house-sf-default`, name "Romanian Single-Family House" / "Casă unifamilială (RO)".
- **ADD**: `WBSTemplateItem` entity (template-tree node) — `{id, template_id FK, parent_id FK NULL, code VARCHAR(32) (e.g., "2.4"), name_en, name_ro, default_unit VARCHAR(32) NULL, sort_order INT, depth INT, embedding VECTOR(1024) NULL, embedding_source_hash VARCHAR(64) NULL}`. Hierarchical via `parent_id`. Embedding column NULL until `add-semantic-search` populates it.
- **ADD**: `WBSItem` entity (per-Project clone, fully editable) — `{id, project_id FK, parent_id FK NULL, code, name, default_unit, sort_order, depth, source_template_item_id FK NULL (traceability — which template node this was cloned from; NULL for user-created), notes, embedding VECTOR(1024) NULL, embedding_source_hash VARCHAR(64) NULL}`. NOT bilingual — owns one user-chosen label. Embedding column NULL until `add-semantic-search` populates it.
- **ADD**: Project creation flow extended — when POSTing /api/projects, optional `wbs_template_id` triggers cloning the entire template tree into per-Project `WBSItem` rows; if omitted, falls back to Organization's `permit_workflow_template_id`-style default OR creates the Project with no WBS (blank). Also: `POST /api/projects/{id}/wbs/seed-from-template/{templateId}` lets a user replace/seed WBS on an existing Project.
- **ADD**: `OfferLine.wbs_item_id` FK NULL (per-project) — allow assigning each line to a WBS node. Assignment UI in the OfferLine grid: an autocomplete/tree picker.
- **MODIFIED** (capability `bid-comparison`): Comparison query SHALL prefer WBS-based grouping over MatchGroup when WBS is populated; MatchGroup remains the fallback for unassigned lines. While-entering recommendations SHALL also leverage WBS-keyed equivalence as the highest-confidence source.
- **ADD**: WBSTemplate CRUD UI (org admins manage custom templates).
- **ADD**: WBSItem editor UI on Project (drag-reorder, indent/outdent, rename, delete; tree view).
- **ADD**: Seed data files for the RO default template — ~50 entries covering the 15 level-1 categories (Teren, Fundație, Structură, Învelitoare, Tâmplărie ext., Inst. sanitare, Inst. electrice, HVAC, Termoizolație, Finisaje int., Tâmplărie int., Bucătărie/Mobilier, Amenajări ext., Sisteme, Diverse) with representative level-2 items each.
- **ADD**: Migration helper — `POST /api/projects/{id}/wbs/adopt-from-match-groups` proposes mapping existing MatchGroups to WBS items based on group label similarity to template item names; user reviews and confirms each suggestion.

## Impact
- **Affected specs**: NEW capability `wbs-catalog`. MODIFIED capability `bid-comparison` (comparison query and while-entering recommendations prefer WBS).
- **Affected code**:
  - New backend packages: `pxro.genba.wbs.template.*`, `pxro.genba.wbs.item.*`.
  - Modified: `pxro.genba.offer.OfferLine` (add `wbs_item_id` FK), `pxro.genba.compare.*` (group-by upgrades).
  - New Liquibase changesets: `db.changelog-genba-wbs-001-template.xml`, `-002-template-item.xml`, `-003-project-wbs.xml`, `-004-offerline-wbs-fk.xml`, `-005-ro-default-seed.xml`.
  - New frontend routes: `app/(app)/settings/wbs-templates/*` (org-admin CRUD), `app/(app)/projects/[id]/wbs/*` (per-project editor).
  - Modified: OfferLine grid in Offer entry — WBS picker per line.
  - Modified: comparison view at `app/(app)/projects/[id]/compare/page.tsx` — switches to WBS-first grouping with MatchGroup fallback.
- **Risk**: Comparison-query backend now has two grouping primitives (WBS + MatchGroup) that must coexist. Mitigation: precedence rule documented in spec; integration tests cover both paths and the mixed case.
- **Risk**: Cloning a 100-row template into a new Project is bulk inserts; do in a single transaction. Acceptable performance.
- **Risk**: Migration helper proposes WBS assignments via label similarity (still Phase 1's exact-match logic). May produce no suggestions for divergent labels — users would do manual mapping. Acceptable for Layer 0; Phase 2.5 could add an LLM-driven helper.
- **Trade-off:** Bilingual labels on templates vs single-language labels on per-Project items. Rationale: templates are library content shipped or shared; user-owned clones use the user's chosen single language to avoid forced bilingual editing. Acceptable mild inconsistency.
