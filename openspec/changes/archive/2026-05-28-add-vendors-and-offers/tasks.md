## 1. Vendor entity
- [ ] 1.1 Liquibase changeset `db.changelog-genba-005-vendor.xml`: `vendor` table — `id UUID PK`, `org_id UUID FK NOT NULL`, `project_id UUID FK NULL`, `name VARCHAR(255) NOT NULL`, `contact_name VARCHAR(255)`, `phone VARCHAR(64)`, `email VARCHAR(255)`, `vat_id VARCHAR(64)` (RO: CUI/CIF), `default_retention_pct NUMERIC(5,2)`, `notes TEXT`, `embedding VECTOR(1024) NULL`, `embedding_source_hash VARCHAR(64) NULL`, audit + `deleted_at`. Index `(org_id, deleted_at)`, `(project_id, deleted_at)`.
- [ ] 1.2 Entity, repository, service, controller, DTO under `eu.px.genba.vendor.*`.
- [ ] 1.3 Endpoints: `GET /api/vendors` (filter `?project_id=...` returns org-wide + project-scoped), `GET /api/vendors/{id}`, `POST /api/vendors`, `PUT /api/vendors/{id}`, `DELETE /api/vendors/{id}` (soft delete).
- [ ] 1.4 Frontend: vendors index page, create/edit drawer, vendor picker component (used in Offer creation), project-detail "Vendors" tab.
- [ ] 1.5 Integration test: create org-scoped vendor, create project-scoped vendor, verify list filters correctly.

## 2. Offer entity (parent)
- [ ] 2.1 Liquibase changeset `db.changelog-genba-006-offer.xml`: `offer` and `offer_line` tables. `offer` — `id UUID PK`, `org_id`, `project_id FK NOT NULL`, `vendor_id FK NOT NULL`, `label VARCHAR(255)`, `received_at DATE`, `valid_until DATE NULL`, `currency_code VARCHAR(3) NOT NULL`, `total_amount_excl_vat NUMERIC(14,2) NOT NULL DEFAULT 0`, `total_amount_incl_vat NUMERIC(14,2) NOT NULL DEFAULT 0`, `status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'`, `notes TEXT`, audit + `deleted_at`. Index `(project_id, status)`.
- [ ] 2.2 Entity + repository + service + controller + DTO under `eu.px.genba.offer.*`. Service exposes `recomputeTotals(offerId)` and ensures it's invoked on any OfferLine mutation under `@Transactional`.
- [ ] 2.3 Endpoints: `GET /api/offers?project_id=...&status=...`, `GET /api/offers/{id}` (returns offer + nested lines + attachment summary), `POST /api/offers`, `PUT /api/offers/{id}`, `PATCH /api/offers/{id}/status` (validates allowed transitions), `DELETE /api/offers/{id}`.
- [ ] 2.4 Status transitions enforced in service: DRAFT→RECEIVED, RECEIVED→ACCEPTED|REJECTED|EXPIRED, ACCEPTED→EXPIRED (e.g., if validity passed), REJECTED→DRAFT (allow correction), EXPIRED is terminal. Any other transition → 422.
- [ ] 2.5 Clone endpoint `POST /api/offers/{id}/clone` accepting optional `{vendor_id, label, received_at}` body; creates a new DRAFT Offer copying `currency_code` + all OfferLines (fresh ids; `wbs_item_id` and `normalized_key` reset to NULL). Attachments NOT copied. Returns new offer + lines.
- [ ] 2.6 Frontend: "Clone offer" action button on offer detail; opens modal pre-filled with source's vendor + label suffix "(copy)"; user can change vendor/label/date before confirming; on success navigates to new offer's edit page.

## 3. OfferLine sub-entity
- [ ] 3.1 `offer_line` table — `id UUID PK`, `offer_id FK NOT NULL ON DELETE CASCADE`, `line_order INT NOT NULL`, `label VARCHAR(512) NOT NULL`, `description TEXT`, `qty NUMERIC(14,4) NOT NULL DEFAULT 1`, `unit VARCHAR(32) NOT NULL DEFAULT 'buc'`, `unit_price NUMERIC(14,4) NOT NULL`, `currency_code VARCHAR(3) NOT NULL`, `vat_rate NUMERIC(5,2) NOT NULL DEFAULT 19.00`, `line_total_excl_vat NUMERIC(14,2) NOT NULL`, `line_total_incl_vat NUMERIC(14,2) NOT NULL`, `wbs_item_id UUID NULL` (FK added in Phase 2), `normalized_key VARCHAR(255) NULL`, `notes TEXT`, `embedding VECTOR(1024) NULL`, `embedding_source_hash VARCHAR(64) NULL`, audit. Index `(offer_id, line_order)`.
- [ ] 3.2 OfferLine entity + child-collection mapping on Offer (lazy-fetch by default; explicit fetch-join when reading full Offer).
- [ ] 3.3 Endpoints: `POST /api/offers/{id}/lines` (single), `POST /api/offers/{id}/lines/bulk` (array, atomic), `PUT /api/lines/{lineId}`, `PATCH /api/lines/{lineId}/order`, `DELETE /api/lines/{lineId}`.
- [ ] 3.4 Service computes line totals server-side from `qty * unit_price` and applies `vat_rate`. Client may not bypass.
- [ ] 3.5 Integration test: create offer, bulk-insert 10 lines, edit 2, delete 1 → verify Offer totals match sum of remaining lines.

## 4. Frontend Offer entry UX
- [ ] 4.1 New-offer page at `app/projects/[id]/offers/new`: header card with vendor picker (Vendor created inline if missing), project context, label, dates, currency.
- [ ] 4.2 OfferLine grid component (Ant Design Table editable or custom): Tab/Shift-Tab between cells, Enter on last cell adds row, Esc cancels current edit, autosave on blur.
- [ ] 4.3 Inline-add row at the bottom of the grid.
- [ ] 4.4 Drag-handle to reorder rows; PATCH order via batched call on drag-end.
- [ ] 4.5 "Paste from spreadsheet" textarea: parses TSV/CSV via PapaParse (or similar); preview the proposed lines; user confirms → POST /lines/bulk.
- [ ] 4.6 Status transition control (Ant Design `Steps` or button group respecting allowed transitions).
- [ ] 4.7 Attachment list at bottom of offer detail using `<AttachmentList type="OFFER" id={offerId} />` and `<AttachmentUpload type="OFFER" id={offerId} channels={["pdf","image","whatsapp","email","voice","verbal","link"]} />`.
- [ ] 4.8 Computed footer in offer detail: per-Offer total excl/incl VAT (mirrors backend persisted values).

## 5. Verification
- [ ] 5.1 TestContainers integration test: vendor → offer → 3 lines → bulk-paste 5 more → totals computed, status transitions enforced.
- [ ] 5.2 Manual smoke test on local Mac: enter 3 real offers from current GC discussions, attach one PDF and one pasted WhatsApp text per offer.
- [ ] 5.3 Move spec deltas from `openspec/changes/add-vendors-and-offers/specs/vendors-and-offers/spec.md` to `openspec/specs/vendors-and-offers/spec.md` after merge.
- [ ] 5.4 `openspec validate add-vendors-and-offers --strict` passes.
