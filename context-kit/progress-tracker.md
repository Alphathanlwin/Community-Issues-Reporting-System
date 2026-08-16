# 10. SCIRS — Progress Tracker

**Current Phase:** Phase 3 — User Management & Approval Queues (backend complete) / Phase 4 — Report Submission (backend: create-report slice complete)
**Last Updated:** 2026-08-16

> Agents: read this file **before** starting work and update it **after** finishing work. Mark `[x]` only when the item is actually working, not merely written.

---

## Status Legend

`[ ]` Not started `[~]` In progress `[x]` Done and verified `[!]` Blocked (add a note)

---
## Phase 0 — Analysis & Design

- [ ] Project proposal finalised and submitted
- [ ] Use case diagram
- [ ] System architecture diagram
- [ ] ER diagram matching `database-schema.md`
- [ ] Report status state-machine diagram
- [ ] UI wireframes — citizen shell
- [ ] UI wireframes — console shell
- [ ] Git repository + branch strategy
- [ ] PostgreSQL database created and reachable
- [x] Spring Boot project scaffolded with dependencies
- [ ] React + TypeScript + Tailwind + shadcn/ui scaffolded

## Phase 1 — Foundation & Authentication

### Backend
- [x] `common/exception/` — ErrorResponse, custom exceptions, GlobalExceptionHandler
- [x] `common/config/` — CORS, async, property beans
- [x] `User`, `Role` entities + `RoleName`, `AccountStatus` enums
- [x] `UserRepository`, `RoleRepository`
- [x] Auth DTOs (login, register, response, UserDTO)
- [x] `AuthService` — login
- [x] `AuthService` — citizen registration (forced role + PENDING status)
- [x] `JwtUtil` (generate, validate, extract claims)
- [x] `CurrentUser` principal
- [x] `JwtAuthenticationFilter`
- [x] `SecurityConfig` with public/protected rules
- [x] `AuthController` — login, register, me
- [x] `DataSeeder` — roles + admin account (idempotent)
- [x] Tests: login success/failure, pending block, 401 without token, 403 wrong role

### Frontend
- [ ] API client with JWT injection + 401 handling
- [ ] `AuthContext` + token storage
- [ ] Login page
- [ ] Citizen registration page
- [ ] `ProtectedRoute` + role-based shell routing
- [ ] `CitizenShell` skeleton (bottom tabs)
- [ ] `ConsoleShell` skeleton (navbar + sidebar)

## Phase 2 — Departments & Categories

### Backend
- [x] `Department` entity, repository, DTOs, mapper
- [x] `DepartmentService` + `DepartmentController` (CRUD, soft delete)
- [x] `Category` entity, repository, DTOs, mapper
- [x] `CategoryService` + `CategoryController` (CRUD, soft delete)
- [x] Seed 6 departments
- [x] Seed default categories with department mapping
- [x] Tests: category requires active department, soft delete behaviour

### Frontend
- [ ] Departments list page
- [ ] Department create/edit form
- [ ] Categories list page
- [ ] Category create/edit form (department, colour, icon)

## Phase 3 — User Management & Approval Queues

### Backend
- [x] `UserService` — list/filter users (`getAll`, `getCitizens`, `getStaff`, `getPending`, `getById` with self/admin ownership check)
- [x] `UserService` — create staff account (`createStaff`, requires an active department, forces role `STAFF` + `accountStatus APPROVED`)
- [x] `UserService` — approve / reject / suspend citizen (each requires the current status listed in `api-standards.md`; reject validates a reason but does not persist it — see Decisions)
- [x] `UserService` — soft delete (`isActive = false`)
- [x] `UserController` — all endpoints from `api-standards.md` § User Management, including `GET`/`PUT /api/users/{id}` (self or admin)
- [x] Tests: staff requires an active department (unit + integration), approval/reject/suspend flow (unit + integration), role gates (integration, 403 for non-admin/non-self)

### Frontend
- [ ] Citizen accounts table
- [ ] Staff accounts table
- [ ] Create new staff form
- [ ] Account approval queue (approve / deny)
- [ ] Citizen profile page (view + edit)

## Phase 4 — Report Submission

### Backend
- [x] `Report`, `ReportImage` entities + enums (`ReportStatus`, `ReportPriority`, `ImageType`) — full column set per `database-schema.md`, including columns Phase 5 will use (`assignedStaffId`, `rejectionReason`, `approvedAt`/`approvedBy`, `resolvedAt`, `closedAt`); no Phase 5 logic touches them yet
- [x] `ReportRepository`, `ReportImageRepository`
- [x] `FileStorageService` interface
- [x] `LocalStorageService` implementation (whitelist type, 5 MB limit, magic-byte check, UUID filename; served back via `WebConfig` → `/uploads/**`)
- [ ] `SupabaseStorageService` implementation — deferred; `app.storage.provider` defaults to `local` so the app is fully functional without it, but the Supabase path from `library-docs.md`/D4 is not built
- [x] Image validation (type, size, magic bytes, renamed file)
- [x] `ReportCodeGenerator` (`common/util`, format `RPT-{year}-{seq}`)
- [x] `ReportService.createReport()` (multipart: `data` JSON part + optional `images` parts)
- [ ] `GET /api/reports/my` — not built this session (out of scope for the "citizen can submit a report" task given to this agent; see Decisions)
- [ ] `GET /api/reports/{id}` with ownership check — same as above
- [x] Tests: unapproved citizen blocked, unknown category rejected, unknown citizen rejected, lat/long bounds, blank title, invalid image content, image storage invoked + URL persisted, 401 unauthenticated, 403 non-citizen, 201 happy path with `PENDING_APPROVAL` + correct owner

### Frontend
- [ ] Geolocation hook + permission-denied fallback
- [ ] Report submission form (location, category, chips, description, photo)
- [ ] Image preview + remove
- [ ] Citizen home page with recent reports
- [ ] Citizen report detail page

## Phase 5 — Approval, Routing & Workflow

### Backend
- [ ] `ReportStatusHistory` entity + repository
- [ ] `ReportComment` entity + repository
- [ ] `StatusHistoryService`
- [ ] `ReportWorkflowService` — transition matrix
- [ ] Approve endpoint (auto-route + history + notification stub)
- [ ] Reject endpoint (reason required)
- [ ] Status change endpoint (remarks)
- [ ] Resolution-photo requirement before RESOLVED
- [ ] `ReportAssignmentService` — reassign department / assign staff
- [ ] Priority endpoint
- [ ] History endpoint
- [ ] Comments endpoints (+ department mention)
- [ ] Department scoping for staff queries
- [ ] Tests: full transition matrix, history per change, department scoping

### Frontend
- [ ] Console reports list (search, filters, pagination)
- [ ] Report detail — Overview tab
- [ ] Report detail — Timeline tab
- [ ] Report detail — Comments tab
- [ ] Report detail — Resolution tab + photo upload
- [ ] Status change dialog
- [ ] Assign / reassign dialog
- [ ] Admin report approval queue
- [ ] Citizen status timeline

## Phase 6 — Map & Filtering

### Backend
- [ ] `ReportMapDTO` slim projection
- [ ] `GET /api/reports/map` with filters + bounding box
- [ ] Citizen visibility rule (no pending/rejected pins)
- [ ] Indexes on coordinates and status

### Frontend
- [ ] Shared `ReportMap` component (Leaflet)
- [ ] Marker clustering
- [ ] Category-coloured pins + status popups
- [ ] Citizen map tab with filter chips
- [ ] Pin bottom sheet (citizen)
- [ ] Console full-screen map view + filter rail
- [ ] Slide-over report panel (console)
- [ ] Debounced refetch on filter / bounds change

## Phase 7 — Notifications, Feedback & Gamification

### Backend
- [ ] `Notification` entity, repository, DTOs, mapper
- [ ] `NotificationService` + triggers for every event type
- [ ] Notification endpoints (list, unread count, mark read, mark all)
- [ ] `@Scheduled` "waiting too long" sweeper
- [ ] `EmailService` (`@Async`, failure-tolerant)
- [ ] `Feedback` entity, repository, DTOs, mapper, service, controller
- [ ] `PointTransaction` entity + repository
- [ ] `ScoreService` — idempotent awards + cached total
- [ ] Leaderboard query + controller
- [ ] Score awards wired into approve / reject / resolve / feedback
- [ ] Tests: duplicate-award prevention, feedback constraints, leaderboard order

### Frontend
- [ ] Notification bell + unread badge
- [ ] Notification list page (mark read)
- [ ] Feedback form on resolved reports
- [ ] Leaderboard page (own row pinned)
- [ ] Score page + point history

## Phase 8 — Dashboards & Analytics

### Backend
- [ ] Aggregate projections in `ReportRepository`
- [ ] `DashboardService`
- [ ] `/api/dashboard/admin`
- [ ] `/api/dashboard/staff`
- [ ] `/api/dashboard/departments`
- [ ] `/api/dashboard/categories`
- [ ] Optional: CSV summary export

### Frontend
- [ ] Admin dashboard (stat cards, recent registrations, pending reports)
- [ ] Staff dashboard (stat cards, compact map, monthly chart, recent table)
- [ ] Departments page (pie chart, bar chart, performance table)
- [ ] Chart empty states

## Phase 9 — Testing, Polish, Deployment & Presentation

- [ ] All priority test cases from `testing-standards.md` written and passing
- [ ] `docs/test-cases.md` completed
- [ ] `docs/bug-log.md` completed
- [ ] Postman collection exported
- [ ] Responsive pass (360 px citizen, 1280 px console, tablet)
- [ ] Accessibility pass (focus, labels, non-colour status, reduced motion)
- [ ] Loading / empty / error states verified on every page
- [ ] Demo seed dataset
- [ ] `README.md` with setup and credentials
- [ ] Diagrams updated to match shipped code
- [ ] Backend + database deployed
- [ ] Frontend deployed + smoke tested
- [ ] Presentation deck
- [ ] Demo script rehearsed (sign-up → approve → report → approve → resolve → rate → dashboard)

---

## Architectural Decisions Made During Development

Append here as work proceeds, then mirror anything significant into `project-overview.md` § Decisions Log.

| Date | Decision | Reason |
|------|----------|--------|
| 2026-08-13 | `User.departmentId` is a plain `Long` FK column, not a `@ManyToOne` to `Department`, until Phase 2 | The `Department` entity doesn't exist yet; a relation can't compile against a nonexistent type. Convert to `@ManyToOne(fetch = LAZY)` once `department/entity/Department.java` is built. |
| 2026-08-13 | `AccountNotApprovedException` maps to 403, not the 400 shown in `code-standards.md`'s generic exception-handler example | `api-standards.md` explicitly documents 403 for `PENDING`/`REJECTED`/`SUSPENDED` accounts on login. `AuthService.login`/`.me` are `@Transactional(readOnly = true)` — `User.role` is lazy and `open-in-view=false`, so the mapper needs the Hibernate session still open when it reads `user.getRole()` (see BUG-01 in `docs/bug-log.md`). |
| 2026-08-14 | `DepartmentRepository`/`CategoryRepository` expose `findByActiveTrue()`, not the `findByIsActiveTrue()` name written in `database-schema.md`'s original draft | Both entities store the flag as a field named `active` with a Java-Bean-compliant `isActive()` getter (matching `User.active`/`isActive()`). Spring Data's derived-query parser resolves the property name from the JavaBean spec (`active`), so `findByIsActiveTrue()` fails at startup with `PropertyReferenceException: No property 'isActive' found`. `database-schema.md` has been corrected to match the working method name (see BUG-02 in `docs/bug-log.md`). |
| 2026-08-14 | `User.departmentId` stays a plain `Long` FK, not converted to `@ManyToOne(fetch = LAZY) Department`, even though `Department` now exists | The field is only populated for staff accounts, and staff creation is Phase 3 work. Converting now would touch `AuthMapper`, `DataSeeder`, and every Phase 1 test for no behavioural gain this phase. Revisit when `UserService.createStaff()` is built. |
| 2026-08-15 | Phase 4's `POST /api/reports` (create-report) slice was built directly on top of Phase 2, skipping Phase 3 (`UserService`/`UserController` — approval queue, staff creation), which is still fully unchecked | Explicit instruction from the session's task brief, which named this exact endpoint as the deliverable and listed Phase 3/5 features as out of scope. The `accountStatus == APPROVED` gate in `ReportService.createReport()` only reads the `User.accountStatus` field seeded in Phase 1 — it does not require any Phase 3 endpoint to exist. Per `build-plan.md`'s own sequencing rule, Phase 3 must still be completed before Phase 4 is considered done; a citizen can only reach `APPROVED` today by being edited directly in the database. |
| 2026-08-15 | `Report`/`ReportImage` entities were built matching the **full** `database-schema.md` column set (including `assignedStaffId`, `rejectionReason`, `approvedAt`/`approvedBy`, `resolvedAt`, `closedAt`, `departmentId`) rather than only the columns `createReport()` populates | `database-schema.md` is the agreed single source of truth for the `reports` table shape, and Hibernate `ddl-auto=update` generates the real table from this entity. Building a partial entity now would force a schema-affecting rewrite in Phase 5 for no reason; no Phase 5 *logic* (status transitions, assignment, workflow) was added — only the persistence columns exist, all nullable and untouched by this phase's service. |
| 2026-08-15 | `GET /api/reports/my` and `GET /api/reports/{id}` (listed under Phase 4 in `build-plan.md`) were **not** implemented this session | The task brief scoped this session strictly to "the backend functionality necessary for an authenticated CITIZEN to submit a new community issue report" and did not list these read endpoints among the acceptance criteria. Left unchecked above rather than falsely marked done; pick up in the next Phase 4 session. |
| 2026-08-16 | `UserService.approve()`/`.reject()`/`.suspend()` do not write a `notifications` row, even though `build-plan.md` calls for a "notifications stub" in Phase 3 | The `Notification` entity/repository (Phase 7 work per `progress-tracker.md`) does not exist yet, so there is no table to write to. Writing an ad-hoc notification mechanism now would invent a Phase 7 architectural layer ahead of schedule (rule 3 in `CLAUDE.md`). Revisit when `notification/entity/Notification.java` is built. |
| 2026-08-16 | `RejectUserDTO.reason` is validated (`@NotBlank`) but not persisted anywhere on `User` | Unlike `reports.rejection_reason`, `database-schema.md`'s `users` table has no column to store an account-rejection reason — this was a deliberate omission, not an oversight (rule 1: never guess a field). The reason exists in the API contract today so the reason text is available once `NotificationService` (Phase 7) is built to embed it in the `ACCOUNT_REJECTED` notification message; until then it is validated but discarded. |
| 2026-08-16 | `UserService.approve()`/`.reject()` require the target account to currently be `PENDING`; `.suspend()` requires `APPROVED` | Not specified in `api-standards.md`, but these are the only transitions that make sense given `AccountStatus`'s seeding rules (only citizens are ever `PENDING`; admin/staff are seeded/created directly as `APPROVED`). Chosen defensively so, e.g., an already-approved account can't be re-approved (which would silently no-op) or a rejected account suspended. |
| 2026-08-16 | `UserDTO` (in `auth/dto/`, reused by the new `user` module) gained an `active` boolean field, populated by both `AuthMapper` and the new `UserMapper` | Needed to make `UserService.delete()` (soft delete) observable via the API — without it, an admin's citizen/staff account tables could not distinguish an active account from a soft-deleted one, mirroring the `active` field already exposed on `CategoryDTO`/`DepartmentDTO`. |
| 2026-08-16 | `UpdateUserDTO` (self-service `PUT /api/users/{id}`) only exposes `fullName`, `phone`, `profileImageUrl` | `email`, `dateOfBirth`, and `nrcNumber` are treated as immutable identity fields not covered by any documented endpoint; changing email in particular would need separate re-verification handling that is out of scope for this phase. |
| 2026-08-15 | `SupabaseStorageService` (the second storage backend from `library-docs.md`/D4) was not implemented; only `LocalStorageService` exists | `app.storage.provider` defaults to `local`, so report creation with photo upload is fully functional without it, and building an untested external integration without Supabase credentials in this environment was judged out of scope for "the minimum backend functionality necessary" to submit a report. `FileStorageService` is coded to the interface so a future `SupabaseStorageService` bean can be added without touching `ReportService`. |

---

## Known Issues / Blockers

| ID | Description | Impact | Owner | Status |
|----|-------------|--------|-------|--------|
| | | | | |
