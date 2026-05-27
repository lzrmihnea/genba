## 1. Project scaffolding
- [ ] 1.1 Create root `pom.xml` (packaging pom) and `genba-backend/pom.xml` (packaging jar, parent points to root). Dependencies per kaisha-03 reference: spring-boot-starter-{web,data-jpa,security,validation,actuator}, springdoc-openapi-starter-webmvc-ui, postgresql, liquibase-core, lombok, mapstruct, jjwt 0.12.3, spring-boot-starter-test, testcontainers, mockito.
- [ ] 1.2 `genba-backend/src/main/resources/application.yml` (common config: datasource, jpa, liquibase change-log path, server.port=8086, openapi config, jwt config block).
- [ ] 1.3 `genba-backend/src/main/resources/application-dev.yml` (local dev overrides: log levels, dev DB credentials).
- [ ] 1.4 Initialize Next.js 15 in `genba-frontend/`: TypeScript, App Router, Tailwind CSS, Ant Design 5; configure port 3002; install @tanstack/react-query, axios, next-intl, zod, react-hook-form.
- [ ] 1.5 Root `docker-compose.yml`: services postgres (15, port 5435, volume `genba-pgdata`), backend (port 8086, depends_on postgres), frontend (port 3002). No Adminer. Single dev profile.
- [ ] 1.6 Root `start.sh` (mirrors kaisha-03's start.sh pattern at the new ports).
- [ ] 1.7 Root `.gitignore` covering target/, node_modules/, .next/, .idea/, .env, .env.local, .env.dev, *.log, uploads/, .DS_Store.
- [ ] 1.8 Root `README.md` with: purpose, ports (3002/8086/5435), `./start.sh` to launch, openspec workflow note, link to https://github.com/lzrmihnea/genba.
- [ ] 1.9 Confirm `mvn clean compile` (backend) and `npm install && npm run build` (frontend) succeed before any further task.

## 2. i18n framework
- [ ] 2.1 Frontend: install + configure next-intl per Next.js 15 App Router pattern (server-side message loading per request). `messages/en.json` and `messages/ro.json` seeded with stub keys (`app.title`, `common.save`, `common.cancel`, `auth.login.*`).
- [ ] 2.2 Frontend: language switcher in user-menu (header). User locale stored in `User.preferred_locale` (added to entity in task 3.1) and on a `genba-locale` cookie for server-side detection.
- [ ] 2.3 Backend: Spring `MessageSource` bean wired to `classpath:messages` with UTF-8; `LocaleResolver` reading `Accept-Language` header; `messages_en.properties`, `messages_ro.properties` seeded with stub keys.
- [ ] 2.4 Backend exception handler resolves error messages via MessageSource; API responses include `{messageKey, message, fieldErrors}` so the client can choose to display server-resolved or key-resolved text.
- [ ] 2.5 Integration test: GET an endpoint with `Accept-Language: ro` returns Romanian; with EN returns English; with unknown returns EN fallback.

## 3. User + Role + Permission entities
- [ ] 3.1 Liquibase changeset `db.changelog-genba-auth-001-user.xml`: `app_user` table (`id UUID PK`, `email VARCHAR(255) UNIQUE NOT NULL`, `password_hash VARCHAR(255) NOT NULL`, `display_name VARCHAR(255)`, `preferred_locale VARCHAR(8) NOT NULL DEFAULT 'en'`, `active BOOLEAN NOT NULL DEFAULT TRUE`, `created_at`, `updated_at`, `deleted_at NULL`).
- [ ] 3.2 Liquibase changeset `db.changelog-genba-auth-002-role.xml`: `role` table (`id UUID PK`, `code VARCHAR(64) UNIQUE NOT NULL`, `name_en`, `name_ro`, `system_managed BOOLEAN NOT NULL DEFAULT TRUE`); `permission` table; `role_permission` join.
- [ ] 3.3 Entities + repositories + DTOs under `eu.px.genba.user.*`, `eu.px.genba.role.*`, `eu.px.genba.permission.*`.
- [ ] 3.4 Seed standard roles (SUPER_ADMIN, OWNER, ADMIN, MEMBER, GUEST) and permissions via Liquibase data changeset.

## 4. Organization + UserOrganization (multi-tenant + RBAC)
- [ ] 4.1 Liquibase changeset `db.changelog-genba-auth-003-org.xml`: `organization` table (`id UUID PK`, `name VARCHAR(255) NOT NULL`, `created_at`, `updated_at`, `deleted_at NULL`). Locale fields (country_code, currency_code, vat_regime, permit_workflow_template_id) added by `add-genba-core`.
- [ ] 4.2 Liquibase changeset `db.changelog-genba-auth-004-user-org.xml`: `user_organization` (`id UUID PK`, `user_id FK NOT NULL`, `org_id FK NOT NULL`, `org_role VARCHAR(32) NOT NULL` ∈ {OWNER, ADMIN, MEMBER, GUEST}, `created_at`, UNIQUE (`user_id`, `org_id`)).
- [ ] 4.3 Entities + repositories + DTOs under `eu.px.genba.organization.*`.
- [ ] 4.4 Authentication response includes the user's UserOrganization memberships so the frontend can render an Organization switcher.

## 5. Spring Security 6 + JWT
- [ ] 5.1 `JwtTokenProvider` issuing access + refresh tokens (JJWT 0.12.3), configurable expiry via application.yml.
- [ ] 5.2 `JwtAuthFilter` extending OncePerRequestFilter, validating Authorization header.
- [ ] 5.3 `SecurityConfig` (Spring Security 6 lambda DSL): stateless, JWT filter inserted before UsernamePasswordAuthenticationFilter, exception entry point returning 401 with JSON body, CORS for http://localhost:3002.
- [ ] 5.4 `BCryptPasswordEncoder` bean.
- [ ] 5.5 Method security with @EnableMethodSecurity + @PreAuthorize on controllers.
- [ ] 5.6 `OrganizationScopeAspect` (or interceptor) ensuring every multi-tenant query is org-scoped — derive current org from JWT claim or active-org header.

## 6. Auth REST endpoints
- [ ] 6.1 `POST /api/auth/login` accepting `{email, password}`, returning `{accessToken, refreshToken, user, organizations}`.
- [ ] 6.2 `POST /api/auth/logout` (best-effort token blacklist via Redis later — for L0, client-side discard suffices).
- [ ] 6.3 `GET /api/auth/me` returns current user + active org membership.
- [ ] 6.4 `POST /api/auth/refresh` returns new access token from valid refresh token.
- [ ] 6.5 Integration tests: login success/failure, refresh, me, logout. TestContainers-based.

## 7. Frontend auth scaffolding
- [ ] 7.1 `lib/api/client.ts` axios instance with JWT interceptor + 401-refresh handler.
- [ ] 7.2 `lib/auth/AuthProvider.tsx` (React context) exposing `user`, `currentOrg`, `login`, `logout`, `setCurrentOrg`.
- [ ] 7.3 `<AuthGuard>` redirecting unauthenticated users to `/login`.
- [ ] 7.4 `/login` page (Ant Design Form) with email/password + i18n strings.
- [ ] 7.5 Header component with user-menu (avatar, current org, language switcher, logout).
- [ ] 7.6 App shell layout (`app/(app)/layout.tsx`) wrapping protected routes with AuthGuard.

## 8. Seed admin user
- [ ] 8.1 Liquibase changeset `db.changelog-genba-auth-005-seed.xml`: insert one Organization (`Mihnea's Builds`), one User (email from env var `GENBA_SEED_EMAIL`, password hash computed from env var `GENBA_SEED_PASSWORD` at first boot), link via UserOrganization with org_role=OWNER.
- [ ] 8.2 `application.yml` documents required env vars; `.env.dev` shipped (gitignored) with safe defaults.
- [ ] 8.3 Smoke test: bring up docker-compose, log in via /login at localhost:3002, see app shell with empty Projects list.

## 9. Verification + spec migration
- [ ] 9.1 Full TestContainers integration test: login → me → refresh → logout flow.
- [ ] 9.2 Manual smoke test on local Mac at ports 3002/8086/5435.
- [ ] 9.3 Move spec deltas from `openspec/changes/add-auth-foundation/specs/auth-foundation/spec.md` to `openspec/specs/auth-foundation/spec.md` after merge to develop.
- [ ] 9.4 `openspec validate add-auth-foundation --strict` passes.
- [ ] 9.5 Archive change after merge.
