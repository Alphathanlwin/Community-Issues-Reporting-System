# 2. SCIRS — Architecture

## Technology Stack

### Backend

| Technology | Version / Detail | Role |
|------------|------------------|------|
| Java | 21 (LTS) | Primary backend language |
| Spring Boot | 3.x | Application framework |
| Spring Web (MVC) | — | REST API layer |
| Spring Data JPA | — | Repository abstraction |
| Hibernate ORM | — | Object-relational mapping |
| Spring Security | — | Authentication and authorization |
| JWT (jjwt) | — | Stateless token authentication |
| PostgreSQL | 15+ (Supabase-hosted or local) | Relational database |
| Spring Mail | — | Email notifications (SendGrid SMTP) |
| Embedded Tomcat | 10 (via Spring Boot) | Web server |

### Frontend

| Technology | Version / Detail | Role |
|------------|------------------|------|
| React | 19 | UI framework |
| TypeScript | — | Type-safe JavaScript |
| Tailwind CSS | — | Utility-first styling |
| shadcn/ui | — | Component primitives |
| Leaflet.js (or Mapbox GL JS) | — | Interactive map, pins, clustering |
| Recharts | — | Dashboard graphs and pie charts |
| React Router | — | Routing and role-guarded routes |

### Infrastructure & Tools

| Tool | Purpose |
|------|---------|
| Supabase Storage | Report and resolution image storage |
| IntelliJ IDEA / Eclipse / VS Code | Development |
| Git + GitHub | Version control |
| Postman | API testing (exported collection is a deliverable) |
| pgAdmin | Direct database inspection |

## System Architecture — Layered

```
┌─────────────────────────────────────────────┐
│  React 19 + TypeScript                      │  ← Frontend (Browser)
│  Citizen (mobile-first) | Staff/Admin (PC)  │
└──────────────────┬──────────────────────────┘
                   │ HTTP / JSON  (JWT Bearer)
                   ▼
┌─────────────────────────────────────────────┐
│  REST API Layer  (@RestController)          │  ← Spring Web
│  Validation, status codes, no logic         │
└──────────────────┬──────────────────────────┘
                   │ Method Calls
                   ▼
┌─────────────────────────────────────────────┐
│  Service Layer  (@Service)                  │  ← Business rules
│  Workflow, routing, scoring, notifications  │
└──────┬───────────────────────┬──────────────┘
       │ Method Calls          │
       ▼                       ▼
┌───────────────────┐   ┌──────────────────────┐
│ Repository Layer  │   │ Integration Layer    │
│ (Spring Data JPA) │   │ Storage / Mail       │
└──────┬────────────┘   └──────────────────────┘
       │ Hibernate / JDBC
       ▼
┌─────────────────────────────────────────────┐
│  PostgreSQL                                 │
└─────────────────────────────────────────────┘
```

## Layer Responsibilities

### Controller Layer

| Rule | Details |
|------|---------|
| Receives HTTP requests | `@GetMapping`, `@PostMapping`, etc. |
| Performs request validation | Via `@Valid` and Jakarta validation annotations |
| Reads the authenticated principal | Via `@AuthenticationPrincipal` — never trust a user ID from the request body |
| Calls service methods | Delegates all logic to the Service layer |
| Returns HTTP responses | `ResponseEntity<>` with explicit status codes |
| MUST NOT contain business logic | Zero business rules in controllers |
| MUST NOT call repositories directly | Always go through the Service layer |
| MUST NOT catch exceptions | Let them propagate to the global handler |

### Service Layer

| Rule | Details |
|------|---------|
| Contains all business logic | Workflow transitions, routing, scoring, validation |
| Coordinates repositories | May call multiple repositories |
| Handles transactions | `@Transactional` on all write operations |
| Enforces ownership rules | A citizen may only touch their own data |
| Throws business exceptions | Custom exceptions, never generic ones |
| Accepts and returns DTOs | Never entities across the layer boundary |
| MUST NOT access HTTP objects | No `HttpServletRequest` in services |

### Repository Layer

| Rule | Details |
|------|---------|
| Database access only | Extends `JpaRepository<Entity, Long>` |
| Custom queries | Spring Data derived method names, or `@Query` when derivation is insufficient |
| MUST NOT contain business logic | Pure data access |
| MUST NOT throw business exceptions | Data-level exceptions only |

### Integration Layer (`common/integration`)

| Component | Responsibility |
|-----------|----------------|
| `FileStorageService` | Interface for image upload/delete. Implementations: `SupabaseStorageService`, `LocalStorageService` (dev/demo fallback) |
| `EmailService` | Sends notification emails; called asynchronously so a mail failure never blocks a status change |

Services depend on the **interface**, never on a concrete implementation.

## Backend Package Structure

```
com.uit.scirs
├── auth/
│   ├── controller/    ← AuthController (login, citizen register)
│   ├── service/
│   ├── dto/
│   └── mapper/
│
├── user/
│   ├── controller/    ← User, citizen-approval, staff-creation endpoints
│   ├── service/
│   ├── repository/
│   ├── entity/        ← User, Role
│   ├── dto/
│   └── mapper/
│
├── department/
│   ├── controller/  service/  repository/  entity/  dto/  mapper/
│
├── category/
│   ├── controller/  service/  repository/  entity/  dto/  mapper/
│
├── report/
│   ├── controller/    ← ReportController, ReportMapController
│   ├── service/       ← ReportService, ReportWorkflowService, ReportAssignmentService
│   ├── repository/    ← ReportRepository, ReportImageRepository, ReportStatusHistoryRepository, ReportCommentRepository
│   ├── entity/        ← Report, ReportImage, ReportStatusHistory, ReportComment
│   ├── dto/
│   └── mapper/
│
├── feedback/
│   ├── controller/  service/  repository/  entity/  dto/  mapper/
│
├── notification/
│   ├── controller/  service/  repository/  entity/  dto/  mapper/
│
├── score/
│   ├── controller/    ← LeaderboardController, ScoreController
│   ├── service/       ← ScoreService (awards points, builds leaderboard)
│   ├── repository/    ← PointTransactionRepository
│   ├── entity/        ← PointTransaction
│   ├── dto/
│   └── mapper/
│
├── dashboard/
│   ├── controller/    ← AdminDashboardController, StaffDashboardController
│   ├── service/       ← DashboardService (aggregate queries only)
│   └── dto/
│
├── common/
│   ├── config/        ← App-wide beans, CORS, async, seed data runner
│   ├── security/      ← JwtUtil, JwtAuthenticationFilter, SecurityConfig, CurrentUser helper
│   ├── exception/     ← GlobalExceptionHandler, custom exceptions, ErrorResponse
│   ├── integration/   ← FileStorageService, EmailService
│   └── util/          ← Shared helpers (report code generator, geo helpers)
│
└── ScirsApplication.java
```

### Package Rules

1. Each feature module is self-contained: controller, service, repository, entity, dto, mapper.
2. Cross-module communication goes through the **Service layer only** — never import another module's repository.
3. `common` holds shared infrastructure only — no feature logic.
4. No circular dependencies between feature modules. If two services need each other, extract the shared rule into a third service or publish an application event.
5. `dashboard` is read-only: it may query repositories through other modules' services, and must never mutate state.

## Data Flow — Typical Request

```
Client Request (JSON + JWT)
        │
        ▼
JwtAuthenticationFilter — validates token, sets SecurityContext
        │
        ▼
Controller — @Valid on DTO, reads principal
        │
        ▼
Service — ownership check → business rules → repository calls → mapper
        │
        ▼
Repository — JPA query
        │
        ▼
PostgreSQL
        │
        ▼
Entity → Service → Mapper → DTO
        │
        ▼
Controller returns ResponseEntity<DTO>
        │
        ▼
Client receives JSON
```

## Key Workflow — Report Lifecycle

```
Citizen submits report
   │  ReportService.create()
   │  ├── verify citizen AccountStatus = APPROVED
   │  ├── validate image (type, size) → FileStorageService
   │  ├── persist Report with status PENDING_APPROVAL
   │  ├── write ReportStatusHistory (null → PENDING_APPROVAL)
   │  └── notify admins ("new report awaiting approval")
   ▼
Admin approves / denies
   │  ReportWorkflowService.approve()
   │  ├── status → ASSIGNED
   │  ├── department = category.defaultDepartment  (auto-routing)
   │  ├── ScoreService.award(citizen, REPORT_APPROVED, +10)
   │  ├── write status history
   │  └── notify citizen + notify department staff
   │
   │  ReportWorkflowService.reject()
   │  ├── status → REJECTED (terminal), rejection reason required
   │  ├── ScoreService.award(citizen, REPORT_REJECTED, −5)
   │  └── notify citizen
   ▼
Staff works the report
   │  ├── status → IN_PROGRESS (remarks optional)
   │  ├── may add ReportComment for the department
   │  ├── uploads completion photo(s) — required before RESOLVED
   │  ├── status → RESOLVED, resolvedAt = now
   │  ├── ScoreService.award(citizen, REPORT_RESOLVED, +20)
   │  └── notify citizen
   ▼
Citizen leaves feedback (rating 1–5 + comment)
   │  └── ScoreService.award(citizen, FEEDBACK_GIVEN, +5)
   ▼
Admin/Staff closes the report → CLOSED (terminal)
```

Reassignment: an admin (or staff with permission) may change `report.department` at any non-terminal status. This writes a status-history row with the same status and a remark describing the reassignment.

## Security Architecture

```
Login request (email + password)
        ▼
AuthController → AuthService
        ├── load user by email
        ├── verify BCrypt password
        ├── reject if AccountStatus != APPROVED  → 403 with a clear message
        └── generate JWT: sub=email, uid, role, deptId, iat, exp
        ▼
Client stores JWT
        ▼
Authorization: Bearer <token> on every protected request
        ▼
JwtAuthenticationFilter
        ├── extract + validate token
        └── set SecurityContext with authorities ROLE_ADMIN / ROLE_STAFF / ROLE_CITIZEN
        ▼
URL-level rules (SecurityConfig) + method-level @PreAuthorize
        ▼
Service-level ownership check (citizen owns the record / staff owns the department)
```

### Security Rules

- All endpoints are protected by default; only `/api/auth/login`, `/api/auth/register`, and static assets are public.
- Passwords are always BCrypt-hashed. Never log or return a password hash.
- JWT claims: user id, email, role, department id (staff only). Expiry: 24 hours.
- Role checks alone are **not sufficient** — every service that reads or writes a user-owned record must also verify ownership.
- Uploaded files: whitelist `image/jpeg`, `image/png`, `image/webp`; max 5 MB; regenerate the filename server-side (never trust the client filename).
- GPS coordinates are validated server-side (`-90 ≤ lat ≤ 90`, `-180 ≤ lng ≤ 180`).
- CORS is configured explicitly for the frontend origin only.
