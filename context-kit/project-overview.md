# 1. SCIRS — Project Overview

## Product Identity

| Field | Value |
|-------|-------|
| Project Name | Smart Community Issue Report System (SCIRS) |
| Short Name | Community Issues Reporting System |
| Type | Full-stack enterprise web application |
| Purpose | Let citizens report civic issues and let local authorities triage, assign, and resolve them |
| Context | CST-4105 J2EE Keystone Project — University of Information Technology, 2025–2026 |
| Team | Section-C, Group-II (Leader: Htoo Myat Min Eain) |
| Duration | 8 weeks (see `build-plan.md`) |
| SDG Alignment | UN SDG 11 — Sustainable Cities and Communities |

## Problem Statement

Communities and municipal offices face everyday civic problems — potholes, damaged streetlights, overflowing garbage bins, broken drainage, water leakages. Today these are reported informally through phone calls, walk-ins, or scattered social media posts. That creates six concrete problems the system must solve:

1. There is no single dedicated channel for citizens to report community issues.
2. Verbal or informal reports are rarely documented, so nobody can tell whether an issue was actually addressed.
3. Citizens cannot check the status or progress of an issue they reported.
4. Reports are not automatically routed to the correct department, so staff must manually redirect misfiled complaints.
5. Authorities lack consolidated data on recurring problem areas, making preventive maintenance and resource allocation guesswork.
6. Duplicate reports of the same issue cause redundant site visits and wasted staff effort.

## Product Vision

SCIRS is a centralized, web-based platform that demonstrates core Java Enterprise concepts in one cohesive application:

- Authentication and role-based access control
- CRUD operations across multiple related modules
- Workflow / state-machine management (report lifecycle)
- Automatic department routing with administrator override
- Notifications
- Analytics dashboards and reporting
- Geospatial data capture and map visualisation
- Gamification (citizen score points and leaderboard)

## Actors and Access

| Actor | Account Origin | Primary Device | Access Level |
|-------|----------------|----------------|--------------|
| **Admin** | **Seeded directly into the database** — no self-registration, no sign-up screen | Desktop | Everything: users, staff accounts, approvals, departments, categories, system-wide analytics |
| **Government Staff** | **Created by an Admin** from the Admin panel ("Create New Staff") — no self-registration | Desktop / PC | Reports assigned to their department, status updates, department map view, department analytics |
| **Citizen** | **Self sign-up**, then waits for Admin approval | Mobile (mobile-first UI) | Submit reports, track own reports, community map, leaderboard, own score, own profile |

> **Critical RBAC rule:** login/sign-up as a public flow exists for **citizens only**. Admin and Staff accounts are provisioned, never self-registered. Every login endpoint is shared, but only citizens can hit `/api/auth/register`.

## Actor Feature Map

### Admin (desktop)
- **Dashboard** — pending account approvals, 10 most recently registered users, recent reports awaiting approve/deny
- **Citizen Accounts** — table of all citizen accounts
- **Staff Accounts** — table of all government staff accounts + create new staff
- **Citizen Account Approval Queue** — pending / approved lists
- **Report Approval Queue** — pending reports to approve or deny
- **Master data** — departments, categories, roles and permissions

### Citizen (mobile-focused)
- **Home** — personal summary: own recent reports, own score, quick "Report" action
- **Map** — all approved reports as pins, filterable (All, Electricity, Roads/Buildings, Water, Sanitation, Parks)
- **Leaderboard** — citizens ranked by score points
- **My Score** — own point total and point history
- **Report** — submit new report: GPS location, category, problem statement (pick an option or type it in), image, upload button; plus own report history with date/time
- **Profile** — view and edit own account data

### Government Staff (PC-focused)
- **Dashboard** — map, total reports, total resolved, total remaining, total new reports, monthly graphs per department, recent reports table (5–10 rows)
- **Reports** — all reports, filter by status/type, search box, per-row "view details", assign to department, change status, add comments for the department, upload completion photos
- **Map View** — full-screen, full filter set (category/status), clustering of nearby reports
- **Departments** — Electricity, Roads, Water, Sanitation, Parks, Buildings; workload per department (bar/pie charts) and performance statistics
- **Settings** — user management, roles and permissions, categories, departments, profile
- **Notifications** — new reports, urgent reports, reports waiting too long, department mentions, completed reports

## Sign-up / Login Fields

| Flow | Fields |
|------|--------|
| Citizen sign-up | Full name, email **or** phone, password, date of birth, NRC number → **Create Account** (lands in PENDING approval state) |
| Login (all roles) | Email, password → **Log In** |

## Core Business Modules

| Module | Primary Responsibility |
|--------|------------------------|
| Auth | Login, JWT issuance, citizen registration, role-based authorization |
| User | Citizen and staff account CRUD, approval queue, activation/suspension |
| Department | Department CRUD, workload and performance statistics |
| Category | Issue category CRUD, default department routing rule |
| Report | Report submission, approval, assignment, status workflow, images, comments |
| Notification | Event-driven notifications to citizens and staff |
| Feedback | Post-resolution rating and comment from the reporting citizen |
| Score | Point awards, point history, leaderboard ranking |
| Dashboard | Aggregated analytics for admin and staff dashboards |

## Business Domain Relationships

```
Citizen (User) ──submits──→ Report
Report ──belongs to──→ Category
Category ──routes to──→ Department (default routing rule)
Report ──assigned to──→ Department  (auto from category, admin can override)
Report ──assigned to──→ Staff (User) (optional, within the department)
Report ──has many──→ Report Images (report photos + resolution photos)
Report ──has many──→ Status History entries
Report ──has one──→ Feedback (after resolution)
Staff (User) ──belongs to──→ Department
User ──has many──→ Notifications
User ──has many──→ Point Transactions ──roll up to──→ Leaderboard
```

## Key Business Rules

1. **Admins are seeded**, staff are **admin-created**, citizens **self-register**. Never build a sign-up screen for admin or staff.
2. A newly registered citizen has `AccountStatus = PENDING` and **cannot submit reports** until an admin approves the account.
3. A newly submitted report has `ReportStatus = PENDING_APPROVAL` and is **not shown on the public map** until approved.
4. On approval, a report is **automatically assigned** to the department mapped to its category. An admin/staff member may reassign it manually.
5. Report status transitions are validated by a state machine — see `database-schema.md` § ReportStatus. A `CLOSED` or `REJECTED` report cannot move backwards.
6. Every status change **must** write a `report_status_history` row and **must** create a notification for the reporting citizen.
7. Only the citizen who created a report may leave feedback on it, and only once, and only after it is `RESOLVED` or `CLOSED`.
8. Score points are awarded by the system, never entered by hand (see § Scoring Rules).
9. Citizens may only read and write their **own** reports, profile, score, and feedback. Staff see reports for **their department**. Admins see everything.
10. All business logic lives in the Service layer — never in Controllers or Repositories.

## Scoring Rules (Gamification)

Points are awarded automatically through `PointTransaction` rows so the leaderboard is always reconstructable.

| Event | Points | Reason enum |
|-------|--------|-------------|
| Report approved by admin | +10 | `REPORT_APPROVED` |
| Report resolved by staff | +20 | `REPORT_RESOLVED` |
| Feedback submitted after resolution | +5 | `FEEDBACK_GIVEN` |
| Report denied as invalid/spam | −5 | `REPORT_REJECTED` |

Leaderboard = sum of point transactions per citizen, ranked descending, ties broken by earliest join date. A citizen's total is cached on `users.score_points` and recalculated on every point transaction.

## Non-Functional Requirements

| Area | Requirement |
|------|-------------|
| Performance | Common operations (report submission, search) respond in under 5 seconds |
| Security | Passwords hashed (BCrypt); role-based authorization enforced on **every** module; uploaded files validated (type + size) before storage |
| Usability | Simple and intuitive for citizens of varying technical ability; responsive across desktop and mobile browsers |
| Reliability | Regular database backups; stable under concurrent use by multiple citizens and staff |
| Scalability | Modular layered architecture — new departments, categories, or features added without major redesign |
| Maintainability | Clear layered structure (Controller → Service → Repository), documented for future maintenance |

## Out of Scope (Explicitly Excluded)

Do not build these. If asked, flag them as out of scope and point here.

- Native mobile app (the citizen UI is a **mobile-responsive web app**, not a native app)
- AI features: automatic image classification, duplicate-issue detection, chatbot assistance
- Payment or fine-processing subsystems
- SMS notifications — **email and in-app notifications only** in this phase

## Evaluation Rubric (What Is Graded)

| Criterion | Weight | What the agent should optimise for |
|-----------|--------|------------------------------------|
| Requirements Analysis & Design | 20% | Diagrams and documentation matching the implementation exactly |
| Technical Implementation | 30% | Clean, efficient, well-structured code that demonstrates J2EE understanding |
| Database Integration | 15% | Sound schema design, efficient queries, no data-integrity holes |
| User Interface (UI) Design | 10% | Intuitive, responsive, visually consistent |
| Testing & Debugging | 10% | Comprehensive test cases, bugs identified and resolved |
| Presentation | 15% | Demo script and materials showing deep understanding |

Implication for agents: **testing and documentation are graded deliverables, not chores.** Do not skip them to ship features faster.

## Project Philosophy

- Clean architecture over quick implementation
- Enterprise coding practices over tutorial shortcuts
- Modular package organisation over monolithic structure
- Consistent API design over ad-hoc endpoints
- Strong separation of concerns at every layer
- Every feature traceable to the proposal or the features file

## Decisions Log

Conflicts between source documents and how they were resolved. Append new decisions here.

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Report lifecycle is `PENDING_APPROVAL → APPROVED/REJECTED → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED` | The proposal's workflow starts at "Submitted"; the features file adds an admin report-approval queue. Merged into one state machine. |
| D2 | Citizen accounts require admin approval before use | Required by the features file's "citizen account approval queue"; the proposal did not mention it. |
| D3 | Frontend is React + TypeScript + Tailwind + shadcn/ui (not JSP/Servlet) | Proposal § 7 Development Tools. The course template lists JSP/Servlet as the default; the team's approved proposal supersedes it. Layered MVC separation is still demonstrated on the backend. |
| D4 | Database is PostgreSQL; storage of images via Supabase Storage with a local-filesystem fallback | Proposal § 7. A local fallback keeps the app demoable without network access. |
| D5 | Single `users` table for all three roles, with nullable `department_id` (staff) and `nrc_number`/`date_of_birth` (citizens) | Keeps auth simple and avoids table-per-role joins on every request. Role-specific fields are validated at the service layer. |
| D6 | Leaderboard is backed by an immutable `point_transactions` ledger, with a cached total on `users` | Auditable, reconstructable, and demo-friendly. |
| D7 | `User.departmentId` is a plain FK column (not a `@ManyToOne`) until Phase 2 builds the `Department` entity | Can't compile a relation against a type that doesn't exist yet; convert once it does. |
| D8 | Login failure on a non-`APPROVED` account returns 403 (`AccountNotApprovedException`), overriding `code-standards.md`'s generic example that maps that exception to 400 | `api-standards.md`'s Auth Endpoints section explicitly documents 403 for `PENDING`/`REJECTED`/`SUSPENDED` on login; that's the more specific, authoritative source for this endpoint. |
