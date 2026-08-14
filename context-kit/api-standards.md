# 5. SCIRS — API Standards

## General API Design Rules

| Rule | Details |
|------|---------|
| Protocol | REST over HTTP |
| Data format | JSON request and response bodies (except image upload → `multipart/form-data`) |
| Base path | `/api/` |
| Authentication | JWT Bearer token in the `Authorization` header |
| Content-Type | `application/json` |
| Naming | Plural nouns, kebab-case for multi-word resources |
| Time format | ISO-8601 UTC (`2026-08-13T10:30:00Z`) |
| Enums | Sent and received as their exact uppercase string name |

---

## Standard CRUD Endpoint Pattern

| Operation | Method | URL | Request Body | Response | Status |
|-----------|--------|-----|--------------|----------|--------|
| Get all | GET | `/api/{resource}` | — | `List<ResourceDTO>` | 200 |
| Get by ID | GET | `/api/{resource}/{id}` | — | `ResourceDTO` | 200 |
| Create | POST | `/api/{resource}` | `CreateResourceDTO` | `ResourceDTO` | 201 |
| Update | PUT | `/api/{resource}/{id}` | `UpdateResourceDTO` | `ResourceDTO` | 200 |
| Delete | DELETE | `/api/{resource}/{id}` | — | — | 204 |

---

## Module Endpoint Map

| Module | Base Path | Notes |
|--------|-----------|-------|
| Auth | `/api/auth` | `login` (public), `register` (public, citizens only), `me` |
| Users | `/api/users` | Admin-managed accounts, approval queue, staff creation |
| Departments | `/api/departments` | Standard CRUD + workload stats |
| Categories | `/api/categories` | Standard CRUD; each carries a default department |
| Reports | `/api/reports` | CRUD + approval + assignment + status transitions + images + comments |
| Map | `/api/reports/map` | Lightweight pin payload for the map view |
| Feedback | `/api/feedback` | One per resolved report |
| Notifications | `/api/notifications` | List, unread count, mark read |
| Leaderboard | `/api/leaderboard` | Ranked citizens |
| Score | `/api/score` | Own point total and history |
| Dashboard | `/api/dashboard` | Read-only aggregates for admin and staff |

---

## Authentication Endpoints

### `POST /api/auth/register` — citizen sign-up (public)

```json
{
  "fullName": "Aung Aung",
  "email": "aung@example.com",
  "phone": "+959123456789",
  "password": "securePass123",
  "dateOfBirth": "2002-05-14",
  "nrcNumber": "12/ABC(N)123456"
}
```

Response `201`:
```json
{
  "id": 42,
  "fullName": "Aung Aung",
  "email": "aung@example.com",
  "role": "CITIZEN",
  "accountStatus": "PENDING",
  "message": "Your account is pending admin approval."
}
```

> Only citizens may register. The endpoint always forces `role = CITIZEN` and `accountStatus = PENDING`. A `role` field in the request body is ignored.

### `POST /api/auth/login` — all roles (public)

```json
{ "email": "admin@scirs.gov", "password": "password123" }
```

Response `200`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "fullName": "System Administrator",
  "email": "admin@scirs.gov",
  "role": "ADMIN",
  "departmentId": null,
  "accountStatus": "APPROVED"
}
```

Failure modes: bad credentials → `401`; `accountStatus = PENDING` → `403` with "Your account is awaiting admin approval"; `REJECTED`/`SUSPENDED` → `403`.

### `GET /api/auth/me`
Returns the current user's profile from the JWT. Used by the frontend on page refresh.

---

## Departments & Categories Endpoints

Both follow the standard CRUD pattern above at `/api/departments` and `/api/categories`.

| Endpoint | Method | Role | Notes |
|----------|--------|------|-------|
| `/api/departments`, `/api/categories` | GET | ADMIN, STAFF, CITIZEN | List all (active and inactive) |
| `/api/departments/{id}`, `/api/categories/{id}` | GET | ADMIN, STAFF, CITIZEN | |
| `/api/departments`, `/api/categories` | POST | ADMIN | `CreateDepartmentDTO` / `CreateCategoryDTO` |
| `/api/departments/{id}`, `/api/categories/{id}` | PUT | ADMIN | `UpdateDepartmentDTO` / `UpdateCategoryDTO` |
| `/api/departments/{id}`, `/api/categories/{id}` | DELETE | ADMIN | Soft delete (`isActive = false`); returns 204 |

Business rules:
- Department and category names must be unique → duplicate name returns `409 Conflict`.
- `CreateCategoryDTO`/`UpdateCategoryDTO.departmentId` must reference an existing, **active** department — an unknown id returns `404`, an inactive department returns `400` (`BusinessRuleException`).
- Deleting (soft-deleting) a department does not cascade to its categories; those categories keep their `departmentId` but a new category can no longer be created against that department until it is reactivated.

---

## User Management Endpoints

| Endpoint | Method | Role | Purpose |
|----------|--------|------|---------|
| `/api/users` | GET | ADMIN | All users; filter by `role`, `accountStatus` |
| `/api/users/citizens` | GET | ADMIN | Citizen account table |
| `/api/users/staff` | GET | ADMIN | Staff account table |
| `/api/users/staff` | POST | ADMIN | Create a staff account (`CreateStaffDTO`) |
| `/api/users/pending` | GET | ADMIN | Citizen approval queue |
| `/api/users/{id}/approve` | PATCH | ADMIN | `accountStatus → APPROVED`, notify citizen |
| `/api/users/{id}/reject` | PATCH | ADMIN | `accountStatus → REJECTED` + reason |
| `/api/users/{id}/suspend` | PATCH | ADMIN | `accountStatus → SUSPENDED` |
| `/api/users/{id}` | GET / PUT | ADMIN, self | View / update profile |
| `/api/users/{id}` | DELETE | ADMIN | Soft delete (`isActive = false`) |

---

## Report Endpoints

| Endpoint | Method | Role | Purpose |
|----------|--------|------|---------|
| `/api/reports` | POST | CITIZEN | Submit a report (`multipart/form-data`: `data` JSON part + `images` file parts) |
| `/api/reports` | GET | ADMIN, STAFF | All reports; staff are scoped to their department |
| `/api/reports/my` | GET | CITIZEN | Own report history |
| `/api/reports/pending` | GET | ADMIN | Report approval queue |
| `/api/reports/{id}` | GET | all (ownership-checked) | Report detail |
| `/api/reports/{id}/approve` | PATCH | ADMIN | Approve → auto-route → `ASSIGNED` |
| `/api/reports/{id}/reject` | PATCH | ADMIN | Deny with `rejectionReason` |
| `/api/reports/{id}/status` | PATCH | STAFF, ADMIN | Change status (`UpdateReportStatusDTO`) |
| `/api/reports/{id}/assign` | PATCH | ADMIN | Reassign department and/or staff member |
| `/api/reports/{id}/priority` | PATCH | STAFF, ADMIN | Change priority |
| `/api/reports/{id}/images` | POST | STAFF | Upload completion photo(s) |
| `/api/reports/{id}/history` | GET | ADMIN, STAFF, owner | Status timeline |
| `/api/reports/{id}/comments` | GET / POST | ADMIN, STAFF | Internal department notes |
| `/api/reports/map` | GET | all | Map pins (see below) |

### Status change request

`PATCH /api/reports/12/status`
```json
{ "status": "IN_PROGRESS", "remarks": "Crew scheduled for tomorrow morning." }
```

Rejected transitions return `409 Conflict`:
```json
{
  "message": "Cannot change status from CLOSED to IN_PROGRESS",
  "timestamp": "2026-08-13T10:30:00Z",
  "status": 409
}
```

### Map endpoint

`GET /api/reports/map?categoryId=3&status=ASSIGNED&minLat=&maxLat=&minLng=&maxLng=`

Returns a **slim** payload — never the full report DTO:
```json
[
  {
    "id": 12,
    "reportCode": "RPT-2026-000012",
    "latitude": 16.8409,
    "longitude": 96.1735,
    "categoryName": "Pothole / Damaged Road",
    "categoryColor": "#F97316",
    "status": "ASSIGNED",
    "priority": "HIGH",
    "createdAt": "2026-08-10T04:12:00Z"
  }
]
```

Citizens only see reports with a status other than `PENDING_APPROVAL` and `REJECTED`.

---

## Dashboard, Leaderboard and Notification Endpoints

| Endpoint | Method | Role | Returns |
|----------|--------|------|---------|
| `/api/dashboard/admin` | GET | ADMIN | Pending account count, pending report count, 10 latest registrations, latest reports awaiting approval |
| `/api/dashboard/staff` | GET | STAFF | Total / resolved / remaining / new report counts, monthly series per department, 10 most recent reports |
| `/api/dashboard/departments` | GET | ADMIN, STAFF | Workload and performance per department (open count, resolved count, average resolution hours, average rating) |
| `/api/dashboard/categories` | GET | ADMIN, STAFF | Issue volume by category |
| `/api/leaderboard` | GET | all | `[{ rank, userId, fullName, scorePoints }]`, default top 50 |
| `/api/score/me` | GET | CITIZEN | Own total plus point history |
| `/api/notifications` | GET | all | Own notifications, newest first |
| `/api/notifications/unread-count` | GET | all | `{ "count": 3 }` |
| `/api/notifications/{id}/read` | PATCH | owner | Mark one as read |
| `/api/notifications/read-all` | PATCH | owner | Mark all as read |

---

## Request Validation

Validation runs before business logic via Jakarta annotations on request DTOs.

| Annotation | Usage |
|------------|-------|
| `@NotBlank` | Required strings |
| `@NotNull` | Required non-strings |
| `@Email` | Email format |
| `@Size(min, max)` | Length constraints |
| `@Min` / `@Max` | Numeric ranges (e.g. rating 1–5) |
| `@DecimalMin` / `@DecimalMax` | Latitude / longitude bounds |
| `@Past` | Date of birth |
| `@Pattern` | NRC format, phone format |

---

## Response Formats

**Single resource** — flat JSON object.

**List** — a JSON array, or a paged envelope when pagination is used:
```json
{
  "content": [ /* items */ ],
  "page": 0,
  "size": 10,
  "totalElements": 137,
  "totalPages": 14
}
```

**Error**
```json
{
  "message": "Report not found with id: 99",
  "timestamp": "2026-08-13T10:30:00Z",
  "status": 404
}
```

**Validation error**
```json
{
  "message": "Validation failed",
  "timestamp": "2026-08-13T10:30:00Z",
  "status": 400,
  "errors": {
    "title": "Title is required",
    "latitude": "Latitude is required"
  }
}
```

---

## HTTP Status Codes

| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST that creates a resource |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation errors, broken business rules |
| 401 | Unauthorized | Missing or invalid JWT |
| 403 | Forbidden | Valid JWT but insufficient role, not the owner, or account not approved |
| 404 | Not Found | Resource id does not exist |
| 409 | Conflict | Duplicate email/NRC, duplicate feedback, invalid status transition |
| 413 | Payload Too Large | Image over 5 MB |
| 415 | Unsupported Media Type | Non-image upload |
| 500 | Internal Server Error | Unexpected server-side error |

---

## Role-Based Access Matrix

| Endpoint Category | ADMIN | STAFF | CITIZEN |
|-------------------|:-----:|:-----:|:-------:|
| Citizen registration | — | — | ✅ (public) |
| Staff account creation | ✅ | ❌ | ❌ |
| Citizen account approval | ✅ | ❌ | ❌ |
| Report approval / denial | ✅ | ❌ | ❌ |
| Submit report | ❌ | ❌ | ✅ |
| View all reports | ✅ | Own department | Own reports only |
| Change report status | ✅ | Own department | ❌ |
| Reassign department | ✅ | ❌ | ❌ |
| Upload completion photo | ✅ | Own department | ❌ |
| Internal comments | ✅ | Own department | ❌ |
| Map pins | ✅ | ✅ | ✅ (approved reports only) |
| Departments / categories CRUD | ✅ | Read only | Read only |
| Leaderboard | ✅ | ✅ | ✅ |
| Feedback | Read | Read | Own resolved reports |
| Admin dashboard | ✅ | ❌ | ❌ |
| Staff dashboard | ✅ | ✅ | ❌ |

"Own department" means a staff member can only access reports whose `department_id` matches the `departmentId` claim in their JWT.

---

## Query Parameters for Filtering

```
GET /api/reports?status=ASSIGNED&categoryId=3&departmentId=2&page=0&size=10
GET /api/reports?search=streetlight
GET /api/reports?startDate=2026-08-01&endDate=2026-08-31
GET /api/reports/map?status=IN_PROGRESS&categoryId=1
GET /api/users?role=CITIZEN&accountStatus=PENDING
GET /api/leaderboard?limit=20
GET /api/notifications?unreadOnly=true
```

**Filtering rules**
- Use query parameters for filtering, never path parameters
- Enum filters use the exact enum value (`ASSIGNED`, `PENDING`)
- Date filters use ISO `YYYY-MM-DD`
- Multiple filters combine with AND
- `search` performs a case-insensitive match on title, description, and report code
- List endpoints that can exceed 50 rows must support `page` and `size` (default `size=10`)
