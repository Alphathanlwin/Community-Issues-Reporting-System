# Smart Community Issue Report System (SCIRS)

A REST API backend for civic issue reporting and resolution, built for government staff to triage citizen-submitted reports (potholes, water leaks, streetlight outages) across departments with role-based access, workflow automation, and admin dashboards.

**Course:** CST-4105 J2EE Keystone Project — University of Information Technology, Yangon.

## Overview

Citizens in Yangon currently report civic problems via phone calls or in-person visits, with no tracking or accountability. SCIRS provides a web-based system where citizens submit geotagged reports with photos, staff triage and resolve issues within their department, and administrators oversee the entire pipeline through dashboards and audit trails.

This repository contains the **Spring Boot backend**. The frontend lives in a separate repository (`Community-Issue-Report-System-Frontend`).

## Key Features

- **Report submission with duplicate detection** — citizens submit geotagged reports; the system checks for existing open reports within 100m using Haversine distance before persisting
- **7-state workflow engine** — `PENDING_APPROVAL → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED` with rejection and re-open paths, enforced via a state machine map
- **Role-based access control** — three roles (ADMIN, STAFF, CITIZEN) with method-level `@PreAuthorize` guards and department-scoped data visibility
- **Audit trail** — every state-changing operation (approve, reject, status change, account review) writes an immutable `audit_logs` row, best-effort in a separate transaction
- **Gamification leaderboard** — citizens earn points for approved reports (+10), resolutions (+20), feedback (+5), and confirmations (+3); idempotent scoring prevents double-awards
- **Admin dashboard** — pending accounts, pending reports, total counts, department performance with resolution times and average ratings, 12-month time series
- **Caffeine caching** — 5 named caches (categories, departments, leaderboard, public map, dept stats) with role-aware safety rules to prevent cross-user data leaks
- **File storage** — pluggable `FileStorageService` interface with local and Supabase implementations, image validation, and type-tagged uploads (report vs. resolution photos)
- **Email notifications** — async SendGrid integration for status changes, new reports, and account decisions (disabled by default)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21, Spring Boot 3.3.4 |
| Security | Spring Security, JWT (jjwt 0.12.6), BCrypt |
| Database | PostgreSQL 15+ (hosted on Supabase), Spring Data JPA |
| Caching | Caffeine (in-memory, Spring Cache abstraction) |
| Testing | JUnit 5, Mockito, Spring Security Test, H2 (test DB) |
| API Docs | Springdoc OpenAPI 2.6.0 (Swagger UI) |
| Email | Spring Mail + SendGrid SMTP |
| Build | Maven 3.9+ |
| Deployment | Docker multi-stage build → Render (free tier) |
| Design Docs | `context-kit/` — architecture, schema, API contract, coding standards |

## Architecture

The backend follows a **package-by-feature** layered architecture. Every request passes through four layers in order:

```
Client (Postman / Frontend)
    │
    ▼
Controller ── validates request, maps to DTOs, returns ResponseEntity
    │
    ▼
Service ── business rules, state transitions, scoring, notifications
    │
    ▼
Repository ── Spring Data JPA interfaces → PostgreSQL
```

Each feature module (`auth/`, `report/`, `user/`, `department/`, `category/`, `score/`, `notification/`, `dashboard/`, `audit/`, `feedback/`) contains its own controller, service, repository, entity, DTO, and mapper — keeping code for each concern co-located.

Shared infrastructure (`common/`) provides security (JWT filter, `SecurityConfig`), configuration (CORS, async, caching, OpenAPI), file storage, email, and the global exception handler.

### Report Workflow State Machine

```
                    ┌──────────────┐
                    │   PENDING_   │──── reject ────┐
                    │   APPROVAL   │                 │
                    └──────┬───────┘                 ▼
                           │ approve            ┌──────────┐
                           ▼                    │ REJECTED  │
                    ┌──────────────┐            └──────────┘
                    │   ASSIGNED   │
                    └──────┬───────┘
                           │ start work
                           ▼
                    ┌──────────────┐
                    │ IN_PROGRESS  │◄─── re-open ───┐
                    └──────┬───────┘                │
                           │ complete (+photo)      │
                           ▼                        │
                    ┌──────────────┐                │
                    │   RESOLVED   │────────────────┘
                    └──────┬───────┘
                           │ close
                           ▼
                    ┌──────────────┐
                    │   CLOSED     │
                    └──────────────┘
```

Every transition writes a `StatusHistory` row, fires a `Notification`, and triggers scoring where applicable.

## How It Works

### Report Submission Flow

1. Citizen calls `POST /api/reports` with title, description, category, coordinates, and optional images
2. `ReportService.submitReport()` runs duplicate detection — queries for open reports of the same category within a 100m bounding box, then applies Haversine filtering
3. If duplicates are found, the API returns them without persisting; the citizen confirms a match or forces creation
4. On creation, the report gets status `PENDING_APPROVAL`, a generated report code (e.g. `RPT-2026-0007`), and images are stored via `FileStorageService`
5. Admins see pending reports on their dashboard; approving auto-routes to the category's department, calculates priority (severity weight + age), and awards +10 points
6. Staff update status through the workflow; resolving requires at least one completion photo and awards +20 points
7. Every transition writes audit history and notifies the reporter

### Authentication Flow

1. Citizens self-register via `POST /api/auth/register` → account status `PENDING`
2. Admin approves via `PUT /api/users/{id}/approve` → status `APPROVED`
3. Login returns a JWT containing `userId`, `email`, `role`, and `departmentId`
4. `JwtAuthenticationFilter` extracts these into a `CurrentUser` object on every request
5. `@PreAuthorize` annotations on controllers enforce role and department ownership

## Project Structure

```
backend/
├── pom.xml
├── Dockerfile
├── src/main/java/com/uit/scirs/
│   ├── ScirsApplication.java
│   ├── auth/          # login, register, JWT generation
│   ├── user/          # CRUD, approve/reject/suspend, staff creation
│   ├── report/        # submission, workflow, assignment, comments, map, public feed
│   ├── department/    # CRUD with soft delete
│   ├── category/      # CRUD, must belong to active department
│   ├── feedback/      # post-resolution citizen feedback
│   ├── notification/  # 10 notification types, email integration
│   ├── score/         # point transactions, leaderboard
│   ├── dashboard/     # admin + staff aggregates, monthly series
│   ├── audit/         # immutable audit trail
│   └── common/
│       ├── security/  # JWT, SecurityConfig, JwtAuthenticationFilter
│       ├── config/    # CORS, async, caching, data seeders, OpenAPI
│       ├── exception/ # global handler, custom exceptions
│       ├── integration/ # file storage (local + Supabase), email
│       └── util/      # report code generator
├── src/test/java/com/uit/scirs/  # 26 test files
└── src/test/resources/application-test.properties
```

## Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21 |
| Maven | 3.9+ |
| PostgreSQL | 15+ (or a Supabase instance) |

### Installation

```bash
git clone https://github.com/[your-username]/Community-Issues-Reporting-System.git
cd Community-Issues-Reporting-System/backend
mvn dependency:go-offline   # fetch all dependencies
```

### Environment Variables

Create `backend/.env` (auto-loaded by Spring, git-ignored):

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/scirs_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT (generate with: openssl rand -base64 64)
JWT_SECRET=<base64-encoded-secret>
JWT_EXPIRATION=86400000

# Storage: "local" or "supabase"
STORAGE_PROVIDER=local
# SUPABASE_URL=           # required if using supabase
# SUPABASE_SERVICE_KEY=   # required if using supabase

# CORS (comma-separated origins)
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Email (optional, off by default)
MAIL_ENABLED=false
# SENDGRID_API_KEY=

# Seeded admin
ADMIN_EMAIL=admin@scirs.gov
ADMIN_PASSWORD=Admin@12345
```

### Run Locally

```bash
cd backend
./mvnw spring-boot:run          # API on :8080
./mvnw test                     # run all 26 test files
```

Seed demo data (9 citizens, 6 staff, 30 reports) for one run:

```bash
SEED_MOCK_DATA=true ./mvnw spring-boot:run
# then restart normally — the flag only needs to be true once
```

### Docker

```bash
cd backend
docker build -t scirs .
docker run -p 8080:8080 --env-file .env scirs
```

## API

Interactive Swagger UI at `/swagger-ui/index.html` (no auth required). Raw spec at `/v3/api-docs`.

A Postman collection is available at `docs/postman/SCIRS.postman_collection.json`.

### Core Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | Public | Citizen self-registration |
| `POST` | `/api/auth/login` | Public | Returns JWT |
| `GET` | `/api/auth/me` | Any | Current user profile |
| `POST` | `/api/reports` | Citizen | Submit report with images |
| `GET` | `/api/reports` | Admin/Staff | Paginated, filterable report list |
| `GET` | `/api/reports/{id}` | Owner/Admin/Staff | Single report detail |
| `PUT` | `/api/reports/{id}/approve` | Admin | Approve + auto-route to department |
| `PUT` | `/api/reports/{id}/reject` | Admin | Reject with reason |
| `PUT` | `/api/reports/{id}/status` | Staff | Change status (state machine enforced) |
| `PUT` | `/api/reports/{id}/completion-photos` | Staff | Upload resolution photos |
| `GET` | `/api/reports/map/pins` | Any | Geotagged map data |
| `GET` | `/api/reports/public/feed` | Public | Active reports (citizen-visible) |
| `GET` | `/api/leaderboard` | Any | Top citizens by score |
| `GET` | `/api/dashboard/admin` | Admin | Pending accounts, pending reports, totals |
| `GET` | `/api/dashboard/staff` | Staff | Department-scoped stats + monthly series |
| `GET` | `/api/dashboard/departments` | Admin/Staff | Cross-department performance |
| `GET` | `/api/audit-logs` | Admin | Searchable audit trail |
| `GET` | `/api/notifications` | Any | User notifications |
| `PUT` | `/api/users/{id}/approve` | Admin | Approve citizen account |

## Testing

26 test files covering unit and integration layers:

- **Unit tests** — Mockito-based service tests for auth, reports, workflow, scoring, notifications, dashboard, audit, feedback, priority, and duplicate detection
- **Integration tests** — `@SpringBootTest` with MockMvc for controllers (auth, user, department, category, reports, dashboard), using H2 in-memory database
- **Cache integration** — `CacheConfigIntegrationTest` verifies Caffeine cache wiring

Tests run against H2 with `application-test.properties` (no external dependencies required).

```bash
cd backend
./mvnw test                              # all tests
./mvnw test -Dtest=ReportWorkflowServiceTest  # single class
```

## Engineering Highlights

1. **State machine with guard conditions** — `ReportWorkflowService` defines allowed transitions as a `Map<Status, Set<Status>>` and enforces them atomically; the resolver path requires a completion photo to exist before allowing `IN_PROGRESS → RESOLVED`

2. **Haversine duplicate detection without PostGIS** — bounding-box pre-filter in SQL, precise Haversine distance in Java; returns up to 5 candidates within 100m, presented to the citizen before report creation

3. **Idempotent scoring** — `ScoreService.award()` checks a `(user, report, reason)` unique constraint before writing; re-approving or re-resolving the same report never double-awards points

4. **Cache safety rules** — five Caffeine caches with explicit policies: never cache user-scoped data (prevents cross-user leaks), never cache single-report reads (citizens track status in near-real-time), only aggregate/reference data is cached

5. **Best-effort audit trail** — `AuditService.record()` writes in a separate `REQUIRES_NEW` transaction and swallows failures, so audit logging can never break the business operation it records

6. **Multi-stage Docker build** — Maven compilation in `maven:3.9-eclipse-temurin-21`, runtime in `eclipse-temurin:21-jre-alpine` with non-root user; dependencies cached in a separate layer for faster rebuilds

## Challenges & Technical Decisions

**Package-by-feature vs. package-by-layer**
The codebase groups code by domain feature (`report/`, `auth/`, `score/`) rather than by technical layer (`controllers/`, `services/`). This keeps related code co-located — the report workflow, its DTOs, entities, and repository all live under `report/` — at the cost of some cross-feature imports. Chosen because it scales better as features grow and makes it clear where new code belongs.

**Caffeine over Redis**
In-memory caching was chosen over Redis because the deployment is single-node on Render's free tier. Caffeine avoids the operational complexity of running a separate Redis instance. The trade-off is documented: caches are per-instance state and would need replacement if the app scales to multiple instances.

**Separate transaction for audit writes**
`AuditService.record()` runs in `REQUIRES_NEW` so a failed audit write (disk full, transient DB error) doesn't roll back the business operation. This is a deliberate reliability trade-off: the audit trail may occasionally miss a row, but business operations are never blocked by audit infrastructure.

**H2 test database**
Tests run against H2 in PostgreSQL compatibility mode rather than a real PostgreSQL instance. This keeps the test suite fast and dependency-free, but means some PostgreSQL-specific behaviors (JSON functions, array types) can't be tested in CI. The trade-off is acceptable for a project of this scope.

## Future Improvements

- **Wire duplicate count into priority scoring** — `PriorityService` already accepts a `duplicateCount` parameter but it's currently hardcoded to 0; connecting it to `DuplicateDetectionService` results would auto-escalate frequently-reported locations
- **Supabase storage integration** — the `FileStorageService` interface has a `SupabaseStorageService` but it needs the Supabase Storage API wiring completed
- **Frontend rebuild** — the React frontend was removed from this repo and needs to be rebuilt in the separate frontend repository
- **Pagination for report list and audit logs** — the repository layer supports it via `Pageable`, but some endpoints return full lists; adding cursor-based pagination would improve performance at scale
- **PostgreSQL migration scripts** — currently using `spring.jpa.hibernate.ddl-auto=update`; generating Flyway or Liquibase scripts would make schema changes safer in production

## License

This project has no license file. All rights reserved by the development team.
