# Community Issues Reporting System (SCIRS)

Smart Community Issue Report System — a full-stack platform letting citizens
report civic issues (potholes, streetlight outages, water leaks, garbage
collection, damaged parks/buildings) and letting government staff triage,
route, and resolve them, with admin oversight, dashboards, and a citizen
gamification leaderboard.

**Course:** CST-4105 J2EE Keystone Project — University of Information
Technology, Section-C, Group-II.

**Stack:** Java 21 + Spring Boot 3 + PostgreSQL (Supabase) · React 19 +
TypeScript + Tailwind + shadcn/ui.

The full design documentation — architecture, database schema, API contract,
coding standards, UI rules, build plan, and live progress — lives in
[`context-kit/`](context-kit/AGENTS.md). Read that before making changes.

---

## Repository layout

```
/backend        Spring Boot app — com.uit.scirs
/frontend       React + TypeScript app (see note below)
/context-kit    Design documentation (source of truth for design decisions)
/docs           test-cases.md, bug-log.md, diagrams, Postman collection
```

> **Note:** `/frontend` was removed from this repository. The team's active
> frontend currently lives in a separate repository
> (`Community-Issue-Report-System-Frontend`). Point it at the backend URL
> below via its own `VITE_API_BASE_URL` env var.

---

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21 (a newer JDK works too for both running and testing — see the JDK note below) |
| Maven | 3.9+ (or use the included `./mvnw` wrapper) |
| Node.js | 20+ (for the frontend repo) |
| PostgreSQL | 15+ — this project runs against a hosted [Supabase](https://supabase.com) Postgres instance |

### JDK note

Backend **unit tests** use Mockito's inline mock maker, which by default
can't instrument classes on a JDK newer than 21. `pom.xml`'s surefire
config already passes `-Dnet.bytebuddy.experimental=true`, which fixes this
on any JDK (21 or newer) — see BUG-08 in `docs/bug-log.md`. No JDK
downgrade or special `JAVA_HOME` juggling needed; `./mvnw test` just works.

---

## Backend setup

1. **Configure secrets.** Copy the example and fill in real values:
   ```bash
   cd backend
   cp .env .env.local   # or edit backend/.env directly — it is git-ignored
   ```
   `backend/.env` is auto-loaded by `spring.config.import` in
   `application.properties`. Required values:

   | Variable | Purpose |
   |----------|---------|
   | `SPRING_DATASOURCE_URL` | Postgres JDBC URL. For Supabase's pooled connection (port 6543, PgBouncer transaction mode) it **must** include `&prepareThreshold=0` — see BUG-06 in `docs/bug-log.md` |
   | `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Database credentials |
   | `JWT_SECRET` | A long random base64 string (e.g. `openssl rand -base64 64`) — the app refuses to start with an empty secret |
   | `JWT_EXPIRATION` | Token lifetime in ms (default 24h) |
   | `STORAGE_PROVIDER` | `local` (default) or `supabase` |
   | `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origin(s), default `http://localhost:5173` |
   | `SENDGRID_API_KEY` / `MAIL_ENABLED` | Optional — email notifications, off by default |

2. **Run it:**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   The API listens on `:8080` (`SERVER_PORT` to change it). On first boot,
   `DataSeeder` creates the three roles, the seeded admin account, the 6
   departments, and the default categories (idempotent — safe to restart).

3. **Run the test suite** (needs `JAVA_HOME` on a JDK 21 install per the note above):
   ```bash
   cd backend
   ./mvnw test
   ./mvnw test -Dtest=ReportWorkflowServiceTest   # a single class
   ```

### Optional: seed demo data

A second seeder (`MockDataSeeder`) populates realistic demo content — 9
citizens (mixed approved/pending/suspended), 6 department staff, and 30
reports spanning every status, each with images, status history, comments,
feedback, notifications, and score-ledger rows. It is **off by default** and
idempotent (safe to re-run). Enable it for exactly one run:

```bash
cd backend
SEED_MOCK_DATA=true ./mvnw spring-boot:run
```
Then stop the app and restart normally (`SEED_MOCK_DATA` unset/`false`) —
the flag only needs to be `true` once.

---

## Frontend setup

The frontend is developed in a separate repository. Point it at this
backend by setting its `VITE_API_BASE_URL` (defaults to
`http://localhost:8080/api` if unset) and run its own `npm install` /
`npm run dev` per that repo's instructions.

---

## Default / seeded credentials

Seeded by `DataSeeder` on every fresh database (always present):

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@scirs.gov` | `Admin@12345` |

Only present after running `MockDataSeeder` (see **Optional: seed demo
data** above):

| Role | Email(s) | Password |
|------|----------|----------|
| Citizen (approved) | `citizen1@example.com` … `citizen7@example.com` | `Citizen@12345` |
| Citizen (pending approval) | `citizen8@example.com` | `Citizen@12345` |
| Citizen (suspended) | `citizen9@example.com` | `Citizen@12345` |
| Staff | `staff.electricity@scirs.gov`, `staff.roads@scirs.gov`, `staff.water@scirs.gov`, `staff.sanitation@scirs.gov`, `staff.parks@scirs.gov`, `staff.buildings@scirs.gov` | `Staff@12345` |

> Never reuse these in a production deployment — rotate the admin password
> and disable `MockDataSeeder` (leave `SEED_MOCK_DATA` unset) before shipping.

---

## API reference

- **Interactive docs (Swagger UI)** — with the backend running: <http://localhost:8080/swagger-ui/index.html>
  (raw OpenAPI spec at `/v3/api-docs`). Generated live from the controllers
  and DTOs, so it never drifts from the shipped code. Log in via
  `POST /api/auth/login` (e.g. the `Auth` tag's `Login` request), then click
  **Authorize** and paste the returned token to try any protected endpoint
  from the browser.
- Full human-authored contract — request/response shapes, status codes, and
  the role-based access matrix: [`context-kit/api-standards.md`](context-kit/api-standards.md).
- A ready-to-import Postman collection covering every endpoint:
  [`docs/postman/SCIRS.postman_collection.json`](docs/postman/SCIRS.postman_collection.json).
  Set the collection's `baseUrl` variable (default `http://localhost:8080/api`)
  and run **Auth → Login** first — it captures the JWT into the `token`
  variable automatically for every other request.

---

## Common commands

```bash
# Backend
cd backend && ./mvnw spring-boot:run          # run API on :8080
cd backend && ./mvnw test                     # run all tests
cd backend && ./mvnw test -Dtest=ReportWorkflowServiceTest

# Frontend (in its own repository)
npm run dev                                   # dev server on :5173
npm run build
npm run lint
```

---

## Documentation index

| Doc | Purpose |
|-----|---------|
| [`context-kit/AGENTS.md`](context-kit/AGENTS.md) | Master index for all design docs |
| [`context-kit/architecture.md`](context-kit/architecture.md) | Layers, package structure, data flow |
| [`context-kit/database-schema.md`](context-kit/database-schema.md) | Entities, columns, enums, relationships |
| [`context-kit/api-standards.md`](context-kit/api-standards.md) | REST contract, status codes, RBAC matrix |
| [`context-kit/build-plan.md`](context-kit/build-plan.md) | Phased roadmap |
| [`context-kit/progress-tracker.md`](context-kit/progress-tracker.md) | Live progress, decisions log, known issues |
| [`docs/test-cases.md`](docs/test-cases.md) | Manual test evidence |
| [`docs/bug-log.md`](docs/bug-log.md) | Bugs found and fixed, with root cause |
