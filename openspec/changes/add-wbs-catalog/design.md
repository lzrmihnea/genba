## Context
This is the first real implementation of the **modular-templates pattern** described in `openspec/project.md`. WBS is a hierarchical, opinionated structure that varies wildly by project type (single-family house vs apartment renovation vs office build-out) and by country (RO vs DE vs FR labor categories). Hard-coding a single locale-specific WBS tree would force users to fight the system. Templates let us ship a recommended starting point while honoring user autonomy.

The pattern documented here SHALL be reused for `add-permit-workflow` (Phase 3), `add-phase-plan` (Phase 3), and `add-payment-milestones` (Phase 4).

## Goals / Non-Goals

**Goals**
- Provide a system-managed default template (RO house) that ships with the app.
- Allow Organizations to create their own custom templates (e.g., a developer firm has its own standardized WBS).
- Make Project WBS fully editable per-Project after clone-on-create.
- Upgrade the comparison query to prefer WBS grouping when available, gracefully falling back to MatchGroup.
- Preserve while-entering recommendations across the upgrade.

**Non-Goals**
- Industry-standard WBS dictionaries (Uniformat, MasterFormat, OmniClass). The default is a pragmatic RO-house structure, not an attempt at construction-industry compliance.
- Cross-project shared editing of WBS (each Project owns its WBS).
- Automatic re-sync of a project's WBS when the source template is updated (one-way clone on create; no upstream link).
- LLM-driven WBS assignment of OfferLines (future Phase 2.5).

## Decisions

**Decision: Two separate entity hierarchies — `WBSTemplate` + `WBSTemplateItem` (library) vs `WBSItem` (per-project).**
- Alternative: shared `WBSItem` table with a discriminator (template-mode vs project-mode).
- Rationale: clean separation of concerns. Template items are bilingual and library-managed; project items are user-owned in one language. Different lifecycle, different permissions, different indexing. The duplication of "tree node" schema is small.

**Decision: Clone-on-create (one-way).**
- Alternative: keep a live reference from project WBS to template, sync changes.
- Rationale: live sync introduces hard questions ("the user renamed item 2.4 — does a template upgrade overwrite that?"). Clone-on-create gives the user a sane starting point and full ownership. We keep `source_template_item_id` for traceability/diff display, not for sync.

**Decision: Templates are bilingual (`name_en` + `name_ro`); project items are single-language.**
- Rationale: templates ship as library content reusable by EN- and RO-speaking users. Project items are user-edited and carry the user's preferred labels — forcing them to maintain two languages would friction the editing flow.
- On clone, the system picks `name_en` or `name_ro` based on the Org's `country_code` (RO orgs default to RO; others default to EN). The user can rename freely after clone.

**Decision: Comparison-query precedence — WBS > MatchGroup > Unmatched.**
- Alternative: WBS replaces MatchGroup entirely once a project adopts WBS.
- Rationale: gradual adoption. Mihnea's first 3 offers (Phase 1) used MatchGroup; when he later adopts WBS, his existing matches keep working. Lines without WBS assignment fall back to MatchGroup. The "adopt from match groups" migration helper converts MatchGroups → WBS assignments at his pace.

**Decision: WBSItem migration helper proposes via exact-label match only (no fuzzy/LLM in this change).**
- Rationale: keeps this proposal scoped. The user can always assign manually for non-exact matches. Phase 2.5 may layer in fuzzy or LLM-driven suggestion.

**Decision: WBS deletion cascades to its subtree, with subtree's OfferLine assignments set to NULL.**
- Rationale: deleting a node should not orphan children (would be data loss); but deleting should not delete linked OfferLines (would lose offer data). Compromise: delete WBS subtree, set affected OfferLines' `wbs_item_id` to NULL (they revert to unmatched / MatchGroup grouping).

## Risks / Trade-offs
- **Risk:** Two-grouping-primitives in the comparison query (WBS + MatchGroup) makes the implementation more complex than Phase 1's MatchGroup-only. → Precedence rule is clearly spec'd; integration tests cover all combinations.
- **Risk:** Cloning a 100-row template into a new Project is a bulk insert. → Single transaction; expected to take <50ms for templates up to a few hundred items.
- **Trade-off:** Bilingual template vs single-language project items is a mild inconsistency. Worth it for editing UX.
- **Risk:** The RO default template will be opinionated and may not perfectly match any specific Romanian build. → It's a STARTING POINT — by design the user customizes. The seed includes a "Diverse & Rezerve" category specifically for items that don't fit.

## Migration Plan
- Pre-flight: ensure Phase 1 ran (Project, OfferLine, MatchGroup exist).
- Changesets run in order:
  - `001-template.xml` → 002-template-item.xml → 003-project-wbs.xml → 004-offerline-wbs-fk.xml → 005-ro-default-seed.xml.
- For projects that exist BEFORE this change (Mihnea's project from Phase 1): no automatic seeding; user invokes "Seed WBS from template" from the project's WBS tab when ready.
- Rollback: `liquibase rollback` reverses 005 → 004 → 003 → 002 → 001; the `offer_line.wbs_item_id` column stays in place (it was added in Phase 1) but the FK constraint is dropped; all `wbs_*` tables are dropped.

## Open Questions
1. Should we add a `tags` field to WBSTemplateItem and WBSItem for cross-tree categorization (e.g., "high-cost", "long-lead", "permit-required")? Recommend NO for this change — defer to a future polish proposal.
2. Should template versioning be supported (so a template can be updated and users can opt into the new version)? Recommend NO for L0 — clone-on-create is one-shot; users edit their copy.
3. Should the RO default template's level-2 items include estimated quantities or unit prices (as defaults)? Recommend NO — quantities and prices are project-specific data, not template data.
4. Should we ship more than one system template (e.g., add "Apartment Renovation (RO)" and "Office Build-out (RO)")? Recommend YES for Phase 2.5 or later, but ONE template for this change to keep scope contained.
