# 3. SCIRS — Database Schema

## Overview

| | |
|---|---|
| Database | PostgreSQL 15+ |
| ORM | Hibernate (via Spring Data JPA) |
| Schema Management | Hibernate auto-generation from entity annotations (`ddl-auto=update` in dev) |
| Total Tables | 11 |
| Seed Data | Roles, one Admin user, 6 departments, default categories — loaded by `common/config/DataSeeder` on startup |

> **NEVER guess a field, type, or enum value. This file is the single source of truth.**

---

## Tables

### 1. `roles`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| name | VARCHAR (Enum → `RoleName`) | UNIQUE, NOT NULL |
| description | VARCHAR | |

**Enum: `RoleName`** — `ADMIN`, `STAFF`, `CITIZEN`

---

### 2. `users`

Single table for all three roles (see Decision D5 in `project-overview.md`).

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| full_name | VARCHAR | NOT NULL |
| email | VARCHAR | UNIQUE, NOT NULL |
| phone | VARCHAR | UNIQUE (nullable) |
| password_hash | VARCHAR | NOT NULL |
| role_id | BIGINT (FK) | → `roles.id`, NOT NULL |
| account_status | VARCHAR (Enum → `AccountStatus`) | NOT NULL, default `PENDING` |
| date_of_birth | DATE | Citizens only |
| nrc_number | VARCHAR | UNIQUE (nullable) — citizens only |
| department_id | BIGINT (FK) | → `departments.id` (nullable) — staff only |
| score_points | INTEGER | NOT NULL, default 0 — citizens only (cached total) |
| profile_image_url | VARCHAR | nullable |
| is_active | BOOLEAN | NOT NULL, default true |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | |

**Enum: `AccountStatus`** — `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`

**Service-layer validation rules (not enforceable by the schema):**
- `role = CITIZEN` → `date_of_birth` and `nrc_number` required, `department_id` must be null
- `role = STAFF` → `department_id` required, `nrc_number` may be null
- `role = ADMIN` → seeded only, `account_status = APPROVED`
- Admin and staff accounts are created with `account_status = APPROVED` immediately

---

### 3. `departments`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| name | VARCHAR | UNIQUE, NOT NULL |
| description | VARCHAR | |
| contact_email | VARCHAR | |
| is_active | BOOLEAN | NOT NULL, default true |

**Seeded departments:** Electricity, Roads, Water, Sanitation, Parks, Buildings

---

### 4. `categories`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| name | VARCHAR | UNIQUE, NOT NULL |
| description | VARCHAR | |
| department_id | BIGINT (FK) | → `departments.id`, NOT NULL — the **default routing target** |
| icon | VARCHAR | Icon key for map pins and filter chips |
| color_hex | VARCHAR(7) | Map pin colour |
| is_active | BOOLEAN | NOT NULL, default true |

**Seeded categories → department mapping:**

| Category | Routes to |
|----------|-----------|
| Street Lighting / Power Outage | Electricity |
| Pothole / Damaged Road | Roads |
| Water Leakage / Drainage | Water |
| Garbage / Sanitation | Sanitation |
| Park & Public Space | Parks |
| Damaged Public Building | Buildings |

---

### 5. `reports`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| report_code | VARCHAR | UNIQUE, NOT NULL — human-readable, e.g. `RPT-2026-000142` |
| title | VARCHAR | NOT NULL |
| description | TEXT | NOT NULL |
| category_id | BIGINT (FK) | → `categories.id`, NOT NULL |
| department_id | BIGINT (FK) | → `departments.id` (nullable until approved) |
| reporter_id | BIGINT (FK) | → `users.id`, NOT NULL |
| assigned_staff_id | BIGINT (FK) | → `users.id` (nullable) |
| status | VARCHAR (Enum → `ReportStatus`) | NOT NULL, default `PENDING_APPROVAL` |
| priority | VARCHAR (Enum → `ReportPriority`) | NOT NULL, default `NORMAL` |
| latitude | DECIMAL(10,7) | NOT NULL |
| longitude | DECIMAL(10,7) | NOT NULL |
| address_text | VARCHAR | Reverse-geocoded or typed by the citizen |
| rejection_reason | VARCHAR | Required when status = `REJECTED` |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | |
| approved_at | TIMESTAMP | |
| approved_by | BIGINT (FK) | → `users.id` (nullable) |
| resolved_at | TIMESTAMP | |
| closed_at | TIMESTAMP | |

**Enum: `ReportStatus`** — `PENDING_APPROVAL`, `REJECTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`

**Enum: `ReportPriority`** — `LOW`, `NORMAL`, `HIGH`, `URGENT`

**Allowed status transitions (enforced in `ReportWorkflowService`):**

```
PENDING_APPROVAL → ASSIGNED        (admin approves; auto-routes to department)
PENDING_APPROVAL → REJECTED        (admin denies; reason required)
ASSIGNED         → IN_PROGRESS
ASSIGNED         → RESOLVED        (allowed shortcut for trivial fixes)
IN_PROGRESS      → RESOLVED        (completion photo required)
RESOLVED         → CLOSED
RESOLVED         → IN_PROGRESS     (re-open if the citizen reports it is not fixed)
REJECTED         → (terminal)
CLOSED           → (terminal)
```

Any other transition throws `InvalidStatusTransitionException` (HTTP 409).

**Indexes:** `status`, `category_id`, `department_id`, `reporter_id`, `created_at`, and a composite `(latitude, longitude)` for map bounding-box queries.

---

### 6. `report_images`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| report_id | BIGINT (FK) | → `reports.id`, NOT NULL |
| image_url | VARCHAR | NOT NULL |
| image_type | VARCHAR (Enum → `ImageType`) | NOT NULL |
| uploaded_by | BIGINT (FK) | → `users.id`, NOT NULL |
| uploaded_at | TIMESTAMP | NOT NULL |

**Enum: `ImageType`** — `REPORT_PHOTO` (uploaded by the citizen), `RESOLUTION_PHOTO` (uploaded by staff as completion evidence)

Cascade: `Report → ReportImage` is `CascadeType.ALL, orphanRemoval = true`.

---

### 7. `report_status_history`

Immutable audit trail. Never updated, never deleted.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| report_id | BIGINT (FK) | → `reports.id`, NOT NULL |
| old_status | VARCHAR (Enum → `ReportStatus`) | nullable (null on creation) |
| new_status | VARCHAR (Enum → `ReportStatus`) | NOT NULL |
| changed_by | BIGINT (FK) | → `users.id`, NOT NULL |
| remarks | VARCHAR | Optional note from the staff member |
| changed_at | TIMESTAMP | NOT NULL |

---

### 8. `report_comments`

Internal notes for the handling department (features file: "add comments (for the department if needed)"). Not visible to citizens.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| report_id | BIGINT (FK) | → `reports.id`, NOT NULL |
| author_id | BIGINT (FK) | → `users.id`, NOT NULL |
| body | TEXT | NOT NULL |
| mentioned_department_id | BIGINT (FK) | → `departments.id` (nullable) — triggers a "department mention" notification |
| created_at | TIMESTAMP | NOT NULL |

---

### 9. `feedback`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| report_id | BIGINT (FK) | → `reports.id`, **UNIQUE**, NOT NULL (one feedback per report) |
| citizen_id | BIGINT (FK) | → `users.id`, NOT NULL |
| rating | INTEGER | NOT NULL, 1–5 |
| comment | TEXT | |
| created_at | TIMESTAMP | NOT NULL |

Service rules: `citizen_id` must equal `report.reporter_id`; report status must be `RESOLVED` or `CLOSED`.

---

### 10. `notifications`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| recipient_id | BIGINT (FK) | → `users.id`, NOT NULL |
| report_id | BIGINT (FK) | → `reports.id` (nullable) |
| type | VARCHAR (Enum → `NotificationType`) | NOT NULL |
| title | VARCHAR | NOT NULL |
| message | VARCHAR | NOT NULL |
| is_read | BOOLEAN | NOT NULL, default false |
| created_at | TIMESTAMP | NOT NULL |

**Enum: `NotificationType`** — `NEW_REPORT`, `URGENT_REPORT`, `REPORT_WAITING_TOO_LONG`, `DEPARTMENT_MENTION`, `STATUS_CHANGED`, `REPORT_APPROVED`, `REPORT_REJECTED`, `REPORT_COMPLETED`, `ACCOUNT_APPROVED`, `ACCOUNT_REJECTED`

Recipient rules: citizens receive status/account notifications about their own records; staff receive `NEW_REPORT`, `URGENT_REPORT`, `REPORT_WAITING_TOO_LONG`, and `DEPARTMENT_MENTION` for their own department; admins receive new-report and new-account notifications.

---

### 11. `point_transactions`

Immutable ledger backing the leaderboard.

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| user_id | BIGINT (FK) | → `users.id`, NOT NULL |
| report_id | BIGINT (FK) | → `reports.id` (nullable) |
| points | INTEGER | NOT NULL (may be negative) |
| reason | VARCHAR (Enum → `PointReason`) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

**Enum: `PointReason`** — `REPORT_APPROVED` (+10), `REPORT_RESOLVED` (+20), `FEEDBACK_GIVEN` (+5), `REPORT_REJECTED` (−5)

Rule: a `(user_id, report_id, reason)` triple must be unique — never award the same event twice.

---

## Entity Relationship Diagram

```
┌────────┐        ┌──────────────┐        ┌──────────────┐
│ Role   │──1:N──▶│    Users     │◀──N:1──│  Department  │
└────────┘        └──────┬───────┘        └──────┬───────┘
                         │                       │ 1:N
       ┌─────────────────┼───────────────┐       ▼
       │ 1:N             │ 1:N           │  ┌──────────┐
       ▼                 ▼               │  │ Category │
┌─────────────┐   ┌───────────────┐      │  └────┬─────┘
│Notification │   │PointTransaction│     │       │ 1:N
└─────────────┘   └───────────────┘      │       ▼
                                         │  ┌──────────┐
       (reporter) 1:N ───────────────────┴─▶│  Report  │
                                            └────┬─────┘
                 ┌───────────────┬──────────────┼──────────────┐
                 │ 1:N           │ 1:N          │ 1:N          │ 1:1
                 ▼               ▼              ▼              ▼
        ┌──────────────┐ ┌──────────────┐ ┌───────────┐ ┌──────────┐
        │ ReportImage  │ │StatusHistory │ │  Comment  │ │ Feedback │
        └──────────────┘ └──────────────┘ └───────────┘ └──────────┘
```

## Relationships Summary

| Relationship | Type | Cascade / Notes |
|--------------|------|-----------------|
| User → Role | N:1 | Each user has exactly one role |
| User → Department | N:1 | Staff only; null for citizens and admins |
| Department → Category | 1:N | Default routing target for each category |
| Category → Report | 1:N | Determines auto-assignment on approval |
| Department → Report | 1:N | Set on approval, reassignable by admin |
| User (reporter) → Report | 1:N | A citizen's report history |
| User (assigned staff) → Report | 1:N | Optional per-staff assignment |
| Report → ReportImage | 1:N | Cascade ALL, orphanRemoval = true |
| Report → ReportStatusHistory | 1:N | Cascade ALL; append-only |
| Report → ReportComment | 1:N | Cascade ALL |
| Report → Feedback | 1:1 | Unique constraint on report_id |
| User → Notification | 1:N | Cascade on user delete |
| User → PointTransaction | 1:N | Append-only ledger |

## Repository Layer

| Repository | Custom Query Methods |
|------------|----------------------|
| `RoleRepository` | `findByName(RoleName)` |
| `UserRepository` | `findByEmail()`, `existsByEmail()`, `existsByPhone()`, `existsByNrcNumber()`, `findByAccountStatus()`, `findByRoleName()`, `findByDepartmentId()`, `findTop10ByRoleNameOrderByCreatedAtDesc()`, `countByAccountStatus()` |
| `DepartmentRepository` | `findByName()`, `findByActiveTrue()` |
| `CategoryRepository` | `findByName()`, `findByActiveTrue()`, `findByDepartmentId()` |
| `ReportRepository` | `findByReporterId()`, `findByStatus()`, `findByDepartmentId()`, `findByDepartmentIdAndStatus()`, `findByCategoryId()`, `findByCreatedAtBetween()`, `findByReportCode()`, `countByStatus()`, `countByDepartmentIdAndStatus()`, `findByStatusAndCreatedAtBefore()` (waiting-too-long alerts), `findByLatitudeBetweenAndLongitudeBetween()` (map bounding box) |
| `ReportImageRepository` | `findByReportId()`, `findByReportIdAndImageType()` |
| `ReportStatusHistoryRepository` | `findByReportIdOrderByChangedAtAsc()` |
| `ReportCommentRepository` | `findByReportIdOrderByCreatedAtAsc()` |
| `FeedbackRepository` | `findByReportId()`, `existsByReportId()`, `findByCitizenId()` |
| `NotificationRepository` | `findByRecipientIdOrderByCreatedAtDesc()`, `countByRecipientIdAndIsReadFalse()`, `findByRecipientIdAndIsReadFalse()` |
| `PointTransactionRepository` | `findByUserIdOrderByCreatedAtDesc()`, `existsByUserIdAndReportIdAndReason()`, `sumPointsByUserId()` (`@Query`) |

Aggregate dashboard queries (issue volume by category, average resolution time, department performance) live in `ReportRepository` as `@Query` projections returning interface-based projections or DTOs — never entity lists that the service then loops over.

## Enum Reference (Quick Lookup)

| Enum | Values |
|------|--------|
| `RoleName` | `ADMIN`, `STAFF`, `CITIZEN` |
| `AccountStatus` | `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED` |
| `ReportStatus` | `PENDING_APPROVAL`, `REJECTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `ReportPriority` | `LOW`, `NORMAL`, `HIGH`, `URGENT` |
| `ImageType` | `REPORT_PHOTO`, `RESOLUTION_PHOTO` |
| `NotificationType` | `NEW_REPORT`, `URGENT_REPORT`, `REPORT_WAITING_TOO_LONG`, `DEPARTMENT_MENTION`, `STATUS_CHANGED`, `REPORT_APPROVED`, `REPORT_REJECTED`, `REPORT_COMPLETED`, `ACCOUNT_APPROVED`, `ACCOUNT_REJECTED` |
| `PointReason` | `REPORT_APPROVED`, `REPORT_RESOLVED`, `FEEDBACK_GIVEN`, `REPORT_REJECTED` |

All enums are persisted with `@Enumerated(EnumType.STRING)` — never `ORDINAL`.

## Data Integrity Notes

1. Deleting a user is a **soft delete** (`is_active = false`) — historical reports must survive.
2. Deleting a category or department is a soft delete (`is_active = false`) — existing reports keep their FK.
3. `report_status_history` and `point_transactions` are append-only. No update or delete endpoints exist for them.
4. Money-free schema — no decimal precision issues, but coordinates must stay `DECIMAL(10,7)` (≈1 cm precision) and never `FLOAT`.
5. All timestamps are stored in UTC; the frontend formats to local time.
