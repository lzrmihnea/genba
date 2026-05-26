# Genba

Construction-management oversight for homeowners — designed for the Romanian market first, architected to scale internationally.

The name comes from 現場 (Japanese for "the actual place where work happens"); pairs with the kaisha (会社, "company") sibling in the pxro / Pantopix portfolio.

> **Status:** scaffold + OpenSpec proposals only. No working features yet.
> Phase 1 lands in four change proposals: `add-auth-foundation` (this branch), `add-genba-core`, `add-vendors-and-offers`, `add-bid-comparison-mvp`. Phase 2 = `add-wbs-catalog`. See `openspec/changes/` and `openspec/project.md`.

## Repository layout

```
genba/
├── genba-backend/                  Spring Boot 3.1.5 / Java 17 / PostgreSQL / Liquibase / JWT
│   └── src/main/{java,resources}/
├── genba-frontend/                 Next.js 15 / React 19 / TypeScript / Tailwind 4 / Ant Design 5 / next-intl
│   └── src/app/
├── docker-compose.yml              PostgreSQL only (backend + frontend run natively for iteration speed)
├── start.sh                        Brings up postgres + prints commands to run backend & frontend
├── openspec/                       OpenSpec workflow — see project.md and changes/
└── .gitignore
```

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+ and npm
- Docker Desktop (running)

## Ports

| Service          | Port |
|------------------|-----:|
| Frontend (Next)  | 3001 |
| Backend (Spring) | 8086 |
| PostgreSQL       | 5435 |

These deliberately sit next to the kaisha ports (3000 / 8085 / 5434) so you can run both portfolios side by side. There is no Adminer service — DBeaver or `psql` are recommended.

## Quick start

```bash
./start.sh
# In one terminal:
cd genba-backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
# In another:
cd genba-frontend && npm install && npm run dev
```

Open http://localhost:3001 (frontend) and http://localhost:8086/swagger-ui/index.html (API docs).

## Environment variables (dev defaults)

Backend reads from `application.yml` + `application-dev.yml`; override anything via env if you want non-default values:

| Variable                          | Default                                                | Purpose                                                  |
|-----------------------------------|--------------------------------------------------------|----------------------------------------------------------|
| `DB_HOST`                         | `localhost`                                            | PostgreSQL host                                          |
| `DB_PORT`                         | `5435`                                                 | PostgreSQL port                                          |
| `DB_NAME`                         | `genba_db`                                             |                                                          |
| `DB_USERNAME` / `DB_PASSWORD`     | `genba_user` / `genba_password`                        |                                                          |
| `JWT_SECRET`                      | `local-dev-genba-secret-key-must-be-at-least-32-chars` | Replace before any non-local use                         |
| `GENBA_SEED_EMAIL`                | (empty — seed bean skips)                              | First-boot admin user email (set when you want to login) |
| `GENBA_SEED_PASSWORD`             | (empty)                                                | First-boot admin user password                           |
| `GENBA_UPLOADS_PATH`              | `./uploads`                                            | Local directory for polymorphic Attachment files         |

## Workflow

- Integration branch: `develop`
- Feature branches: `feature/<openspec-change-id>` from `develop`, rebased before push
- Commit messages: max 9 lines, no AI/Claude/Co-Authored-By
- OpenSpec proposals are reviewed and approved before implementation. See `openspec/AGENTS.md`.

## Tech stack (patterns referenced from pxro-kaisha-03 — independent codebase)

- **Backend:** Spring Boot 3.1.5, Java 17, PostgreSQL 15, Liquibase, Spring Security 6, JWT (JJWT 0.12.3), JPA/Hibernate, Lombok, MapStruct, SpringDoc OpenAPI, TestContainers
- **Frontend:** Next.js 15, React 19, TypeScript, Tailwind CSS 4, Ant Design 5, TanStack Query, axios, next-intl, zod
- **Infra (L0):** Docker Compose, local files for attachments. No production deployment infrastructure in Layer 0.

## License

TBD.
