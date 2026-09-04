# 10. SCIRS — Progress Tracker

**Current Phase:** Phase 5 — Approval, Routing & Workflow (backend complete). Phase 8 — Dashboards & Analytics (backend), Phase 6 — Map & Filtering (backend map endpoint), Phase 3 — User Management & Approval Queues (backend), and Phase 7 — Notifications, Feedback & Gamification (backend + frontend) have also merged onto `main`. Phase 4 (create-report backend slice) is in too; only frontend work remains for Phases 1–3, 5, 6, and 8.
**Last Updated:** 2026-09-05

> **⚠️ `/frontend` was deleted from the repo** (commit `3347e8a`, "fix: delete frontend files") — every `[x]` frontend box below predates that commit and no longer reflects a working app. Rebuilding the frontend from scratch is required before any frontend checklist item is trustworthy again.

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
- [x] `SupabaseStorageService` implementation — uploads to a public Supabase Storage bucket via its REST API using Spring's `RestClient` (no new dependency); `ImageValidator` extracted out of `LocalStorageService` so both backends share the same size/type/magic-byte checks; selected via `app.storage.provider=supabase` (`@ConditionalOnProperty`, mutually exclusive with `LocalStorageService`'s `local`/default)
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
- [x] `ReportComment` entity + repository (`findByReportIdOrderByCreatedAtAsc`, matching `database-schema.md` exactly)
- [x] `StatusHistoryService`
- [x] `ReportWorkflowService` — transition matrix
- [x] Approve endpoint (auto-route + history + a **real** notification, not a stub — `NotificationService` now exists)
- [x] Reject endpoint (reason required)
- [x] Status change endpoint (remarks)
- [x] Resolution-photo requirement before RESOLVED
- [x] `ReportAssignmentService` — `PATCH /api/reports/{id}/assign` (ADMIN only), reassigns department and/or staff, allowed only for `ASSIGNED`/`IN_PROGRESS`/`RESOLVED`, writes a same-status `report_status_history` row per `architecture.md` § Reassignment
- [x] Priority endpoint — `PATCH /api/reports/{id}/priority` (`ReportWorkflowService.updatePriority`, STAFF own-department/ADMIN), no history row (priority has no audit column); now a post-approval override, not the only way priority is set (see next item)
- [~] `PriorityService` (`report/service/PriorityService.java`) — computes priority automatically in `ReportWorkflowService.approve()` from `category.severityWeight` (1–5, seeded per category in `DataSeeder`), a duplicate-count bonus (parameter always `0` today — no duplicate-detection feature exists), and a report-age bonus; writes both `report.priority` and the new `reports.priority_score` column. `categories.severity_weight` and `reports.priority_score` added to `database-schema.md` in the same change. Marked in-progress, not done, because `mvn test` could not be run this session (see Known Issues KI-03) — code and tests are written but unverified by an actual test run.
- [x] History endpoint
- [x] Comments endpoints (+ department mention) — `ReportCommentService`, `GET`/`POST /api/reports/{id}/comments` (ADMIN, STAFF own-department); `mentionedDepartmentId` now fires `NotificationService.notifyDepartmentMention()`, finally giving `NotificationType.DEPARTMENT_MENTION` a caller
- [x] Department scoping for staff queries (`ReportService.getReports`, `ReportWorkflowService.assertStaffOwnsDepartment`, and the same check duplicated again in `ReportCommentService` per the codebase's existing per-service convention)
- [~] Tests: full transition matrix (parameterised), history per change, department scoping, priority happy/unknown-value/wrong-department paths, assignment (department-only, staff-only, wrong-department staff, non-staff target, neither field, terminal status, inactive department, unknown department), comments (with/without mention, department-scoped visibility, unknown mentioned department); `PriorityServiceTest` (new) covers exact score arithmetic across severity/duplicate/age combinations and both threshold boundaries (score 5 → HIGH, score 8 → URGENT); `ReportWorkflowServiceTest.approve_setsStatusAssignedAndRoutesToCategoryDepartment` now also asserts `report.priority`/`report.priorityScore` are persisted from `PriorityService`'s result — the pre-existing tests were still `[x]` as of 2026-09-04, but the whole line is downgraded to `[~]` here because none of it (old or new) could actually be re-run this session, per KI-03

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
- [x] `ReportMapDTO` slim projection (`id`, `reportCode`, `latitude`, `longitude`, `categoryName`, `categoryColor`, `status`, `priority`, `createdAt` — matches `api-standards.md`'s documented shape exactly)
- [x] `GET /api/reports/map` with `categoryId`, `status`, and `minLat`/`maxLat`/`minLng`/`maxLng` bounding-box filters, all optional
- [x] Citizen visibility rule — `ReportService.getMapPins()` excludes `PENDING_APPROVAL` and `REJECTED` when the caller is `CITIZEN`; ADMIN/STAFF see every status
- [x] Indexes on coordinates and status (`@Table(indexes = ...)` on `Report`: `idx_reports_status` on `status`, `idx_reports_coordinates` composite on `(latitude, longitude)`)
- [x] Tests: citizen visibility rule (unit + integration), admin sees all statuses, bounding-box filter, status filter, slim payload shape, unauthenticated → 401

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
- [x] Aggregate projections in `ReportRepository` (`countByDepartmentAndStatusIn`, `findCreatedAtSince`, `countOpenAndResolvedByDepartment`, `findResolutionTimes`, `countGroupedByCategory`) and `FeedbackRepository` (`averageRatingByDepartment`) — interface-based projections, never full entity lists
- [x] `DashboardService` (`dashboard/service/`) — reads directly from `ReportRepository`/`UserRepository`/`FeedbackRepository`/`DepartmentRepository`/`CategoryRepository`, matching the cross-module repository access already established by `FeedbackService`/`UserService`/`CategoryService`, not routed through each module's service
- [x] `GET /api/dashboard/admin` — pending account/report counts, 10 latest citizen registrations, 10 latest reports awaiting approval
- [x] `GET /api/dashboard/staff` — total/resolved/remaining/new counts, a 12-month zero-filled series, 10 most recent reports; STAFF is forced to their own department, ADMIN may pass `departmentId` or omit it for a global view
- [x] `GET /api/dashboard/departments` — open/resolved counts, average resolution hours, average rating per department, including departments with no data (nulled averages, not zeroed)
- [x] `GET /api/dashboard/categories` — report count per category, including categories with no reports
- [ ] Optional: CSV summary export — not built (marked optional in `build-plan.md`; no CSV/export library is on the classpath and no session task has asked for it)
- [x] Tests: role gates (401/403) on all four endpoints, admin dashboard counts, staff dashboard department scoping (STAFF ignores a supplied `departmentId`; ADMIN honours it or aggregates globally), 12-month series zero-fill, department/category rows default to zero/`null` when a department or category has no reports yet

### Frontend
- [ ] Admin dashboard (stat cards, recent registrations, pending reports)
- [ ] Staff dashboard (stat cards, compact map, monthly chart, recent table)
- [ ] Departments page (pie chart, bar chart, performance table)
- [ ] Chart empty states

## Phase 9 — Testing, Polish, Deployment & Presentation

Backend-only pass done 2026-08-31 — see the 2026-08-31 decisions above. The
frontend items (responsive/accessibility/loading-empty-error passes) are
blocked on KI-01 (no frontend in this repo) and untouched.

- [x] All 27 priority test cases from `testing-standards.md` written and passing — `./mvnw test` is 166/166 green (163 pre-existing + 3 new `LocalStorageServiceTest` cases for #25/#27); found and fixed two real bugs surfaced along the way (BUG-08 environment, BUG-09 `ReportAssignmentService` ordering — see `docs/bug-log.md`)
- [x] `docs/test-cases.md` completed — extended from 77 to 100 rows, closing the Feedback/Notification/Score/Leaderboard/Upload gaps
- [x] `docs/bug-log.md` completed — 9 entries, each with root cause and fix
- [x] Postman collection exported — `docs/postman/SCIRS.postman_collection.json`, every endpoint in `api-standards.md`, auto-captures the JWT on login
- [x] Interactive/generated API docs — added `springdoc-openapi-starter-webmvc-ui` (`OpenApiConfig`, `@Tag` on all 10 controllers); Swagger UI live at `/swagger-ui/index.html`, spec at `/v3/api-docs`, JWT bearer "Authorize" wired up; permitted unauthenticated in `SecurityConfig` alongside `/uploads/**`. Verified: all 40 endpoints across 10 tags, `mvn test` still 166/166 after the change.
- [ ] Responsive pass (360 px citizen, 1280 px console, tablet) — blocked on KI-01
- [ ] Accessibility pass (focus, labels, non-colour status, reduced motion) — blocked on KI-01
- [ ] Loading / empty / error states verified on every page — blocked on KI-01
- [x] Demo seed dataset — `MockDataSeeder`, 30 reports across every status; see the 2026-08-31 decision above
- [x] `README.md` with setup and credentials — rewritten with prerequisites, env vars, run/test instructions, seeded + demo credentials, Postman pointer
- [ ] Diagrams updated to match shipped code — not started this session
- [ ] Backend + database deployed — no hosting platform chosen yet
- [ ] Frontend deployed + smoke tested — blocked on KI-01 and a hosting platform
- [ ] Presentation deck — out of scope for an agent session; needs a human author
- [ ] Demo script rehearsed (sign-up → approve → report → approve → resolve → rate → dashboard) — needs an actual rehearsal, not something this session can do

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
| 2026-08-16 | ~~Backend tests must be run with `JAVA_HOME` pointed at a JDK 21 install~~ — **superseded 2026-08-31, see below** | This machine's Homebrew Maven install pulled in OpenJDK 26 as a dependency; Mockito's inline mock maker (bundled with this Spring Boot 3.3.4 / Mockito version) cannot instrument classes under JDK 26, producing `MockitoException: Cannot modify class` failures unrelated to any test's own logic. The project's own `java.version` is 21, and a JDK 21 install already existed on this machine at `/Users/thantthadar/Library/Java/JavaVirtualMachines/ms-21.0.12`. |
| 2026-08-31 | `pom.xml`'s `maven-surefire-plugin` now sets `argLine=-Dnet.bytebuddy.experimental=true`, superseding the 2026-08-16 decision above — no JDK 21 install is required to run `mvn test` any more | Hit the same JDK-version Mockito failure again on a machine with only JDK 25/26 installed and no JDK 21 available at all. Rather than requiring every contributor to install and juggle a second JDK, the actual root cause (ByteBuddy rejecting class file versions from JDK 22+) has a one-line fix: tell ByteBuddy to accept them. Verified `mvn test` (plain, no `JAVA_HOME` override, no manual `-DargLine`) passes 163/163 on JDK 25. See BUG-08 in `docs/bug-log.md`. |
| 2026-08-17 | Phase 6's map endpoint (`ReportMapDTO`, `GET /api/reports/map`, citizen visibility rule, indexes) was built without first finishing the remaining Phase 5 items (`ReportComment`/`ReportAssignmentService`/priority endpoint) or any Phase 1–3/5/6 console frontend | Explicit user instruction, scoped to exactly the four Phase 6 backend checklist items already fully specified in `database-schema.md` (composite `(latitude, longitude)` index) and `api-standards.md` (the exact `/api/reports/map` request/response shape was already documented, unchanged by this work). The map query only reads `Report.status`/`category`/`latitude`/`longitude`, none of which depend on assignment, priority, or comments. `findForMap()` always passes a non-empty `hiddenStatuses` list (`PENDING_APPROVAL`, `REJECTED`) to the `NOT IN` clause and toggles it on/off via a `restrictToPublic` boolean, rather than passing an empty list when unrestricted — Hibernate/JPQL `NOT IN` with an empty parameter list is not a construct worth relying on for correctness. |
| 2026-08-16 | Merged the local, fuller Phase 3 `user` module (status guards on approve/reject/suspend, dedicated `UserMapper`, phone-uniqueness check on update) with the incoming Phase 7 branch's notification wiring (`notificationService.notifyAccountApproved`/`notifyAccountRejected` on `createStaff`/`approve`/`reject`) rather than picking one side wholesale; method names (`getAll`, `getById`, `update`, `delete`, …) were resolved to match the `getAll`/`getById`/`update`/`delete` convention already established by `DepartmentController`/`CategoryController`, and `UserController.reject()` now extracts the reason string from `RejectUserDTO` before calling the service, matching `ReportController.reject()`'s existing pattern | This supersedes the 2026-08-16 decision above stating `approve()`/`.reject()`/`.suspend()` write no notification — `Notification`/`NotificationService` arrived with the merged Phase 7 branch, so the blocker no longer applies. `RejectUserDTO.reason` is still not persisted to a `User` column (per that same earlier decision), but it is now used transiently as the `notifyAccountRejected` message. |
| 2026-08-17 | `DashboardService` injects `ReportRepository`/`UserRepository`/`FeedbackRepository`/`DepartmentRepository`/`CategoryRepository` directly, rather than going through each module's service (`ReportService`, `UserService`, `FeedbackService`, ...) | `architecture.md`'s package rule ("cross-module communication goes through the Service layer only — never import another module's repository") is already not followed elsewhere in this codebase: `FeedbackService` injects `ReportRepository` directly, and `UserService`/`CategoryService` both inject `DepartmentRepository` directly. `database-schema.md`'s own Repository Layer table also states aggregate dashboard queries "live in `ReportRepository`", implying direct access. Matched the established convention for consistency rather than introducing a new, stricter pattern only for this module. |
| 2026-08-17 | `/api/dashboard/staff` is gated `hasAnyRole('ADMIN','STAFF')`, not `STAFF`-only as the Module Endpoint Map table in `api-standards.md` originally listed | The Role-Based Access Matrix further down the same file already documented "Staff dashboard: ADMIN ✅, STAFF ✅, CITIZEN ❌" — the two tables disagreed. Implemented to match the more specific access-matrix table and corrected the Module Endpoint Map row to say "ADMIN, STAFF" so the file is internally consistent. |
| 2026-08-17 | `GET /api/dashboard/staff` accepts an optional `departmentId` query param: STAFF callers always have it forced to their own JWT department (ignoring any value supplied), ADMIN callers may pass it or omit it for a global (all-departments) view | Neither `build-plan.md` nor `api-standards.md` specified how an ADMIN caller of the nominally-STAFF dashboard should be scoped, since admins have no department of their own. Chosen to keep STAFF's per-record ownership rule absolute (rule 7 in `CLAUDE.md`) while still making the shared endpoint useful for an admin who wants either a specific department's view or an aggregate one. |
| 2026-08-17 | The staff dashboard's "new" report count is defined as reports currently in `ASSIGNED` status (routed to the department, not yet picked up); "remaining" is `ASSIGNED` + `IN_PROGRESS`; "resolved" is `RESOLVED` + `CLOSED`; "total" is all four (a report only has a `department` once approved, so `PENDING_APPROVAL`/`REJECTED` are naturally excluded) | `api-standards.md` names these four metrics ("Total / resolved / remaining / new report counts") without defining them further, and no schema field distinguishes "new" from "remaining". This is the only self-consistent reading where the four numbers don't double-count or overlap in a confusing way; documented here per rule 10 in `CLAUDE.md` ("ask when uncertain about a design decision") since no one was available to ask mid-session. |
| 2026-08-17 | The staff dashboard's 12-month volume series is bucketed in Java (`ReportRepository.findCreatedAtSince()` returns raw timestamps; `DashboardService` groups them by `YearMonth`) instead of a single SQL `GROUP BY to_char(created_at, 'YYYY-MM')` query | A DB-side date-formatting function is Postgres-specific and its behaviour under the H2-in-PostgreSQL-mode test database (`application-test.properties`) could not be verified in this environment (no local Maven/JDK available to run the test suite this session — see the 2026-08-16 `JAVA_HOME` decision above for the general issue). Bucketing in Java is fully portable and, at this project's data scale, the extra rows fetched are negligible; it also lets the service zero-fill months with no reports, which a `GROUP BY` alone would silently omit from the chart. |
| 2026-08-17 | Department/category performance rows are seeded from `departmentRepository.findAll()` / `categoryRepository.findAll()` and left-joined with the aggregate counts in Java, rather than starting from the aggregate query's `GROUP BY` result | A `GROUP BY` only returns rows for departments/categories that already have at least one matching report or feedback row, which would silently drop brand-new departments/categories from the dashboard charts (`build-plan.md` explicitly calls for "chart empty states" on the frontend — the backend needs to hand back a zero row for that to render, not omit the row). `averageResolutionHours`/`averageRating` are left `null` (not defaulted to `0`) when there's no qualifying data, so the frontend can distinguish "no data yet" from "a real average of zero". |
| 2026-08-17 | The Phase 8 backend (`DashboardService`, four endpoints, aggregate projections) was built without first finishing the remaining Phase 5 items (`ReportComment`/`ReportAssignmentService`/priority) or any Phase 1–3/5/6/8 frontend | Same pattern as the 2026-08-17 Phase 6 decision above: explicit user instruction scoped to exactly the Phase 8 backend checklist in `progress-tracker.md`, whose metrics (status counts, timestamps, feedback ratings, category volume) only read fields that have existed since Phase 4/7 and don't depend on assignment, priority, or comments. CSV export was left undone as explicitly optional in `build-plan.md`. |
| 2026-08-20 | `/api/reports/{id}/assign` is gated `hasRole('ADMIN')` only, not "an admin (or staff with permission)" as `architecture.md`'s prose describes | `api-standards.md`'s endpoint table and Role-Based Access Matrix both explicitly say ADMIN-only for this endpoint ("Reassign department: ADMIN ✅, STAFF ❌, CITIZEN ❌") — the same precedent as Decision D8 in `project-overview.md`, where a more specific, endpoint-level source overrides a generic prose description elsewhere. |
| 2026-08-20 | `ReportAssignmentService.assign()` only permits reassignment while the report is `ASSIGNED`, `IN_PROGRESS`, or `RESOLVED` — not `PENDING_APPROVAL` (must go through `/approve`/`/reject` instead) even though it is technically non-terminal | `architecture.md` says reassignment is allowed "at any non-terminal status," but a `PENDING_APPROVAL` report has no `department` yet (it's only set on approval) and already has its own dedicated approve/reject flow with auto-routing and scoring side effects. Allowing `/assign` to silently route an unapproved report would bypass that flow. Documented here per rule 10 in `CLAUDE.md` since this reading wasn't spelled out explicitly. |
| 2026-08-20 | `ReportAssignmentService.assign()` validates a `staffId` against the *resulting* department — the one just set by `departmentId` in the same request, or the report's current department if `departmentId` was omitted — rather than always requiring `departmentId` to be sent alongside `staffId` | Keeps the two fields genuinely independent (per `api-standards.md`, either or both may be sent) while still enforcing the existing "staff must belong to their assigned department" invariant used everywhere else (`UserService.createStaff`, `ReportWorkflowService.assertStaffOwnsDepartment`). |
| 2026-08-20 | `updatePriority()` was added to `ReportWorkflowService` (not a new service) despite non-negotiable rule 8 in `CLAUDE.md` restricting that class to `report.status` mutations | Rule 8 names `report.status` specifically; `priority` is a distinct column with no transition matrix or audit table of its own. Reusing `ReportWorkflowService` avoided duplicating `assertStaffOwnsDepartment` a third time (it already appears once in `ReportService` and once here — see the established, if repetitive, per-service convention this codebase already follows rather than extracting a shared helper). |
| 2026-08-20 | `ReportCommentService` is a new, separate service (not folded into `ReportService`) | Mirrors `StatusHistoryService` already being split out from `ReportWorkflowService` — comments are a distinct concern (internal department notes + a notification trigger) with only two operations, and keeping it separate avoids growing `ReportService` into a god service (an explicit anti-pattern in `code-standards.md`). |
| 2026-08-20 | `ReportDTO` gained `assignedStaffId`/`assignedStaffName`, populated by `ReportMapper.toDTO()` | Without it, `PATCH /api/reports/{id}/assign`'s staff-assignment half would be invisible in every API response — `Report.assignedStaff` has existed on the entity since Phase 4 (per the 2026-08-15 decision above) but was never surfaced. |
| 2026-08-31 | `ReportAssignmentService.assign()` now resolves the acting admin user (`findUser(admin.getId())`) as the very last step, immediately before `reportRepository.save()`, instead of first thing in the method | Real bug (BUG-09 in `docs/bug-log.md`) surfaced by the full Phase 9 test-suite run: resolving the actor before validating the department/staff meant a validation failure could be masked by an unrelated `ResourceNotFoundException` if the admin id lookup happened to fail first — harmless in production (a real JWT's admin id always resolves) but it meant the method wasn't fail-fast in request-validation order, and it silently made two existing test stubs (`userRepository.findById(99L)` in `assign_withNonStaffUserAsStaffId_...` / `assign_withStaffFromAnotherDepartment_...`) into dead weight the moment the ordering changed. Removed those two now-unnecessary stubs; `ReportAssignmentServiceTest` is 8/8 green. |
| 2026-09-04 | `SupabaseStorageService` built via Spring's `RestClient` (part of `spring-web`, already on the classpath) rather than a dedicated Supabase Java SDK | No official Supabase Java SDK exists; the Storage API is plain REST (`POST`/`DELETE /storage/v1/object/{bucket}/{path}`, service-role key as both `apikey` and `Authorization: Bearer` headers). Adding a new HTTP client dependency for one integration would be unjustified when `RestClient` already covers it. `LocalStorageService`'s validation (size/type/magic-byte checks) was extracted into a new `ImageValidator` component so both storage backends enforce identical rules instead of duplicating ~50 lines. The object path (`folder + "/" + uuid + "." + ext`) is passed to `RestClient.uri()` as a literal string, not a `{template}` variable — Spring's `UriBuilderFactory` percent-encodes `/` inside a substituted template value, which would have broken the path. Selected via the existing `app.storage.provider=supabase` property (`@ConditionalOnProperty`), mutually exclusive with `LocalStorageService` (`local`, the default) so exactly one `FileStorageService` bean exists at a time — deploying with ephemeral disk on Render's free plan (see `render.yaml`) is the reason this was finally built. |
| 2026-09-05 | Added `report/service/PriorityService.java` — computes `ReportPriority` + a raw int score from `category.severityWeight` (1–5, new `categories.severity_weight` column) + a duplicate-count bonus (parameter, always `0` for now) + a report-age bonus, called from `ReportWorkflowService.approve()` right after department auto-routing; kept the existing manual `PATCH /api/reports/{id}/priority` endpoint as a post-approval override rather than removing it | Session task asked for automatic priority scoring but described it as if no manual priority-setting existed yet; Phase 5 had already shipped `updatePriority()`, documented in `api-standards.md` and covered by its own tests. Asked the user how to reconcile the two rather than guessing (rule 10 in `CLAUDE.md`): keep-as-override was chosen over deleting the endpoint/tests. See D19 in `project-overview.md`. |
| 2026-08-31 | Added `common/config/MockDataSeeder.java` (`@Order(2)`, gated by `app.mock-data.enabled`/`SEED_MOCK_DATA`, default `false`) — 9 citizens, 6 department staff, 30 reports spanning every `ReportStatus`, plus images, status history, comments, feedback, notifications, and point-ledger rows for each | User asked to populate the DB with demo data. Kept out of `DataSeeder` (which only seeds structural/reference data — roles, admin, departments, categories) rather than adding conditionals to it, so demo content stays a single, clearly-optional, clearly-fake component never at risk of running against a real deployment. Idempotent via `reportRepository.existsByReportCode("RPT-2026-000001")`; citizen/staff creation additionally skip-if-email-exists so an interrupted run can be safely re-run. Ran once against the Supabase dev DB in `backend/.env` — verified via `psql`: 30 reports (5 `PENDING_APPROVAL`, 3 `REJECTED`, 6 `ASSIGNED`, 6 `IN_PROGRESS`, 6 `RESOLVED`, 4 `CLOSED`), 16 users, 40 report images, 85 status-history rows, 22 comments, 5 feedback rows, 92 notifications, 40 point transactions, cached `users.score_points` correctly summed. |

---

## Known Issues / Blockers

| ID | Description | Impact | Owner | Status |
|----|-------------|--------|-------|--------|
| KI-01 | `/frontend` was deleted from the repo in commit `3347e8a` ("fix: delete frontend files") | Every frontend `[x]` box in Phases 1–3, 5, 6, 7, 8 above is stale — there is currently no React app to run against the backend or the new mock data | | Open |
| KI-02 | `api-standards.md` documents `415 Unsupported Media Type` for a non-image upload, but `LocalStorageService`'s validation always throws `FileStorageException`, which `GlobalExceptionHandler` maps to `400` for every case (unsupported content type, corrupted content, empty file) — there is no `415` path anywhere in the codebase | Minor — the API contract doc and the shipped behaviour disagree on one status code; not a functional bug (rejection still happens, citizen still gets a clear error), but a grader spot-checking `api-standards.md` against a live request will see `400`, not `415` | | Open — fix by either adding a dedicated content-type-mismatch branch that throws a `415`-mapped exception, or by correcting `api-standards.md`'s HTTP Status Codes table to say `400` |
| KI-03 | This machine has no Maven install and `backend/` has no `mvnw`/`mvnw.cmd` wrapper (only `.mvn/`, missing the wrapper script/jar) | `mvn test` cannot be run at all in this environment — every session working here must review changes by reading code, not by executing the test suite, until a wrapper is added or Maven is installed | | Open — regenerate the wrapper (`mvn -N wrapper:wrapper`) from a machine that has Maven, commit `mvnw`/`mvnw.cmd`/`.mvn/wrapper/`, or install Maven directly on this machine |
