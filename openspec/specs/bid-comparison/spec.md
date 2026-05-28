# bid-comparison

Canonical specification. Last updated 2026-05-28 via the archived
[`add-bid-comparison-mvp`](../../changes/archive/2026-05-28-add-bid-comparison-mvp/) change.

## Requirements

### Requirement: User-Driven OfferLine Matching
The system SHALL allow an authenticated user to declare two or more `OfferLine` records (across different `Offer` records within the same `Project`) as representing the equivalent scope of work, by creating a project-scoped `MatchGroup` (`OfferLineMatchGroup`). Each OfferLine MAY belong to at most one MatchGroup within a Project.

#### Scenario: Match two lines across offers
- **WHEN** an authenticated user POSTs /api/projects/{id}/match-groups with `{lines: [A1.id, B1.id], label: "Săpătură fundație"}` where A1 belongs to Offer A and B1 belongs to Offer B
- **THEN** the system SHALL create a MatchGroup containing both lines, scoped to the Project; subsequent comparison queries SHALL render those lines as a single row

#### Scenario: A line may belong to at most one MatchGroup
- **WHEN** an authenticated user attempts to add OfferLine A1 to a second MatchGroup while it is already a member of group G1
- **THEN** the system SHALL reject the request with 409 Conflict, including the existing group's id in the error body

#### Scenario: Group dissolves on falling to single membership
- **WHEN** all but one OfferLine is removed from a MatchGroup
- **THEN** the system SHALL delete the MatchGroup; the remaining OfferLine reverts to "unmatched" status

#### Scenario: Cannot create a group with fewer than two lines
- **WHEN** a user POSTs /api/projects/{id}/match-groups with `{lines: [A1.id]}`
- **THEN** the system SHALL reject the request with 422 Unprocessable Entity

#### Scenario: Cross-project membership rejected
- **WHEN** the proposed lines include OfferLines from Offers belonging to different Projects
- **THEN** the system SHALL reject the request with 422

### Requirement: Side-by-Side Comparison Query
The system SHALL provide a comparison query that, given a Project and an optional set of Offer IDs, returns a row-major structure where each row corresponds to either a MatchGroup or an unmatched OfferLine, and each column corresponds to a selected Offer, plus per-Offer aggregates for totals, matched count, and missing count.

#### Scenario: Compare three offers
- **WHEN** an authenticated user calls GET /api/projects/{id}/compare with `offer_ids=A,B,C`
- **THEN** the system SHALL return rows for every MatchGroup that contains at least one line from {A, B, C} plus every unmatched OfferLine from those Offers; each row contains `{offerId → OfferLine | null}` mapping where `null` indicates MISSING

#### Scenario: Default offer selection
- **WHEN** an authenticated user calls GET /api/projects/{id}/compare with no `offer_ids` query param
- **THEN** the system SHALL include all non-deleted Offers in the Project with status ∈ {DRAFT, RECEIVED, ACCEPTED}

#### Scenario: Aggregates per offer
- **WHEN** the comparison query is executed
- **THEN** the response SHALL include for each selected Offer an aggregate `{total_excl_vat, total_incl_vat, matched_count, missing_count}` where `total_excl_vat` and `total_incl_vat` mirror the persisted Offer totals, `matched_count` is the number of rows in which that Offer contributed a line, and `missing_count` is the number of rows in which that Offer is MISSING

#### Scenario: Missing item rendering
- **WHEN** a MatchGroup contains lines from Offers A and B but not C
- **THEN** the response row SHALL include `cells: { A: <line>, B: <line>, C: null }`

### Requirement: Mismatch Warnings
The system SHALL detect and surface, without auto-correcting, two classes of mismatch within a MatchGroup: currency mismatch (matched lines use different `currency_code`) and unit mismatch (matched lines use different `unit`), so the user can investigate before trusting comparisons.

#### Scenario: Currency mismatch warning
- **WHEN** a MatchGroup contains a line in RON and a line in EUR
- **THEN** the comparison query response row for that group SHALL include `warnings: ["CURRENCY_MISMATCH"]`; the UI SHALL render the warning visibly on the row label

#### Scenario: Unit mismatch warning
- **WHEN** a MatchGroup contains a line with `unit="m2"` and a line with `unit="m3"`
- **THEN** the response row SHALL include `warnings: ["UNIT_MISMATCH"]`

#### Scenario: Both warnings can co-exist
- **WHEN** a single MatchGroup has both kinds of mismatch
- **THEN** the response row SHALL include both `CURRENCY_MISMATCH` and `UNIT_MISMATCH` in `warnings`

### Requirement: Match-By-Exact-Label Auto-Suggest
The system SHALL provide a one-shot auto-suggest action that scans the OfferLines of the selected Offers and proposes MatchGroup creations wherever two or more lines have identical (case-insensitive, whitespace-trimmed) `label` values. Suggestions MUST NOT be persisted automatically — the user MUST confirm each proposal before the system creates the group.

#### Scenario: Auto-suggest finds duplicates
- **WHEN** Offers A, B, C each contain a line labeled "Hidroizolatie fundatie" (with possible case/whitespace differences) and the user invokes POST /api/projects/{id}/compare/suggest-matches
- **THEN** the system SHALL return a single proposal containing the three line IDs and a `label` derived from the first occurrence; no MatchGroup is created until the user accepts

#### Scenario: User rejects a suggestion
- **WHEN** the user dismisses a proposed MatchGroup in the UI
- **THEN** the system SHALL discard the proposal without persisting anything; the affected OfferLines remain unmatched

#### Scenario: User accepts a suggestion
- **WHEN** the user accepts a proposal
- **THEN** the frontend SHALL POST /api/projects/{id}/match-groups with the proposal's line IDs and label, creating the MatchGroup persistently

#### Scenario: Lines already in groups are excluded from suggestions
- **WHEN** an OfferLine is already a member of an existing MatchGroup
- **THEN** the auto-suggest scan SHALL exclude that line from any proposal

### Requirement: Future WBS-First Comparison
The comparison query SHALL be designed so that, once `add-wbs-catalog` ships in Phase 2, OfferLine.`wbs_item_id` takes precedence over MatchGroup as the grouping primitive: lines sharing the same `wbs_item_id` automatically share a row, with the WBS item name as the row label. MatchGroups remain available as a fallback when WBS is not assigned.

#### Scenario: WBS-keyed grouping takes precedence (post-Phase-2)
- **WHEN** Phase 2 has shipped and OfferLines A1, B1, C1 share `wbs_item_id = W`
- **THEN** the comparison query SHALL produce one row keyed by W with label = W.name regardless of whether a MatchGroup exists for those lines

#### Scenario: MatchGroup remains the fallback for unassigned lines (post-Phase-2)
- **WHEN** OfferLines A2, B2 are unassigned to any WBSItem but share a MatchGroup G
- **THEN** the comparison query SHALL produce one row for G with label = G.label

### Requirement: While-Entering Offer Recommendations
The system SHALL surface, during Offer editing, a list of recommendations sourced from OTHER Offers in the same Project — line items those offers contain that the current offer is missing. Detection uses two sources: existing MatchGroups (high confidence), and exact-label equality (medium confidence). Recommendations can be acted on by adding the line to the current offer (with editable qty/unit_price), marking it equivalent to an existing line, or dismissing it (persistently for this offer).

#### Scenario: Recommendations from MatchGroup membership
- **WHEN** project P has offers A and B, MatchGroup G contains a line `B1` from B but no line from A, and the user is editing A
- **THEN** GET /api/projects/{P}/offers/{A}/missing-from-others SHALL include `B1` in the recommendations, tagged with `source_match_group_id = G.id`

#### Scenario: Recommendations from label equality
- **WHEN** project P has offers A and B, B contains a line labeled "Hidroizolatie fundatie" and A contains no line with that exact label (case-insensitive, trimmed), and no MatchGroup exists for those lines yet
- **THEN** the recommendations SHALL include B's line with `source_match_group_id = null`

#### Scenario: Dismissed recommendations stay hidden
- **WHEN** the user has previously dismissed `source_line_id = L` for the current offer
- **THEN** subsequent calls to the recommendations endpoint MUST NOT include `L` in the returned list

#### Scenario: Add a recommendation to the current offer
- **WHEN** the user POSTs /api/offers/{currentOfferId}/lines/from-recommendation `{source_line_id, qty: 100, unit_price: 12.50}`
- **THEN** the system SHALL create a new OfferLine on the current offer with the source's `label`, `description`, `unit`, `vat_rate` and the provided `qty` and `unit_price`; AND SHALL create or extend a MatchGroup containing both the new line and the source line; AND SHALL recompute the current offer's totals

#### Scenario: Mark as same (without copying)
- **WHEN** the user identifies an existing OfferLine `L_current` in the current offer that represents the same scope as a recommended source line `L_source`
- **THEN** the user MAY create a MatchGroup containing both, via the existing POST /api/projects/{id}/match-groups endpoint; the recommendation SHALL stop appearing because `L_current` now belongs to a MatchGroup shared with `L_source`

#### Scenario: Dismiss a recommendation persistently
- **WHEN** the user POSTs /api/offers/{currentOfferId}/recommendations/{sourceLineId}/dismiss
- **THEN** the system SHALL insert a `dismissed_recommendation` row; idempotent on conflict; the dismissal persists across sessions until reversed via DELETE on the same endpoint

#### Scenario: Recommendations exclude deleted and out-of-scope offers
- **WHEN** an Offer C in the project has `status=EXPIRED` or `deleted_at IS NOT NULL`
- **THEN** lines from C MUST NOT appear in recommendations

#### Scenario: Ordering surfaces consensus
- **WHEN** the same line concept appears in 3 other offers (e.g., A and B and C each have a "Săpătură fundație" line that the current Offer D lacks)
- **THEN** that recommendation SHALL appear higher in the list than a line that appears in only one other offer
