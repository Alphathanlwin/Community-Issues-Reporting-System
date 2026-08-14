# SCIRS — Manual Test Cases

Maintained per `testing-standards.md`. Automated coverage for the same
scenarios lives in `backend/src/test/java/.../auth/`, `.../department/`,
`.../category/`, and `.../report/`.

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
