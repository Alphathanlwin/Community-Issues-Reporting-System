# 10. SCIRS — Progress Tracker

**Current Phase:** Phase 3 — User Management & Approval Queues (backend complete, now wired to real account-approved/rejected notifications) and Phase 7 — Notifications, Feedback & Gamification (backend + frontend complete) have merged onto `main`. Phase 4 (create-report backend slice) and most of Phase 5 (approve/reject/status workflow) are also in; Phase 5 assignment/priority/comments, and the Phase 1–3/5/6 console frontend, remain.
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
- [x] React + TypeScript + Tailwind + shadcn/ui scaffolded

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
- [x] API client with JWT injection + 401 handling
- [x] `AuthContext` + token storage
- [x] Login page
- [ ] Citizen registration page
- [x] `ProtectedRoute` + role-based shell routing
- [x] `CitizenShell` skeleton (bottom tabs)
- [x] `ConsoleShell` skeleton (navbar + sidebar)

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
- [x] `UserService` — approve / reject / suspend citizen (each requires the current status listed in `api-standards.md`; approve/reject now fire real `NotificationService.notifyAccountApproved`/`notifyAccountRejected` calls — see Decisions)
- [x] `UserService` — soft delete (`isActive = false`)
- [x] `UserController` — all endpoints from `api-standards.md` § User Management, including `GET`/`PUT /api/users/{id}` (self or admin)
- [x] Tests: staff requires an active department (unit + integration), approval/reject/suspend flow (unit + integration), role gates (integration, 403 for non-admin/non-self), notification dispatch verified (unit)

### Frontend
- [ ] Citizen accounts table
- [ ] Staff accounts table
- [ ] Create new staff form
- [ ] Account approval queue (approve / deny)
- [ ] Citizen profile page (view + edit) — a read-only self profile page (`pages/citizen/ProfilePage.tsx`) exists, but not the admin-facing view/edit-any-citizen page this item describes

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
- [x] `GET /api/reports/my`
- [x] `GET /api/reports/{id}` with ownership check
- [x] Tests: unapproved citizen blocked, unknown category rejected, unknown citizen rejected, lat/long bounds, blank title, invalid image content, image storage invoked + URL persisted, 401 unauthenticated, 403 non-citizen, 201 happy path with `PENDING_APPROVAL` + correct owner

### Frontend
- [ ] Geolocation hook + permission-denied fallback
- [ ] Report submission form (location, category, chips, description, photo)
- [ ] Image preview + remove
- [x] Citizen home page with recent reports (also the anchor point for the Phase 7 feedback form on resolved reports)
- [ ] Citizen report detail page

## Phase 5 — Approval, Routing & Workflow

### Backend
- [x] `ReportStatusHistory` entity + repository
- [ ] `ReportComment` entity + repository — not needed by any Phase 7 trigger; deferred with `ReportAssignmentService`/priority/comments below
- [x] `StatusHistoryService`
- [x] `ReportWorkflowService` — transition matrix
- [x] Approve endpoint (auto-route + history + a **real** notification, not a stub — `NotificationService` now exists)
- [x] Reject endpoint (reason required)
- [x] Status change endpoint (remarks)
- [x] Resolution-photo requirement before RESOLVED
- [ ] `ReportAssignmentService` — reassign department / assign staff — out of scope this session (not a Phase 7 dependency); department is still auto-routed on approval
- [ ] Priority endpoint — out of scope this session, same reason
- [x] History endpoint
- [ ] Comments endpoints (+ department mention) — depends on the `ReportComment` entity above, not built
- [x] Department scoping for staff queries (`ReportService.getReports`, `ReportWorkflowService.assertStaffOwnsDepartment`)
- [x] Tests: full transition matrix (parameterised), history per change, department scoping

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
- [x] `Notification` entity, repository, DTOs, mapper
- [x] `NotificationService` + triggers for every event type reachable from what's built (new report, status change incl. approve/reject/resolve, account approved/rejected, waiting-too-long); `URGENT_REPORT` and `DEPARTMENT_MENTION` triggers are wired in the `NotificationType` enum but have no caller yet since priority and comments aren't built (see Phase 5 notes)
- [x] Notification endpoints (list, unread count, mark read, mark all)
- [x] `@Scheduled` "waiting too long" sweeper (`ReportService.sweepWaitingTooLong`, hourly, `app.sla.waiting-too-long-hours`, guarded against duplicate alerts)
- [x] `EmailService` (`@Async`, failure-tolerant, gated by `app.mail.enabled`)
- [x] `Feedback` entity, repository, DTOs, mapper, service, controller
- [x] `PointTransaction` entity + repository
- [x] `ScoreService` — idempotent awards + cached total (points-per-reason centralised in `ScoreService.POINTS_BY_REASON`, callers only pass the reason)
- [x] Leaderboard query + controller
- [x] Score awards wired into approve / reject / resolve / feedback
- [x] Tests: duplicate-award prevention, feedback constraints, leaderboard order, plus a full `approve → status → resolve → feedback` integration test asserting the exact point total and notification count

### Frontend
- [x] Notification bell + unread badge (60 s poll)
- [x] Notification list page (mark read / mark all read)
- [x] Feedback form on resolved reports (`components/feedback/FeedbackForm.tsx`, surfaced from the citizen Home page's recent-reports list)
- [x] Leaderboard page (own row highlighted, with a fallback message when not yet ranked)
- [x] Score page + point history

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
| 2026-08-16 | Phase 7 (Notifications, Feedback, Gamification) was built on top of only a **minimal slice** of Phase 3 (`UserService`/`UserController` — no admin UI) and Phase 5 (`ReportWorkflowService` covering approve/reject/status/history, but not assignment, priority, or comments) rather than those phases being fully completed first | `build-plan.md`'s own sequencing rule ("never start a phase while the previous has unchecked items … unless the user explicitly says to") was explicitly overridden by the user for this session, scoped to exactly the prerequisites Phase 7's triggers genuinely depend on: an approval flow so accounts exist, and a workflow so a report can actually reach `RESOLVED`. Manual department/staff reassignment, priority, and internal comments are not touched by any Phase 7 code path and were left for a dedicated Phase 5 session. |
| 2026-08-16 | `/frontend` was scaffolded from scratch this session (Vite + React 19 + TypeScript + Tailwind + shadcn/ui, hand-configured rather than via the interactive CLI) and only the routes/pages needed to reach the Phase 7 UI were built (login, both shells, Home with recent reports + feedback, leaderboard, score, notifications, settings/profile) | Same session-scoping decision as above. Registration, the report submission flow, the map, and the full admin/staff consoles remain future-phase work; their nav entries are intentionally omitted (console sidebar) or point to an explicit "coming soon" placeholder (citizen Map/Report tabs) rather than a dead link, per `ui-rules.md`'s "never render a link the user cannot open." |
| 2026-08-16 | `ScoreService.award(user, reason, report)` takes no `points` parameter — the reason→points mapping (`+10`/`+20`/`+5`/`−5`) lives in a single `POINTS_BY_REASON` map inside `ScoreService`, not at each of the four call sites | `testing-standards.md`'s own `ReportWorkflowServiceTest` example calls `scoreService.award(reporter, PointReason.REPORT_APPROVED, report)` — no points argument — which is also safer: a caller passing the wrong literal for a reason was a real risk with the original 4-arg signature. |
| 2026-08-16 | Backend tests must be run with `JAVA_HOME` pointed at a JDK 21 install (e.g. `ms-21.0.12`), not whatever `mvn` resolves by default | This machine's Homebrew Maven install pulled in OpenJDK 26 as a dependency; Mockito's inline mock maker (bundled with this Spring Boot 3.3.4 / Mockito version) cannot instrument classes under JDK 26, producing `MockitoException: Cannot modify class` failures unrelated to any test's own logic. The project's own `java.version` is 21, and a JDK 21 install already existed on this machine at `/Users/thantthadar/Library/Java/JavaVirtualMachines/ms-21.0.12`. |
| 2026-08-16 | Merged the local, fuller Phase 3 `user` module (status guards on approve/reject/suspend, dedicated `UserMapper`, phone-uniqueness check on update) with the incoming Phase 7 branch's notification wiring (`notificationService.notifyAccountApproved`/`notifyAccountRejected` on `createStaff`/`approve`/`reject`) rather than picking one side wholesale; method names (`getAll`, `getById`, `update`, `delete`, …) were resolved to match the `getAll`/`getById`/`update`/`delete` convention already established by `DepartmentController`/`CategoryController`, and `UserController.reject()` now extracts the reason string from `RejectUserDTO` before calling the service, matching `ReportController.reject()`'s existing pattern | This supersedes the 2026-08-16 decision above stating `approve()`/`.reject()`/`.suspend()` write no notification — `Notification`/`NotificationService` arrived with the merged Phase 7 branch, so the blocker no longer applies. `RejectUserDTO.reason` is still not persisted to a `User` column (per that same earlier decision), but it is now used transiently as the `notifyAccountRejected` message. |

---

## Known Issues / Blockers

| ID | Description | Impact | Owner | Status |
|----|-------------|--------|-------|--------|
| | | | | |
