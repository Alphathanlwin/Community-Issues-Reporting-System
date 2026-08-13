# 10. SCIRS — Progress Tracker

**Current Phase:** Phase 0 — Analysis & Design
**Last Updated:** 2026-08-13

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
- [ ] `common/exception/` — ErrorResponse, custom exceptions, GlobalExceptionHandler
- [ ] `common/config/` — CORS, async, property beans
- [ ] `User`, `Role` entities + `RoleName`, `AccountStatus` enums
- [ ] `UserRepository`, `RoleRepository`
- [ ] Auth DTOs (login, register, response, UserDTO)
- [ ] `AuthService` — login
- [ ] `AuthService` — citizen registration (forced role + PENDING status)
- [ ] `JwtUtil` (generate, validate, extract claims)
- [ ] `CurrentUser` principal
- [ ] `JwtAuthenticationFilter`
- [ ] `SecurityConfig` with public/protected rules
- [ ] `AuthController` — login, register, me
- [ ] `DataSeeder` — roles + admin account (idempotent)
- [ ] Tests: login success/failure, pending block, 401 without token, 403 wrong role

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
- [ ] `Department` entity, repository, DTOs, mapper
- [ ] `DepartmentService` + `DepartmentController` (CRUD, soft delete)
- [ ] `Category` entity, repository, DTOs, mapper
- [ ] `CategoryService` + `CategoryController` (CRUD, soft delete)
- [ ] Seed 6 departments
- [ ] Seed default categories with department mapping
- [ ] Tests: category requires active department, soft delete behaviour

### Frontend
- [ ] Departments list page
- [ ] Department create/edit form
- [ ] Categories list page
- [ ] Category create/edit form (department, colour, icon)

## Phase 3 — User Management & Approval Queues

### Backend
- [ ] `UserService` — list/filter users
- [ ] `UserService` — create staff account
- [ ] `UserService` — approve / reject / suspend citizen
- [ ] `UserService` — soft delete
- [ ] `UserController` — all user endpoints
- [ ] Tests: staff requires department, approval flow, role gates

### Frontend
- [ ] Citizen accounts table
- [ ] Staff accounts table
- [ ] Create new staff form
- [ ] Account approval queue (approve / deny)
- [ ] Citizen profile page (view + edit)

## Phase 4 — Report Submission

### Backend
- [ ] `Report`, `ReportImage` entities + enums
- [ ] `ReportRepository`, `ReportImageRepository`
- [ ] `FileStorageService` interface
- [ ] `LocalStorageService` implementation
- [ ] `SupabaseStorageService` implementation
- [ ] Image validation (type, size, magic bytes, renamed file)
- [ ] `ReportCodeGenerator`
- [ ] `ReportService.createReport()` (multipart)
- [ ] `GET /api/reports/my`
- [ ] `GET /api/reports/{id}` with ownership check
- [ ] Tests: pending citizen blocked, validation, code uniqueness, ownership

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
| | | |

---

## Known Issues / Blockers

| ID | Description | Impact | Owner | Status |
|----|-------------|--------|-------|--------|
| | | | | |
