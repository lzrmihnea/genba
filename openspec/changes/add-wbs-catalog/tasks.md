## 1. WBSTemplate (library)
- [ ] 1.1 Liquibase changeset `db.changelog-genba-wbs-001-template.xml`: `wbs_template` table — `id UUID PK`, `org_id UUID FK NULL`, `code VARCHAR(64) NOT NULL`, `name_en VARCHAR(255) NOT NULL`, `name_ro VARCHAR(255) NOT NULL`, `description TEXT`, `system_managed BOOLEAN NOT NULL DEFAULT FALSE`, `project_type_code VARCHAR(64) NULL`, `created_at`, `updated_at`, `created_by UUID FK NULL`, `deleted_at NULL`. UNIQUE(org_id, code); partial UNIQUE(code) WHERE org_id IS NULL.
- [ ] 1.2 Entity + repository + service + DTO + controller under `pxro.genba.wbs.template.*`.
- [ ] 1.3 Endpoints: `GET /api/wbs-templates` (system + active org's custom + system_managed filter), `GET /api/wbs-templates/{id}`, `POST /api/wbs-templates` (org-admin only; org-scoped row), `PUT /api/wbs-templates/{id}` (org-admin only; reject if `system_managed`), `DELETE /api/wbs-templates/{id}` (soft delete; reject system).

## 2. WBSTemplateItem (template tree nodes)
- [ ] 2.1 Liquibase changeset `db.changelog-genba-wbs-002-template-item.xml`: `wbs_template_item` table — `id UUID PK`, `template_id UUID FK NOT NULL ON DELETE CASCADE`, `parent_id UUID FK NULL`, `code VARCHAR(32) NOT NULL`, `name_en VARCHAR(255) NOT NULL`, `name_ro VARCHAR(255) NOT NULL`, `default_unit VARCHAR(32) NULL`, `sort_order INT NOT NULL DEFAULT 100`, `depth INT NOT NULL`, `embedding VECTOR(1024) NULL`, `embedding_source_hash VARCHAR(64) NULL`. Indexes (`template_id`), (`template_id, parent_id`).
- [ ] 2.2 Entity + repository (tree-traversal queries) + service + DTO.
- [ ] 2.3 Endpoints under nested resource: `GET /api/wbs-templates/{id}/items` (returns flat list with depth for client tree-build, OR nested tree at client request via `?as=tree`), `POST/PUT/DELETE` for child items (org-admin only; reject if parent template is system-managed).

## 3. WBSItem (per-Project clones)
- [ ] 3.1 Liquibase changeset `db.changelog-genba-wbs-003-project-wbs.xml`: `wbs_item` table — `id UUID PK`, `project_id UUID FK NOT NULL ON DELETE CASCADE`, `parent_id UUID FK NULL`, `code VARCHAR(32) NOT NULL`, `name VARCHAR(255) NOT NULL`, `default_unit VARCHAR(32) NULL`, `sort_order INT NOT NULL DEFAULT 100`, `depth INT NOT NULL`, `source_template_item_id UUID FK NULL`, `notes TEXT`, `embedding VECTOR(1024) NULL`, `embedding_source_hash VARCHAR(64) NULL`, `created_at`, `updated_at`, `deleted_at NULL`. Indexes (`project_id`), (`project_id, parent_id`).
- [ ] 3.2 Entity + repository + service + DTO + controller under `pxro.genba.wbs.item.*`.
- [ ] 3.3 Endpoints: `GET /api/projects/{id}/wbs` (tree or flat), `POST /api/projects/{id}/wbs` (add node), `PUT /api/wbs/{id}` (rename/move via parent_id change), `PATCH /api/wbs/{id}/order` (sort_order), `DELETE /api/wbs/{id}` (cascade soft-delete subtree).
- [ ] 3.4 Service `seedFromTemplate(projectId, templateId)`: clone the entire WBSTemplateItem tree into `wbs_item` rows; copy `name_en` OR `name_ro` based on the Org's `country_code` (RO orgs get RO names by default; others get EN); preserve `code`, `default_unit`, `sort_order`, `depth`; populate `source_template_item_id` for traceability. Single transaction.
- [ ] 3.5 Endpoint `POST /api/projects/{id}/wbs/seed-from-template/{templateId}`: REPLACE existing WBS for the project (with confirmation flag in body) OR APPEND if requested.

## 4. Project-creation flow integration
- [ ] 4.1 Extend `POST /api/projects` body with optional `wbs_template_id`. If provided, after creating the Project, invoke `seedFromTemplate`.
- [ ] 4.2 If `wbs_template_id` omitted, fall back to: (a) Org's preferred WBS template if a future Org field is added; for now, the system default `ro-house-sf-default` for RO orgs / blank otherwise.
- [ ] 4.3 Frontend: Project create drawer adds a template picker (system + custom org templates) with a "Start blank" option.

## 5. OfferLine ↔ WBS linkage
- [ ] 5.1 Liquibase changeset `db.changelog-genba-wbs-004-offerline-wbs-fk.xml`: ALTER TABLE `offer_line` ADD CONSTRAINT FK `wbs_item_id` REFERENCES `wbs_item(id)` (column already added as NULL in Phase 1; this adds the FK constraint).
- [ ] 5.2 Service: when assigning `wbs_item_id` on an OfferLine, validate the WBSItem belongs to the same Project as the OfferLine's Offer.
- [ ] 5.3 Frontend OfferLine grid: WBS picker column (autocomplete with tree drop-down) per line; bulk-assign affordance for selecting multiple lines and applying one WBS item.
- [ ] 5.4 Endpoint `PATCH /api/lines/{lineId}/wbs` `{wbs_item_id}` for direct assignment.

## 6. Comparison query upgrade (MODIFIED `bid-comparison`)
- [ ] 6.1 Service `compareOffers` updated: row key precedence — if `OfferLine.wbs_item_id IS NOT NULL`, group by `wbs_item_id`; else if line belongs to a MatchGroup, group by MatchGroup; else row is UNMATCHED.
- [ ] 6.2 Row label precedence: WBSItem.name; else MatchGroup.label; else OfferLine.label.
- [ ] 6.3 Aggregates and warnings continue to work over the precedence-resolved row set.
- [ ] 6.4 Integration test: project with mixed lines (some WBS-assigned, some MatchGroup-only, some unmatched) → comparison query groups correctly per precedence rules.

## 7. While-entering recommendations upgrade
- [ ] 7.1 Update GET `/missing-from-others` to include WBS-keyed equivalence: lines from other offers sharing a `wbs_item_id` with no equivalent in the current offer are highest-confidence recommendations.
- [ ] 7.2 When the user accepts a recommendation derived from WBS, the new OfferLine on the current offer SHALL inherit the same `wbs_item_id`.

## 8. RO default template seed
- [ ] 8.1 Liquibase data changeset `db.changelog-genba-wbs-005-ro-default-seed.xml`: insert one `wbs_template` row with `code='ro-house-sf-default'`, `name_en='Romanian Single-Family House'`, `name_ro='Casă unifamilială (RO)'`, `system_managed=TRUE`, `org_id=NULL`, `project_type_code='house-sf-ro'`.
- [ ] 8.2 Insert ~50 `wbs_template_item` rows covering 15 level-1 categories with 2-4 level-2 items each. Drafted list:
  - 1 Teren și Pregătire — 1.1 Defrișare/demolări, 1.2 Terasamente, 1.3 Trasare
  - 2 Fundație — 2.1 Săpătură, 2.2 Cofraj, 2.3 Armare, 2.4 Turnare beton, 2.5 Hidroizolație
  - 3 Structură — 3.1 Zidărie portantă, 3.2 Planșee, 3.3 Stâlpi/grinzi, 3.4 Scări
  - 4 Învelitoare — 4.1 Șarpantă, 4.2 Astereală, 4.3 Hidroizolație acoperiș, 4.4 Învelitoare
  - 5 Tâmplărie exterioară — 5.1 Ferestre, 5.2 Uși ext., 5.3 Jaluzele/Rulouri
  - 6 Instalații sanitare — 6.1 Alimentare apă, 6.2 Canalizare interioară, 6.3 Obiecte sanitare
  - 7 Instalații electrice — 7.1 Tablou + protecții, 7.2 Circuite + prize, 7.3 Iluminat, 7.4 Împământare, 7.5 Curenți slabi
  - 8 HVAC — 8.1 Sursă termică (pompă/centrală), 8.2 Distribuție/Radiatoare, 8.3 VMC, 8.4 AC
  - 9 Termoizolație & Fațadă — 9.1 Termoizolație fațadă, 9.2 Tencuieli, 9.3 Finisaj fațadă
  - 10 Finisaje interioare — 10.1 Pereți (gipscarton), 10.2 Tavane, 10.3 Pardoseli, 10.4 Vopsitorii
  - 11 Tâmplărie interioară — 11.1 Uși interioare
  - 12 Bucătărie & Mobilier fix — 12.1 Mobilier, 12.2 Electrocasnice fixe
  - 13 Amenajări exterioare — 13.1 Alei/Pavaje, 13.2 Gard/Poartă, 13.3 Peluză/Plantare, 13.4 Irigații
  - 14 Sisteme — 14.1 Alarmă/CCTV, 14.2 Smart home, 14.3 Fotovoltaice, 14.4 Baterii, 14.5 EV charger
  - 15 Diverse & Rezerve — 15.1 Diverse, 15.2 Rezervă (5–10%)
- [ ] 8.3 Each item has `name_en` (English translation) populated; `default_unit` set where unambiguous (e.g., "m³" for excavation/concrete, "m²" for surfaces, "buc" for fixtures).
- [ ] 8.4 Migration is idempotent and only inserts if no matching template exists.

## 9. WBSTemplate admin UI
- [ ] 9.1 Frontend route `app/(app)/settings/wbs-templates/page.tsx` (list system + org's custom).
- [ ] 9.2 Detail page `[templateId]/page.tsx` with tree editor (read-only for system templates).
- [ ] 9.3 Create-template flow: name, project_type_code, start from scratch OR clone-from existing template.

## 10. Project WBS editor UI
- [ ] 10.1 Tab `app/(app)/projects/[id]/wbs/page.tsx` showing the project's WBS tree.
- [ ] 10.2 Tree component (Ant Design Tree or custom): drag-reorder, indent/outdent, rename inline, delete with confirmation (cascade subtree).
- [ ] 10.3 "Re-seed from template" affordance (modal with diff preview before applying).
- [ ] 10.4 "Adopt from match groups" affordance (triggers migration-helper endpoint).

## 11. Migration helper (MatchGroup → WBS)
- [ ] 11.1 Backend `POST /api/projects/{id}/wbs/adopt-from-match-groups`: for each MatchGroup in the project, compute fuzzy-not-yet (exact-match for now) similarity between the group's `label` and the project's WBSItem.name; return proposals `[{match_group_id, suggested_wbs_item_id|null, confidence: 'EXACT'|'NONE'}]`.
- [ ] 11.2 Backend `POST /api/projects/{id}/wbs/adopt-from-match-groups/apply` `{accepted: [{match_group_id, wbs_item_id}]}` sets `wbs_item_id` on every OfferLine in those MatchGroups; optionally deletes the now-redundant MatchGroups.
- [ ] 11.3 Frontend review UI listing proposals + accept/reject per row.

## 12. Verification + spec migration
- [ ] 12.1 TestContainers integration test: create project from `ro-house-sf-default` template → verify WBSItems cloned (~50 rows); add OfferLine with WBS assignment; run comparison → verify WBS-grouping precedence.
- [ ] 12.2 Smoke test on local Mac: clone the RO default for Mihnea's project, rename a few items to match his actual scope, link existing OfferLines from Phase 1's bid-comparison-mvp to WBS items, re-run compare → see WBS-grouped rows.
- [ ] 12.3 Move spec deltas from `add-wbs-catalog/specs/wbs-catalog/spec.md` to `openspec/specs/wbs-catalog/spec.md`.
- [ ] 12.4 Update `openspec/specs/bid-comparison/spec.md` per the MODIFIED deltas.
- [ ] 12.5 `openspec validate add-wbs-catalog --strict` passes.
- [ ] 12.6 Archive after merge.
