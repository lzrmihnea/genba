# vendors-and-offers

Canonical specification. Last updated 2026-05-28 via the archived
[`add-vendors-and-offers`](../../changes/archive/2026-05-28-add-vendors-and-offers/) change.

## Requirements

### Requirement: Vendor Entity
The system SHALL provide a `Vendor` entity representing any party who can issue an Offer (constructor, subcontractor, supplier), scoped to one Organization, with optional Project scoping for project-specific vendors. Vendors carry contact information and an optional default retention percentage used by Phase 4 payment-milestone logic.

#### Scenario: Create an Organization-scoped vendor
- **WHEN** an authenticated user POSTs to /api/vendors with `{name: "ACME Electric SRL", contact_name: "Ion Popescu", phone: "+40712345678", vat_id: "RO12345678"}`
- **THEN** the system SHALL create a Vendor with `org_id` = user's org, `project_id = null`, available across all Projects in that Organization

#### Scenario: Create a project-scoped vendor
- **WHEN** the request includes `project_id: <uuid>`
- **THEN** the system SHALL set `project_id` accordingly, so this Vendor surfaces only in that project's vendor pickers

#### Scenario: Vendor list respects project filter
- **WHEN** GET /api/vendors?project_id=P is called
- **THEN** the response SHALL include both Organization-scoped vendors AND vendors scoped to Project P; vendors scoped to other projects are excluded

#### Scenario: Soft-delete a vendor
- **WHEN** an authenticated ADMIN issues DELETE /api/vendors/{id}
- **THEN** the system SHALL set `deleted_at`; existing Offers from this Vendor remain accessible but the vendor SHALL NOT appear in pickers for new Offers

### Requirement: Offer Entity
The system SHALL provide an `Offer` entity representing a structured bid from a Vendor for a Project, with a defined status workflow, total amounts (excl and incl VAT) computed from OfferLines and cached on the parent, and zero-or-more polymorphic Attachments.

#### Scenario: Create an empty offer
- **WHEN** an authenticated user POSTs `{project_id, vendor_id, label, received_at, currency_code: "RON"}`
- **THEN** the system SHALL create an Offer with `status=DRAFT`, totals=0, ready to accept OfferLines

#### Scenario: Totals derive from OfferLines
- **WHEN** OfferLines are added, modified, or deleted on an Offer
- **THEN** the Offer's `total_amount_excl_vat` and `total_amount_incl_vat` SHALL be recomputed within the same transaction and persisted on the Offer row

#### Scenario: Allowed status transitions
- **WHEN** an authenticated user PATCHes /api/offers/{id}/status with a target status
- **THEN** the system SHALL allow DRAFT→RECEIVED, RECEIVED→ACCEPTED, RECEIVED→REJECTED, RECEIVED→EXPIRED, ACCEPTED→EXPIRED, REJECTED→DRAFT; any other transition MUST be rejected with 422

#### Scenario: Attach a PDF to an Offer
- **WHEN** an authenticated user uploads a file via the Attachment API with `attachable_type=OFFER`, `attachable_id=<offerId>`, `source_channel=pdf`
- **THEN** the system SHALL link the Attachment to the Offer per the Attachment API; the Offer remains valid with or without attachments

#### Scenario: List offers for a project filtered by status
- **WHEN** GET /api/offers?project_id=P&status=RECEIVED is called
- **THEN** the system SHALL return only Offers with `project_id=P` and `status=RECEIVED`, ordered by `received_at` descending

#### Scenario: Soft-delete cascades effectively to comparison views
- **WHEN** an Offer is soft-deleted
- **THEN** comparison and listing endpoints MUST exclude it; its OfferLines and Attachments remain in the database but are not exposed

### Requirement: OfferLine Sub-Entity
The system SHALL provide an `OfferLine` entity as a child of Offer, capturing label, optional description, quantity, unit, unit price, currency, VAT rate, server-computed line totals (excl/incl VAT), an optional `wbs_item_id` (populated in Phase 2), and an optional `normalized_key` used by Phase 1 user-driven matching.

#### Scenario: Add a line to an Offer
- **WHEN** an authenticated user POSTs to /api/offers/{id}/lines with `{label: "Săpătură fundație", qty: 80, unit: "m3", unit_price: 15, currency_code: "RON", vat_rate: 19}`
- **THEN** the system SHALL create the OfferLine, compute `line_total_excl_vat = 1200.00` and `line_total_incl_vat = 1428.00`, and trigger Offer total recomputation

#### Scenario: Bulk-add lines from spreadsheet paste
- **WHEN** an authenticated user POSTs an array of line objects to /api/offers/{id}/lines/bulk
- **THEN** the system SHALL create all lines in a single transaction, recompute Offer totals exactly once at the end, and return the created OfferLine IDs in the order received

#### Scenario: Reorder lines
- **WHEN** an authenticated user PATCHes /api/lines/{lineId}/order with a new `line_order`
- **THEN** the system SHALL persist the new ordering; comparison and listing views use `line_order` ascending as the canonical sort within an Offer

#### Scenario: Edit a line
- **WHEN** an authenticated user PUTs /api/lines/{lineId} with new `qty`, `unit_price`, or `vat_rate`
- **THEN** the system SHALL recompute that line's totals and the parent Offer totals atomically

#### Scenario: Delete a line
- **WHEN** an authenticated user DELETEs /api/lines/{lineId}
- **THEN** the system SHALL remove the row and recompute Offer totals

#### Scenario: Server-side total computation is authoritative
- **WHEN** a client submits an OfferLine with both raw inputs and a pre-computed `line_total_excl_vat`
- **THEN** the system SHALL ignore the client-supplied totals and recompute from `qty`, `unit_price`, `vat_rate`

### Requirement: Clone an Offer
The system SHALL provide an offer-clone action that creates a new DRAFT Offer copying the source Offer's `currency_code` and OfferLines, with optional new `vendor_id`, `label`, and `received_at` provided in the clone request. Attachments and status are NOT copied; the new offer always starts as DRAFT with no attachments.

#### Scenario: Clone an offer keeping the same vendor
- **WHEN** an authenticated user POSTs /api/offers/{sourceId}/clone with empty body
- **THEN** the system SHALL create a new Offer with `vendor_id`, `currency_code` copied from the source, `status=DRAFT`, `label` defaulting to source label + " (copy)", `received_at=NOW()`, and full copies of all OfferLines with fresh ids, identical qty/unit/unit_price/vat_rate/label/description, and `wbs_item_id` + `normalized_key` reset to NULL

#### Scenario: Clone an offer to a different vendor
- **WHEN** the clone request includes `{vendor_id: <newVendorId>, label: "ACME Electric — competitive bid"}`
- **THEN** the new Offer SHALL use the provided vendor and label

#### Scenario: Attachments are not cloned
- **WHEN** the source Offer has 3 Attachments and is cloned
- **THEN** the new Offer SHALL have ZERO Attachments; this is intentional — Attachments are evidence specific to the source

#### Scenario: Clone respects status DRAFT for the new offer
- **WHEN** the source Offer status is ACCEPTED
- **THEN** the clone SHALL still create the new Offer with `status=DRAFT`, allowing the user to edit before re-submission
