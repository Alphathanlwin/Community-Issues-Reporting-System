# SCIRS — Manual Test Cases

Maintained per `testing-standards.md`. Automated coverage for the same
scenarios lives in `backend/src/test/java/.../auth/`, `.../department/`,
and `.../category/`.

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
