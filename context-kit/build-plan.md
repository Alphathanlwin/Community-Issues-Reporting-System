# 9. SCIRS — Build Plan

Phased development roadmap mapped to the 8-week schedule in the project proposal. Complete each phase fully before moving to the next — later phases assume earlier ones work.

| Phase | Focus | Proposal week |
|-------|-------|---------------|
| 0 | Analysis, design artefacts, environment | Week 1–2 |
| 1 | Foundation, security, authentication | Week 3 |
| 2 | Master data: departments & categories | Week 3 |
| 3 | User management & approval queues | Week 3–4 |
| 4 | Report submission (citizen) | Week 4 |
| 5 | Approval, routing & workflow | Week 5 |
| 6 | Map & filtering | Week 5–7 |
| 7 | Notifications, feedback & gamification | Week 6 |
| 8 | Dashboards & analytics | Week 7 |
| 9 | Testing, polish, deployment, presentation | Week 8 |

---

## Phase 0 — Analysis & Design (Week 1–2)

Deliverables the rubric grades under "Requirements Analysis & Design" (20%).

- Finalise the proposal (done)
- Use case diagram — three actors, all use cases from `project-overview.md`
- System architecture diagram — the layered diagram from `architecture.md`
- ER diagram matching `database-schema.md` exactly
- Report status state-machine diagram
- UI wireframes for both shells
- Git repository initialised, branch strategy agreed
- PostgreSQL database created, connection verified
- Spring Boot project scaffolded with dependencies: Web, Data JPA, Security, Validation, PostgreSQL Driver, Mail, Lombok (optional), jjwt
- React + TypeScript + Tailwind + shadcn/ui project scaffolded

---

## Phase 1 — Foundation & Authentication (Week 3)

**Goal:** anyone can log in; roles are enforced; the app has a spine.

### Backend
- `common/exception/` — `ErrorResponse`, custom exceptions, `GlobalExceptionHandler`
- `common/config/` — CORS, async, app properties beans
- `user/entity/` — `User`, `Role`, `RoleName`, `AccountStatus`
- `user/repository/` — `UserRepository`, `RoleRepository`
- `auth/dto/` — `LoginRequestDTO`, `LoginResponseDTO`, `CitizenRegisterDTO`, `UserDTO`
- `auth/service/` — `AuthService` (login, citizen register, token issuance)
- `common/security/` — `JwtUtil`, `CurrentUser`, `JwtAuthenticationFilter`, `SecurityConfig`
- `auth/controller/` — `AuthController` (`login`, `register`, `me`)
- `common/config/DataSeeder` — seed roles + one admin account (idempotent)
- Tests: login success/failure, pending-account block, protected endpoint without token

### Frontend
- Auth context, token storage, API client with JWT injection and 401 handling
- Login page, citizen registration page
- `ProtectedRoute` with role-based shell selection
- `CitizenShell` (bottom tabs) and `ConsoleShell` (navbar + sidebar) skeletons

---

## Phase 2 — Departments & Categories (Week 3)

**Goal:** the routing table exists before anything routes.

### Backend
- `department/` — entity, repository, DTOs, mapper, service, controller (full CRUD, soft delete)
- `category/` — entity, repository, DTOs, mapper, service, controller (full CRUD, soft delete, `departmentId` required)
- Seed 6 departments and their categories
- Tests: category must reference an active department; deleting a department in use is a soft delete

### Frontend
- Console: Departments list + create/edit form
- Console: Categories list + create/edit form (with department dropdown, colour, icon)

---

## Phase 3 — User Management & Approval Queues (Week 3–4)

**Goal:** the admin can run the account side of the system end to end.

### Backend
- `user/service/UserService` — list, filter, staff creation, approve, reject, suspend, soft delete
- `user/controller/UserController` — all endpoints from `api-standards.md` § User Management
- Notifications stub: approving/rejecting an account records a notification row (wired properly in Phase 7)
- Tests: staff creation requires a department; approval flips status; citizens cannot reach these endpoints

### Frontend
- Console: Citizen accounts table (search, filter by status)
- Console: Staff accounts table + "Create new staff" form
- Console: Account approval queue with Approve / Deny actions
- Citizen: Profile page (view + edit own details)

---

## Phase 4 — Report Submission (Week 4)

**Goal:** an approved citizen can file a report with a photo and GPS.

### Backend
- `report/entity/` — `Report`, `ReportImage`, `ReportStatus`, `ReportPriority`, `ImageType`
- `report/repository/` — `ReportRepository`, `ReportImageRepository`
- `common/integration/FileStorageService` + local implementation (+ Supabase implementation)
- `common/util/ReportCodeGenerator`
- `report/service/ReportService` — create (multipart), get own, get by id with ownership checks
- `report/controller/ReportController` — `POST /api/reports`, `GET /api/reports/my`, `GET /api/reports/{id}`
- Tests: pending citizen blocked, image validation, code uniqueness, ownership enforcement

### Frontend
- Citizen: report form with geolocation capture, category select, preset problem chips, description, photo picker with preview
- Citizen: Home page with own recent reports
- Citizen: report detail page (photos, status, submitted date)

---

## Phase 5 — Approval, Routing & Workflow (Week 5)

**Goal:** the full lifecycle works, with history and validation.

### Backend
- `report/entity/ReportStatusHistory`, `ReportComment`
- `report/service/ReportWorkflowService` — transition matrix, approve, reject, status change, resolution-photo requirement
- `report/service/ReportAssignmentService` — auto-route on approval, manual reassignment, staff assignment
- `report/service/StatusHistoryService` — append-only history writes
- Endpoints: `pending`, `approve`, `reject`, `status`, `assign`, `priority`, `images`, `history`, `comments`
- Department-scoped filtering for staff
- Tests: full transition matrix (parameterised), history row per change, department scoping

### Frontend
- Console: Reports list with search + filters + pagination
- Console: Report detail with Overview / Timeline / Comments / Resolution tabs
- Console: status change dialog, assign dialog, priority control, completion photo upload
- Admin: Report approval queue with Approve / Deny (reason required)
- Citizen: status timeline on the report detail page

---

## Phase 6 — Map & Filtering (Week 5–7)

**Goal:** both audiences can see problem hotspots at a glance.

### Backend
- `ReportMapDTO` slim projection + `GET /api/reports/map` with category, status, and bounding-box filters
- Visibility rule: citizens never receive `PENDING_APPROVAL` or `REJECTED` pins
- Index check on `(latitude, longitude)` and `status`

### Frontend
- Shared `ReportMap` component (Leaflet + marker clustering)
- Category-coloured pins, status-marked popups
- Citizen: map tab with filter chips and a pin bottom sheet
- Console: full-screen map view with the full filter rail and a slide-over detail panel
- Debounced re-fetch on filter and bounds change

---

## Phase 7 — Notifications, Feedback & Gamification (Week 6)

**Goal:** citizens stay informed and engaged.

### Backend
- `notification/` — entity, repository, DTOs, mapper, `NotificationService`, controller
- Notification triggers: new report (admins + department staff), status change (reporter), urgent report, department mention, completion, account approved/rejected
- `@Scheduled` sweeper for "waiting too long" (report in `ASSIGNED`/`IN_PROGRESS` past an SLA threshold)
- `EmailService` (`@Async`, failure-tolerant)
- `feedback/` — entity, repository, DTOs, mapper, service (one per report, reporter only, resolved only), controller
- `score/` — `PointTransaction`, `ScoreService` (idempotent awards, cached total), leaderboard query, controllers
- Wire score awards into approve / reject / resolve / feedback
- Tests: duplicate-award prevention, feedback constraints, leaderboard ordering, notification per status change

### Frontend
- Notification bell with unread count + notification list (mark read / mark all read)
- Citizen: feedback form on resolved reports (1–5 rating + comment)
- Citizen: leaderboard page with own row pinned
- Citizen: score page with point history

---

## Phase 8 — Dashboards & Analytics (Week 7)

**Goal:** decision-support data for admins and staff.

### Backend
- `dashboard/service/DashboardService` with aggregate `@Query` projections only
- `/api/dashboard/admin`, `/api/dashboard/staff`, `/api/dashboard/departments`, `/api/dashboard/categories`
- Metrics: totals by status, monthly volume per department, average resolution time, average citizen rating, issue volume by category
- Optional: CSV export of a summary report

### Frontend
- Admin dashboard: stat cards + recent registrations + reports awaiting approval
- Staff dashboard: stat cards + compact map + monthly bar chart + recent reports table
- Departments page: workload pie chart, monthly bar chart, performance table
- Empty states on every chart

---

## Phase 9 — Testing, Polish, Deployment & Presentation (Week 8)

- Complete the priority test list in `testing-standards.md`
- Fill in `docs/test-cases.md` and `docs/bug-log.md`
- Export the Postman collection
- Responsive pass: citizen shell at 360 px, console at 1280 px, tablet check
- Accessibility pass: focus rings, labels, colour-independent status, reduced motion
- Error, loading and empty states verified on every page
- Seed a realistic demo dataset (approved citizens, ~30 reports across all statuses and departments, a populated leaderboard)
- `README.md`: setup, environment variables, run instructions, default credentials
- Final documentation: updated diagrams matching the shipped code
- Deployment (backend + database + frontend) and a smoke test on the deployed URL
- Presentation deck + a rehearsed 3-actor demo script: citizen signs up → admin approves → citizen reports → admin approves → staff resolves → citizen rates → dashboard updates

---

## Sequencing Rules for Agents

1. Never start a phase while the previous phase has unchecked items in `progress-tracker.md` — unless the user explicitly says to.
2. Backend before frontend within a phase; the API contract is settled first.
3. Write the service tests in the same session as the service, not "later".
4. Any new entity field requires a `database-schema.md` update in the same change.
5. Any new endpoint requires an `api-standards.md` update in the same change.
