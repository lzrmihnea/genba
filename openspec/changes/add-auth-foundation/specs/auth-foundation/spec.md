## ADDED Requirements

### Requirement: User Identity
The system SHALL provide a `User` entity with email (unique), bcrypt password hash, display name, preferred locale (`en` or `ro`), active flag, and soft-delete capability.

#### Scenario: Login with valid credentials
- **WHEN** an inactive client POSTs /api/auth/login with valid `{email, password}`
- **THEN** the system SHALL return 200 with `{accessToken, refreshToken, user, organizations}` and the user record EXCLUDING the password hash

#### Scenario: Login with invalid credentials
- **WHEN** /api/auth/login is called with an unknown email or wrong password
- **THEN** the system SHALL return 401 with a generic message (no distinction between "unknown user" and "wrong password") and i18n key `auth.error.invalidCredentials`

#### Scenario: Inactive user blocked
- **WHEN** an inactive user attempts login
- **THEN** the system SHALL return 401 with i18n key `auth.error.userInactive`

### Requirement: JWT-Based Stateless Auth
The system SHALL issue JWT access tokens and refresh tokens on successful login. Access tokens MUST be short-lived (default 30 minutes), refresh tokens longer-lived (default 30 days); both expiries SHALL be configurable in `application.yml`.

#### Scenario: Refresh access token
- **WHEN** the client POSTs /api/auth/refresh with a valid refresh token
- **THEN** the system SHALL return a new `{accessToken}` (and optionally rotate the refresh token); expired or revoked refresh tokens MUST return 401

#### Scenario: Access protected endpoint with valid token
- **WHEN** the client includes `Authorization: Bearer <validToken>` on a protected endpoint
- **THEN** the system SHALL authenticate the request and resolve the user

#### Scenario: Access protected endpoint with expired token
- **WHEN** the access token has expired
- **THEN** the system SHALL return 401 with i18n key `auth.error.tokenExpired`, prompting the client to refresh

### Requirement: Organization Tenant Boundary
The system SHALL provide an `Organization` entity that is the multi-tenant boundary. Every domain entity owned by an Organization SHALL reference `org_id`. Users MAY belong to multiple Organizations through `UserOrganization` membership rows carrying an `org_role` ∈ {OWNER, ADMIN, MEMBER, GUEST}.

#### Scenario: Authentication response includes memberships
- **WHEN** a User logs in and is a member of 2 Organizations
- **THEN** the response SHALL include `organizations: [{id, name, org_role}, ...]`; the active org SHALL be picked client-side and sent on subsequent requests via JWT claim or `X-Genba-Org-Id` header

#### Scenario: Cross-organization data isolation
- **WHEN** a User in Organization X requests an endpoint that returns Organization Y's data
- **THEN** the system SHALL filter by the active org from the request; data not belonging to that org MUST NOT appear in responses (404 for direct id lookups)

### Requirement: i18n Framework
The system SHALL support two languages (English and Romanian) end-to-end: frontend UI strings via `next-intl` with `messages/en.json` and `messages/ro.json` bundles; backend response messages via Spring `MessageSource` with `messages_en.properties` and `messages_ro.properties`. The selected language SHALL persist per User in `preferred_locale` and be detectable via `Accept-Language` header for anonymous/login flows.

#### Scenario: User updates preferred locale
- **WHEN** an authenticated User updates their profile with `preferred_locale: "ro"`
- **THEN** subsequent UI renders SHALL load Romanian bundles; backend responses to that user MUST default to Romanian when no `Accept-Language` is sent

#### Scenario: Anonymous request honors Accept-Language
- **WHEN** an unauthenticated request reaches /api/auth/login with `Accept-Language: ro-RO`
- **THEN** the failure response SHALL use the Romanian message bundle

#### Scenario: Unknown locale falls back to English
- **WHEN** `Accept-Language: fr-FR` is sent
- **THEN** responses SHALL default to English (the system's only fallback locale)

### Requirement: Seed Admin User
On first boot in development, the system SHALL seed one Organization (`Mihnea's Builds`) and one OWNER User from environment variables `GENBA_SEED_EMAIL` and `GENBA_SEED_PASSWORD`; the seed Liquibase changeset MUST be idempotent and MUST NOT run in production environments.

#### Scenario: First boot seeds successfully
- **WHEN** the backend starts against an empty database with both env vars set
- **THEN** the system SHALL create the Organization, User, and UserOrganization rows once

#### Scenario: Second boot does not duplicate seed
- **WHEN** the backend restarts against a database that already has the seed rows
- **THEN** the seed changeset SHALL detect existing data and not duplicate
