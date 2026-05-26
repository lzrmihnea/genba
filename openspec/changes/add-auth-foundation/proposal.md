## Why
Genba is being built as an independent codebase (not a git fork of kaisha) but mirrors kaisha's tech stack and patterns. This change creates the foundational scaffolding: Maven multi-module project, Next.js frontend, docker-compose, Liquibase setup, i18n framework (EN+RO), and the auth/multi-org/RBAC primitives that every downstream feature depends on. Reference implementation: `pxro-kaisha-03` — patterns to mirror, no `cp -R`, no Java package renaming.

## What Changes
- **ADD**: Maven root `pom.xml` + `genba-backend/` module (Spring Boot 3.1.5, Java 17, packaging jar). Dependencies match kaisha-03's pattern: spring-boot-starter-{web,data-jpa,security,validation,actuator}, springdoc-openapi-starter-webmvc-ui, postgresql, liquibase-core, lombok, mapstruct, jjwt 0.12.3, spring-boot-starter-test, testcontainers.
- **ADD**: `genba-frontend/` Next.js 15 + React 19 + TypeScript project. Dependencies: ant-design 5, tailwind 4, @tanstack/react-query, axios, next-intl, zod, react-hook-form.
- **ADD**: root `docker-compose.yml` with services: `postgres` (port 5435, image postgres:15), `backend` (port 8086, depends_on postgres), `frontend` (port 3001). **No Adminer.** Single profile (no staging/prod splits in L0). Persistent named volume `genba-pgdata`.
- **ADD**: `application.yml` and `application-dev.yml` for backend; `next.config.ts` and `.env.local.example` for frontend. Pattern referenced from kaisha-03.
- **ADD**: Liquibase master changelog `db.changelog-master.xml` referencing modular changesets per change-proposal.
- **ADD**: i18n framework setup — `next-intl` configuration on frontend with EN+RO bundle stubs (`messages/en.json`, `messages/ro.json`); Spring `MessageSource` on backend with `messages_en.properties` + `messages_ro.properties`; `Accept-Language` header parsing.
- **ADD**: Entities `User`, `Role`, `Permission`, `Organization`, `UserOrganization` (membership join with `org_role` enum: OWNER, ADMIN, MEMBER, GUEST). Liquibase changesets `db.changelog-genba-auth-001` through `-004`.
- **ADD**: Spring Security 6 configuration — stateless JWT auth, BCrypt password encoding, `JwtAuthFilter`, role-based method security via `@PreAuthorize`. Pattern referenced from kaisha-03's `pxro.kaisha.security.*` (reimplemented in `pxro.genba.security.*`).
- **ADD**: Auth REST endpoints — `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`, `POST /api/auth/refresh`. No registration endpoint in L0 (user is seeded).
- **ADD**: Frontend auth scaffolding — `AuthProvider`, `useAuth` hook, `<AuthGuard>` route protection, axios interceptor injecting JWT + handling 401-refresh, login page at `/login`.
- **ADD**: Seed Liquibase changeset `db.changelog-genba-auth-005-seed.xml` populating one Organization (`Mihnea's Builds`, locale RO/RON/RO_STANDARD) and one User (Mihnea, OWNER) with password set from env var at first boot.
- **ADD**: `README.md` and `.gitignore` (covering target/, node_modules/, .next/, .idea/, .env, .env.local, *.log, uploads/).
- **NO** registration UI, password reset, email verification, OAuth, MFA in this change. All deferred to Layer 1.
- **NO** kaisha code copied wholesale. Patterns referenced; implementations fresh.

## Impact
- **Affected specs**: NEW capability `auth-foundation`.
- **Affected code**:
  - New backend modules: `pxro.genba.{user, organization, role, permission, security, auth, i18n, config, common}`.
  - New frontend: `app/{login,layout}/`, `lib/{api, auth, i18n}/`, `components/{AuthProvider, AuthGuard}`.
  - Root infra: `pom.xml`, `docker-compose.yml`, `start.sh`, `.gitignore`, `README.md`.
- **Risk**: Reimplementing auth instead of forking adds engineering time vs. inheriting kaisha's mature pieces. Mitigation: cherry-pick specific kaisha-03 files as patterns (open them side-by-side, reimplement equivalent in genba's structure); avoid line-by-line copy that would confuse the "independent codebase" promise.
- **Risk**: i18n setup from day 1 vs. retrofitted later — small upfront cost, large later avoidance. Worth it.
- **Risk**: Single seeded user means no realistic multi-user test in L0. Acceptable; first real multi-user test happens at L1.
