# SCIRS — Manual Test Cases

Maintained per `testing-standards.md`. Automated coverage for the same
scenarios lives in `backend/src/test/java/.../auth/`.

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
