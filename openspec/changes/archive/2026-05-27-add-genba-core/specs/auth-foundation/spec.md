## MODIFIED Requirements

### Requirement: Organization Tenant Boundary
The system SHALL provide an `Organization` entity that is the multi-tenant boundary, with locale-keyed defaults (`country_code`, `currency_code`, `vat_regime`, `permit_workflow_template_id`) so that internationalization is data-driven and new Projects inherit sensible starting defaults per the modular-templates principle. Every domain entity owned by an Organization SHALL reference `org_id`. Users MAY belong to multiple Organizations through `UserOrganization` membership rows carrying an `org_role` ∈ {OWNER, ADMIN, MEMBER, GUEST}.

#### Scenario: Authentication response includes memberships and locale
- **WHEN** a User logs in and is a member of 2 Organizations
- **THEN** the response SHALL include `organizations: [{id, name, country_code, currency_code, vat_regime, org_role}, ...]`; the active org SHALL be picked client-side and sent on subsequent requests via JWT claim or `X-Genba-Org-Id` header

#### Scenario: Cross-organization data isolation
- **WHEN** a User in Organization X requests an endpoint that returns Organization Y's data
- **THEN** the system SHALL filter by the active org from the request; data not belonging to that org MUST NOT appear in responses (404 for direct id lookups)

#### Scenario: New Organization defaults to Romanian locale
- **WHEN** an Organization is created without explicit locale fields
- **THEN** the system SHALL set `country_code = 'RO'`, `currency_code = 'RON'`, `vat_regime = 'RO_STANDARD'`, `permit_workflow_template_id = NULL`

#### Scenario: Organization API exposes locale
- **WHEN** GET /api/organizations/{id} is called
- **THEN** the response SHALL include all four locale fields

#### Scenario: permit_workflow_template_id is a default, not a constraint
- **WHEN** a User creates a new Project under an Organization with `permit_workflow_template_id = T`
- **THEN** the Project SHALL be initialized with a cloned copy of template T's structure (in Phase 3 once templates ship); the user MAY freely modify or replace the Project's per-Project permit structure without affecting the Organization's default
