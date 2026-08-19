# SCIRS — Manual Test Cases

Maintained per `testing-standards.md`. Automated coverage for the same
scenarios lives in `backend/src/test/java/.../auth/`, `.../department/`,
`.../category/`, `.../report/`, and `.../user/`.

| ID | Module | Scenario | Steps | Expected | Actual | Pass/Fail | Date |
|----|--------|----------|-------|----------|--------|-----------|------|
| AUTH-01 | Auth | Citizen registration creates a PENDING account | POST `/api/auth/register` with valid citizen data | 201, `role=CITIZEN`, `accountStatus=PENDING` | As expected | Pass | 2026-08-13 |
| AUTH-02 | Auth | Duplicate email on registration | Register twice with the same email | 409 `DuplicateResourceException` | As expected | Pass | 2026-08-13 |
| AUTH-03 | Auth | Duplicate NRC on registration | Register twice with the same NRC number | 409 `DuplicateResourceException` | As expected | Pass | 2026-08-13 |
| AUTH-04 | Auth | Login with PENDING account | Register a citizen, then log in immediately | 403 "Your account is awaiting admin approval." | As expected | Pass | 2026-08-13 |
| AUTH-05 | Auth | Login with wrong password | Log in as the seeded admin with an incorrect password | 401 "Invalid email or password" | As expected | Pass | 2026-08-13 |
| AUTH-06 | Auth | Login with correct credentials | Log in as the seeded admin | 200, JWT token + `role=ADMIN` in response | As expected | Pass | 2026-08-13 |
| AUTH-07 | Auth | Protected endpoint without a token | GET `/api/auth/me` with no `Authorization` header | 401 | As expected | Pass | 2026-08-13 |
| AUTH-08 | Auth | Protected endpoint with a valid token | GET `/api/auth/me` with a valid admin JWT | 200, profile matches the seeded admin | As expected | Pass | 2026-08-13 |
| AUTH-09 | Auth | Role gate enforcement | Call an ADMIN-only route with a CITIZEN JWT vs. an ADMIN JWT | 403 for CITIZEN, 200 for ADMIN | As expected | Pass | 2026-08-13 |
| DEPT-01 | Department | List departments (any role) | GET `/api/departments` with a CITIZEN JWT | 200, seeded departments included | As expected | Pass | 2026-08-14 |
| DEPT-02 | Department | Unauthenticated list | GET `/api/departments` with no token | 401 | As expected | Pass | 2026-08-14 |
| DEPT-03 | Department | Create department (ADMIN) | POST `/api/departments` with a valid body and ADMIN JWT | 201, `active=true` | As expected | Pass | 2026-08-14 |
| DEPT-04 | Department | Create department (CITIZEN/STAFF) | POST `/api/departments` with a CITIZEN or STAFF JWT | 403 | As expected | Pass | 2026-08-14 |
| DEPT-05 | Department | Duplicate department name | POST `/api/departments` with a name that already exists | 409 `DuplicateResourceException` | As expected | Pass | 2026-08-14 |
| DEPT-06 | Department | Soft delete | DELETE `/api/departments/{id}` as ADMIN, then GET the same id | 204 on delete; GET returns 200 with `active=false` | As expected | Pass | 2026-08-14 |
| CAT-01 | Category | List categories (any role) | GET `/api/categories` with a STAFF JWT | 200, seeded categories included | As expected | Pass | 2026-08-14 |
| CAT-02 | Category | Create category with active department | POST `/api/categories` referencing an active department, ADMIN JWT | 201, `departmentId` matches | As expected | Pass | 2026-08-14 |
| CAT-03 | Category | Create category with inactive department | Soft-delete a department, then POST a category against it | 400 `BusinessRuleException` | As expected | Pass | 2026-08-14 |
| CAT-04 | Category | Create category with unknown department | POST `/api/categories` with a non-existent `departmentId` | 404 `ResourceNotFoundException` | As expected | Pass | 2026-08-14 |
| CAT-05 | Category | Create category (CITIZEN) | POST `/api/categories` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-14 |
| RPT-01 | Report | Submit report (approved citizen, no photo) | POST `/api/reports` multipart (`data` part only) with a CITIZEN JWT for an `APPROVED` account | 201, `status=PENDING_APPROVAL`, `reporterId` matches the JWT subject | As expected | Pass | 2026-08-15 |
| RPT-02 | Report | Submit report with a photo | POST `/api/reports` multipart with `data` + one `images` part (valid JPEG bytes) | 201, `images` has one entry with a populated `imageUrl` | As expected | Pass | 2026-08-15 |
| RPT-03 | Report | Submit report with corrupted image content | POST `/api/reports` with an `images` part declared `image/jpeg` but whose bytes are plain text | 400 `FileStorageException` ("content does not match its declared image type") | As expected | Pass | 2026-08-15 |
| RPT-04 | Report | Submit report against an unknown category | POST `/api/reports` with a non-existent `categoryId` | 404 `ResourceNotFoundException` | As expected | Pass | 2026-08-15 |
| RPT-05 | Report | Submit report with a blank title | POST `/api/reports` with `title=""` | 400, `errors.title` present | As expected | Pass | 2026-08-15 |
| RPT-06 | Report | Submit report with out-of-range latitude | POST `/api/reports` with `latitude=95.0` | 400, `errors.latitude` present | As expected | Pass | 2026-08-15 |
| RPT-07 | Report | Submit report with out-of-range longitude | POST `/api/reports` with `longitude=185.0` | 400, `errors.longitude` present | As expected | Pass | 2026-08-15 |
| RPT-08 | Report | Submit report unauthenticated | POST `/api/reports` with no `Authorization` header | 401 | As expected | Pass | 2026-08-15 |
| RPT-09 | Report | Submit report as STAFF | POST `/api/reports` with a STAFF JWT | 403 | As expected | Pass | 2026-08-15 |
| RPT-11 | Report | Submit report with an image over 5 MB | POST `/api/reports` with an `images` part larger than `spring.servlet.multipart.max-file-size` | 400 `FileStorageException` via MockMvc (a real servlet container returns 413 `MaxUploadSizeExceededException` before the request reaches the controller — not reproducible under MockMvc's simulated multipart parsing) | As expected | Pass | 2026-08-15 |
| RPT-10 | Report | Submit report as an unapproved citizen | Call `ReportService.createReport()` for a citizen whose `accountStatus != APPROVED` | 400 `BusinessRuleException` ("not yet approved to submit reports") — service-level test; no self-service path exists yet to reach this via HTTP since Phase 3 login-gating is what normally prevents it | As expected | Pass | 2026-08-15 |
| USR-01 | User | Create staff account (ADMIN, active department) | POST `/api/users/staff` with a valid body and an active `departmentId`, ADMIN JWT | 201, `role=STAFF`, `accountStatus=APPROVED`, `departmentId` matches | As expected | Pass | 2026-08-16 |
| USR-02 | User | Create staff account against an inactive department | Soft-delete a department, then POST `/api/users/staff` against it | 400 `BusinessRuleException` | As expected | Pass | 2026-08-16 |
| USR-03 | User | Create staff account against an unknown department | POST `/api/users/staff` with a non-existent `departmentId` | 404 `ResourceNotFoundException` | As expected | Pass | 2026-08-16 |
| USR-04 | User | Create staff account (CITIZEN) | POST `/api/users/staff` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-16 |
| USR-05 | User | Citizen approval queue | GET `/api/users/pending` as ADMIN, with one PENDING and one APPROVED citizen seeded | 200, only the PENDING citizen is present | As expected | Pass | 2026-08-16 |
| USR-06 | User | Approve a pending citizen | PATCH `/api/users/{id}/approve` as ADMIN on a PENDING account | 200, `accountStatus=APPROVED` | As expected | Pass | 2026-08-16 |
| USR-07 | User | Reject a pending citizen without a reason | PATCH `/api/users/{id}/reject` with `{"reason":""}` | 400, `errors.reason` present | As expected | Pass | 2026-08-16 |
| USR-08 | User | Reject a pending citizen with a reason | PATCH `/api/users/{id}/reject` with a non-blank `reason` | 200, `accountStatus=REJECTED` | As expected | Pass | 2026-08-16 |
| USR-09 | User | Suspend an approved account | PATCH `/api/users/{id}/suspend` as ADMIN on an APPROVED account | 200, `accountStatus=SUSPENDED` | As expected | Pass | 2026-08-16 |
| USR-10 | User | View own profile | GET `/api/users/{id}` with a citizen's own JWT | 200, matches own record | As expected | Pass | 2026-08-16 |
| USR-11 | User | View another citizen's profile | GET `/api/users/{id}` with a different citizen's JWT | 403 | As expected | Pass | 2026-08-16 |
| USR-12 | User | Update own profile | PUT `/api/users/{id}` with own JWT and a new `fullName`/`phone` | 200, fields updated | As expected | Pass | 2026-08-16 |
| USR-13 | User | Soft delete an account | DELETE `/api/users/{id}` as ADMIN, then GET the same id | 204 on delete; GET returns 200 with `active=false` | As expected | Pass | 2026-08-16 |
| USR-14 | User | Soft delete (CITIZEN) | DELETE `/api/users/{id}` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-16 |
| DASH-01 | Dashboard | Admin dashboard counts | GET `/api/dashboard/admin` as ADMIN, with a PENDING citizen and a PENDING_APPROVAL report seeded | 200, `pendingAccountCount`/`pendingReportCount` include them, `reportsAwaitingApproval` non-empty | As expected | Pass | 2026-08-17 |
| DASH-02 | Dashboard | Admin dashboard role gate | GET `/api/dashboard/admin` with a CITIZEN or STAFF JWT | 403 | As expected | Pass | 2026-08-17 |
| DASH-03 | Dashboard | Staff dashboard scoping | GET `/api/dashboard/staff` as STAFF (Roads), with Roads-department reports in `ASSIGNED` and `RESOLVED` seeded | 200, `totalReports`/`resolvedReports`/`newReports` reflect only Roads reports, `monthlySeries` has 12 entries | As expected | Pass | 2026-08-17 |
| DASH-04 | Dashboard | Staff dashboard role gate | GET `/api/dashboard/staff` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-17 |
| DASH-05 | Dashboard | Department performance | GET `/api/dashboard/departments` as STAFF | 200, every seeded department present (including zero-report departments with `null` averages) | As expected | Pass | 2026-08-17 |
| DASH-06 | Dashboard | Category volume | GET `/api/dashboard/categories` as ADMIN | 200, every seeded category present with its `reportCount` | As expected | Pass | 2026-08-17 |
| DASH-07 | Dashboard | Departments/categories role gate | GET `/api/dashboard/departments` or `/api/dashboard/categories` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-17 |
| MAP-01 | Report Map | Citizen visibility rule | GET `/api/reports/map` as CITIZEN with PENDING_APPROVAL, REJECTED, and ASSIGNED reports seeded | 200, only the ASSIGNED pin is present | As expected | Pass | 2026-08-17 |
| MAP-02 | Report Map | Admin sees every status | GET `/api/reports/map` as ADMIN with a PENDING_APPROVAL report seeded | 200, the PENDING_APPROVAL pin is present | As expected | Pass | 2026-08-17 |
| MAP-03 | Report Map | Slim payload shape | GET `/api/reports/map` as CITIZEN | 200, each pin has `reportCode`, `categoryName`, `status` | As expected | Pass | 2026-08-17 |
| MAP-04 | Report Map | Bounding-box filter | GET `/api/reports/map?minLat=&maxLat=&minLng=&maxLng=` with one pin inside and one outside the box | 200, only the in-bounds pin is present | As expected | Pass | 2026-08-17 |
| MAP-05 | Report Map | Status filter | GET `/api/reports/map?status=ASSIGNED` with an ASSIGNED and an IN_PROGRESS report seeded | 200, only the ASSIGNED pin is present | As expected | Pass | 2026-08-17 |
| MAP-06 | Report Map | Unauthenticated | GET `/api/reports/map` with no `Authorization` header | 401 | As expected | Pass | 2026-08-17 |
| RPT-12 | Report | Reassign department (ADMIN) | PATCH `/api/reports/{id}/assign` with `{"departmentId": <water>}` on an ASSIGNED Roads report | 200, `departmentId` updated, a same-status `report_status_history` row written | As expected | Pass | 2026-08-20 |
| RPT-13 | Report | Reassign as STAFF | PATCH `/api/reports/{id}/assign` with a STAFF JWT | 403 | As expected | Pass | 2026-08-20 |
| RPT-14 | Report | Assign staff outside the target department | `ReportAssignmentService.assign()` with a `staffId` whose `departmentId` doesn't match | 400 `BusinessRuleException` | As expected | Pass | 2026-08-20 |
| RPT-15 | Report | Reassign a terminal report | `ReportAssignmentService.assign()` on a `CLOSED` report | 400 `BusinessRuleException` | As expected | Pass | 2026-08-20 |
| RPT-16 | Report | Change priority (STAFF, own department) | PATCH `/api/reports/{id}/priority` with `{"priority": "URGENT"}` | 200, `priority` updated | As expected | Pass | 2026-08-20 |
| RPT-17 | Report | Change priority as CITIZEN | PATCH `/api/reports/{id}/priority` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-20 |
| RPT-18 | Report | Unknown priority value | `ReportWorkflowService.updatePriority()` with `priority="SUPER_URGENT"` | 400 `BusinessRuleException` | As expected | Pass | 2026-08-20 |
| RPT-19 | Report | Add a comment with a department mention | POST `/api/reports/{id}/comments` with `mentionedDepartmentId` set, as STAFF (own department) | 201, comment persisted, every STAFF member of the mentioned department gets a `DEPARTMENT_MENTION` notification | As expected | Pass | 2026-08-20 |
| RPT-20 | Report | Comments are department-scoped for STAFF | GET `/api/reports/{id}/comments` with a STAFF JWT from a different department | 403 | As expected | Pass | 2026-08-20 |
| RPT-21 | Report | Comments hidden from citizens | GET `/api/reports/{id}/comments` with a CITIZEN JWT | 403 | As expected | Pass | 2026-08-20 |
