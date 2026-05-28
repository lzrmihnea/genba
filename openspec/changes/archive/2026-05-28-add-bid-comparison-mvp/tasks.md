## 1. MatchGroup data model
- [ ] 1.1 Liquibase changeset `db.changelog-genba-007-compare.xml`: `offer_line_match_group` table — `id UUID PK`, `project_id UUID FK NOT NULL`, `label VARCHAR(512) NOT NULL`, `notes TEXT`, audit. Index `(project_id)`.
- [ ] 1.2 `match_group_line` join table — `match_group_id UUID FK NOT NULL`, `offer_line_id UUID FK NOT NULL UNIQUE`, PRIMARY KEY (match_group_id, offer_line_id). The UNIQUE on `offer_line_id` enforces one-group-per-line.
- [ ] 1.3 Entity, repository, service, controller, DTO under `eu.px.genba.compare.*`.
- [ ] 1.4 Endpoints: `POST /api/projects/{id}/match-groups` `{lines: [...], label?}` (create + add initial lines atomically), `POST /api/match-groups/{id}/lines` (add to existing), `DELETE /api/match-groups/{id}/lines/{lineId}` (remove; if group has ≤1 line after, dissolve), `PUT /api/match-groups/{id}` (label/notes), `DELETE /api/match-groups/{id}` (dissolve fully).
- [ ] 1.5 Integration test: create 3 offers × 4 lines, create match group spanning 2 lines, attempt to add a line already in another group → 409, remove a line to leave 1 → group dissolves, create empty group with single line via direct API → 422 (require ≥2 to create).

## 2. Comparison query
- [ ] 2.1 Service method `compareOffers(projectId, offerIds?)`:
  - If `offerIds` empty/null, default to all non-deleted offers in project with status ∈ {DRAFT, RECEIVED, ACCEPTED}.
  - Returns DTO `CompareResult { rows: List<CompareRow>, perOfferAggregate: Map<UUID, CompareAggregate> }`.
  - `CompareRow { rowKey: String, kind: 'GROUP'|'UNMATCHED', label, cells: Map<UUID, OfferLineDTO|null>, warnings: List<String> }`.
  - `CompareAggregate { totalExclVat, totalInclVat, matchedCount, missingCount }`.
- [ ] 2.2 Endpoint `GET /api/projects/{id}/compare?offer_ids=...&offer_ids=...` (repeat param for multi-value) returns the DTO.
- [ ] 2.3 Warning detection: `CURRENCY_MISMATCH` if any pair of lines in the same group has different `currency_code`; `UNIT_MISMATCH` if any pair differs in `unit`.
- [ ] 2.4 Result ordering: GROUP rows first by group `created_at` DESC, then UNMATCHED rows by `line_order ASC` within their offer, then by offer's `received_at` DESC.
- [ ] 2.5 Integration test: 3 offers × 5 lines, match 2 cross-offer groups, run compare → assert rows count, MISSING cells, aggregates.

## 3. Side-by-side UI shell
- [ ] 3.1 Route `app/projects/[id]/compare/page.tsx`.
- [ ] 3.2 Offer selector at top: checkboxes for each Offer in project (default all DRAFT/RECEIVED/ACCEPTED), with quick "Select all" / "Clear" actions.
- [ ] 3.3 Grid component (Ant Design Table with `sticky` columns or a custom virtualized table):
  - Frozen first column (row label).
  - One column per selected Offer.
  - Cell content: `qty × unit_price = line_total RON` with unit chip; tooltip exposes full label + description + VAT rate.
  - MISSING cell: red background, "—" symbol.
  - Footer row: per-Offer totals (excl + incl VAT, color-coded), `(M missing of N rows)` badge.
- [ ] 3.4 Row click → expand to show the original OfferLines per offer (for matched rows shows side-by-side label/description from each offer; for unmatched, just the source).

## 4. Match-mode interaction
- [ ] 4.1 Toggle button "Match mode" + keyboard shortcut M.
- [ ] 4.2 In match mode: clicking a line in Offer A selects it (highlight); clicking a line in Offer B/C/... adds to selection; pressing Enter creates a MatchGroup with all selected lines, labeled from first selected line's `label`.
- [ ] 4.3 Esc exits match mode and clears selection.
- [ ] 4.4 Visual link: paired cells share a colored dot or thin connector line, group label visible on hover.
- [ ] 4.5 "Unmatch" affordance on each grouped cell: removes that line; group dissolves to unmatched if single member remains.

## 5. Auto-suggest helper
- [ ] 5.1 Backend endpoint `POST /api/projects/{id}/compare/suggest-matches?offer_ids=...` returns a list of proposed groups, each `{label, candidateLineIds: [...]}`, derived from exact-label equality (case-insensitive, whitespace-trimmed) across the selected offers.
- [ ] 5.2 Suggestions do NOT touch the DB.
- [ ] 5.3 Frontend: modal listing all suggestions; per-suggestion accept/reject; "Accept all" / "Reject all" bulk actions; on accept, POST to /match-groups creates the group.
- [ ] 5.4 Integration test: 3 offers each containing "Hidroizolatie fundatie" (case/whitespace variants) → suggestion returns one group with three line IDs; accepting it creates the group.

## 6. Warnings + UX polish
- [ ] 6.1 Currency-mismatch indicator on the row label (small warning chip).
- [ ] 6.2 Unit-mismatch indicator (different chip color).
- [ ] 6.3 Unmatched-line counter prominent at top: "12 lines unmatched across selected offers" with a "Start matching" CTA opening match mode.
- [ ] 6.4 Empty state when no Offers exist or none selected ("Add Offers to compare" with CTA to /offers/new).

## 7. While-entering recommendations
- [ ] 7.1 Liquibase changeset (extend `db.changelog-genba-007-compare.xml` or add `-007b`): `dismissed_recommendation` table — `id UUID PK`, `project_id FK NOT NULL`, `current_offer_id FK NOT NULL`, `source_line_id FK NOT NULL`, `dismissed_by UUID FK app_user`, `dismissed_at TIMESTAMPTZ`. UNIQUE (`current_offer_id`, `source_line_id`).
- [ ] 7.2 Backend endpoint `GET /api/projects/{id}/offers/{offerId}/missing-from-others` returns a list of recommendations:
  - Compute set A = OfferLines in offer `offerId` (the current offer).
  - Compute set B = OfferLines in all OTHER non-deleted offers of project `id` with status ∈ {DRAFT, RECEIVED, ACCEPTED}.
  - For each line in B, recommend it if:
    - It belongs to a MatchGroup containing no line from set A, OR
    - Its `label` (case-insensitive, trimmed) does not match any line in set A.
  - Exclude lines that are in `dismissed_recommendation` for this `current_offer_id`.
  - Return DTO: `[{ sourceLineId, sourceOfferId, sourceVendorName, label, qty, unit, unit_price, currency, vat_rate, source_match_group_id NULL, similar_lines_in_other_offers: [...] }, ...]` ordered by `(similar_lines_in_other_offers.length DESC, sourceOfferReceivedAt DESC)` — recommendations seen across multiple competing offers float to the top.
- [ ] 7.3 Backend endpoint `POST /api/offers/{offerId}/lines/from-recommendation` accepting `{source_line_id, qty?, unit_price?, label?, description?, vat_rate?}` — copies the source line into the current offer (with overrides), then creates or extends a MatchGroup linking the new line to the source line.
- [ ] 7.4 Backend endpoint `POST /api/offers/{offerId}/recommendations/{sourceLineId}/dismiss` — inserts a `dismissed_recommendation` row; idempotent.
- [ ] 7.5 Backend endpoint `DELETE /api/offers/{offerId}/recommendations/{sourceLineId}/dismiss` — undoes a dismissal (so the recommendation reappears).
- [ ] 7.6 Frontend side-panel on the Offer edit page (`app/(app)/projects/[id]/offers/[offerId]/edit`): "Missing from your offer (N)" list, each row shows label, source vendor + amount, three actions: "Add to my offer" → opens inline qty/unit_price form pre-filled from source; "Mark as same" → opens a "pick the equivalent line in your offer" selector and creates MatchGroup; "Dismiss" → calls dismiss endpoint, removes from list with undo toast.
- [ ] 7.7 Live refresh: after adding a line or saving an OfferLine, refetch the recommendations list (TanStack Query invalidation).
- [ ] 7.8 Integration test: project with 3 offers (A=5 lines, B=4 lines, C=3 lines), open A in edit mode → recommendations panel shows the lines from B/C not in A; add one from B → recommendation disappears and a MatchGroup is created; dismiss one from C → recommendation disappears; revisit, dismissed entries stay hidden.

## 7. Verification
- [ ] 7.1 TestContainers integration test covering full happy path: project → 3 offers × 5 lines → match 3 groups → auto-suggest 2 more → run compare → verify rows, MISSING cells, aggregates, and warnings.
- [ ] 7.2 Manual smoke test on local Mac: enter Mihnea's 3 real candidate-GC offers, match the obvious overlaps, eyeball missing items and per-vendor totals against his own mental model. This is the actual product validation.
- [ ] 7.3 Move spec deltas from `openspec/changes/add-bid-comparison-mvp/specs/bid-comparison/spec.md` to `openspec/specs/bid-comparison/spec.md` after merge.
- [ ] 7.4 `openspec validate add-bid-comparison-mvp --strict` passes.
