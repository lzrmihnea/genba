## MODIFIED Requirements

### Requirement: Side-by-Side Comparison Query
The system SHALL provide a comparison query that, given a Project and an optional set of Offer IDs, returns a row-major structure where each row corresponds to a grouping key resolved by precedence — WBSItem first, then MatchGroup, then unmatched OfferLine — and each column corresponds to a selected Offer, plus per-Offer aggregates for totals, matched count, and missing count.

#### Scenario: Compare three offers
- **WHEN** an authenticated user calls GET /api/projects/{id}/compare with `offer_ids=A,B,C`
- **THEN** the system SHALL return rows for every grouping key present across those offers; each row contains `{offerId → OfferLine | null}` mapping where `null` indicates MISSING

#### Scenario: Default offer selection
- **WHEN** an authenticated user calls GET /api/projects/{id}/compare with no `offer_ids` query param
- **THEN** the system SHALL include all non-deleted Offers in the Project with status ∈ {DRAFT, RECEIVED, ACCEPTED}

#### Scenario: WBSItem grouping takes precedence over MatchGroup
- **WHEN** OfferLines A1 (offer A) and B1 (offer B) share `wbs_item_id = W`, AND A1 also belongs to a MatchGroup G with C1 from offer C
- **THEN** the comparison query SHALL produce ONE row keyed by W with cells `{A: A1, B: B1, C: C1}` (C1 included because its MatchGroup G overlaps with the WBS row through A1); the row label = WBSItem W's name; the row's `kind` is `WBS`

#### Scenario: MatchGroup is the fallback when WBS is absent
- **WHEN** OfferLines D1, E1 share a MatchGroup but neither has `wbs_item_id`
- **THEN** the system SHALL produce one row keyed by the MatchGroup, with label = MatchGroup.label, `kind` = `GROUP`

#### Scenario: Unmatched fallback
- **WHEN** an OfferLine has neither `wbs_item_id` nor MatchGroup membership
- **THEN** the comparison query SHALL produce one row per such line with `kind` = `UNMATCHED`, label = OfferLine.label

#### Scenario: Aggregates per offer
- **WHEN** the comparison query is executed
- **THEN** the response SHALL include for each selected Offer an aggregate `{total_excl_vat, total_incl_vat, matched_count, missing_count}` mirroring persisted Offer totals; `matched_count` = number of rows in which the Offer contributed a line; `missing_count` = number of rows in which the Offer is MISSING

#### Scenario: Missing item rendering
- **WHEN** a row (keyed by WBSItem W) contains lines from Offers A and B but not C
- **THEN** the response row SHALL include `cells: { A: <line>, B: <line>, C: null }`

#### Scenario: Row ordering
- **WHEN** the comparison query returns rows
- **THEN** WBS-keyed rows SHALL be ordered by WBSItem `sort_order` ASC then `code` ASC; then MatchGroup-keyed rows by `created_at` DESC; then UNMATCHED rows by source-Offer `received_at` DESC then `line_order` ASC

### Requirement: While-Entering Offer Recommendations
The system SHALL surface, during Offer editing, a list of recommendations sourced from OTHER Offers in the same Project — line items those offers contain that the current offer is missing. Detection uses three sources in priority order: shared WBSItem (highest confidence; available once Phase 2 ships), existing MatchGroup, and exact-label equality. Recommendations can be acted on by adding the line to the current offer (with editable qty/unit_price), marking it equivalent to an existing line, or dismissing it persistently for this offer.

#### Scenario: WBS-keyed recommendations take highest priority
- **WHEN** the user is editing offer A, and offer B has a line `B1` with `wbs_item_id = W` where the current offer A has NO line assigned to W
- **THEN** the recommendations response SHALL include `B1` with source = `WBS`, ordered above MatchGroup- or label-derived suggestions

#### Scenario: Adding a WBS-derived recommendation inherits WBS assignment
- **WHEN** the user accepts a recommendation derived from WBS via POST /api/offers/{currentOfferId}/lines/from-recommendation `{source_line_id: B1.id}`
- **THEN** the new OfferLine on the current offer SHALL inherit `wbs_item_id = W`; AND a MatchGroup linking the new line to `B1` MAY also be created for traceability (though the WBS link is the primary equivalence)

#### Scenario: MatchGroup recommendations still surface
- **WHEN** a line belongs to a MatchGroup that has no line from the current offer (and no WBS keying applies)
- **THEN** the recommendation SHALL include it with source = `MATCH_GROUP`

#### Scenario: Label-equality recommendations are last
- **WHEN** a line in another offer has neither WBS assignment nor MatchGroup overlap with the current offer
- **THEN** the recommendation MAY include it with source = `LABEL`, ordered below WBS and MatchGroup sources

#### Scenario: Dismissed recommendations stay hidden
- **WHEN** the user has dismissed `source_line_id = L` for the current offer
- **THEN** subsequent recommendations endpoints MUST NOT include `L` regardless of source
