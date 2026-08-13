# CLAUDE.md

Auto-loaded by Claude Code at the repository root. Read this first, every session.

**Project:** Smart Community Issue Report System (SCIRS)
**Stack:** Java 21 + Spring Boot 3 + PostgreSQL · React 19 + TypeScript + Tailwind + shadcn/ui
**Context:** CST-4105 J2EE Keystone Project — UIT, Section-C, Group-II

---

## Start-of-session checklist

1. Read `context-kit/AGENTS.md` (master index).
2. Read `context-kit/progress-tracker.md` — what is done, what phase we are in.
3. Read `context-kit/build-plan.md` — what comes next in this phase.
4. Load the topic files relevant to the task (see the routing table below).
5. Only then start writing code.

## Which file to load for which task

| If the task involves… | Load |
|---|---|
| Anything at all | `project-overview.md` |
| Where code goes, layers, packages | `architecture.md` |
| Entities, columns, enums, relationships, queries | `database-schema.md` |
| Writing any Java class | `code-standards.md` |
| Any endpoint, DTO, status code, or role gate | `api-standards.md` |
| Spring/JPA/JWT/React/Leaflet/Recharts specifics | `library-docs.md` |
| Any screen, component, or copy | `ui-rules.md` |
| Any test, or finishing a feature | `testing-standards.md` |
| Planning or sequencing work | `build-plan.md` + `progress-tracker.md` |

All files live in `context-kit/`.

---

## Repository layout

```
/backend        Spring Boot app — com.uit.scirs
/frontend       React + TypeScript app
/context-kit    This documentation kit (source of truth for design decisions)
/docs           test-cases.md, bug-log.md, diagrams, Postman collection
CLAUDE.md       This file
```

## Common commands

```bash
# Backend
cd backend && ./mvnw spring-boot:run          # run API on :8080
cd backend && ./mvnw test                     # run all tests
cd backend && ./mvnw test -Dtest=ReportWorkflowServiceTest

# Frontend
cd frontend && npm run dev                    # dev server on :5173
cd frontend && npm run build
cd frontend && npm run lint
```

Local secrets go in `backend/src/main/resources/application-local.properties` (git-ignored). Never commit real credentials, JWT secrets, or Supabase keys.

---

## Non-negotiable rules

1. **Never guess** an entity field, column name, enum value, or relationship. It is in `database-schema.md`. If it is not there, ask — do not invent it.
2. **Never invent a feature.** Every feature traces to the project proposal or `Community report system features.txt`. Out-of-scope items are listed in `project-overview.md` — flag them, do not build them.
3. **Never invent an architectural layer.** Controller → Service → Repository → DB. Nothing else.
4. **Constructor injection only.** No `@Autowired` on fields, ever.
5. **Never return entities from controllers.** Map to DTOs.
6. **Never trust a user id from a request body.** Read the principal from the JWT.
7. **Role check ≠ ownership check.** Both are required on every user-owned record.
8. **Only `ReportWorkflowService` mutates `report.status`,** and every change writes a status-history row plus a notification.
9. **`@Enumerated(EnumType.STRING)` and `FetchType.LAZY`** on everything. No exceptions.
10. **Ask when uncertain** about a design decision rather than assuming.

## RBAC quick reference

- **Admin** — seeded in the database. No sign-up screen. Full access.
- **Staff** — created by an admin from the console. No sign-up screen. Scoped to their own department.
- **Citizen** — the only self-registering role. Starts `PENDING`, cannot submit reports until an admin approves the account.

## Report lifecycle

```
PENDING_APPROVAL ──approve──> ASSIGNED ──> IN_PROGRESS ──> RESOLVED ──> CLOSED
        └────────deny────────> REJECTED (terminal)
                                RESOLVED ──reopen──> IN_PROGRESS
```

Approval auto-routes to `category.department`. `RESOLVED` requires at least one completion photo. `CLOSED` and `REJECTED` are terminal.

---

## Definition of done

A feature is not finished until every box is true:

- [ ] Endpoints follow `api-standards.md`
- [ ] Service unit tests cover the happy path and every business rule
- [ ] Controller test covers validation and role gates
- [ ] Frontend screen follows `ui-rules.md`, with loading, empty, and error states
- [ ] Row added to `docs/test-cases.md`
- [ ] Postman request added to the collection
- [ ] `context-kit/progress-tracker.md` updated

## End-of-session checklist

1. Update `context-kit/progress-tracker.md`.
2. Record any architectural decision in `progress-tracker.md` § Decisions, and mirror significant ones into `project-overview.md` § Decisions Log.
3. If an entity changed → update `database-schema.md` in the same change.
4. If an endpoint changed → update `api-standards.md` in the same change.
5. Log any bug found and fixed in `docs/bug-log.md`.

## Grading context

This is a graded keystone project. Weights: Technical Implementation 30%, Requirements Analysis & Design 20%, Presentation 15%, Database Integration 15%, UI Design 10%, Testing & Debugging 10%.

Practical consequence: **documentation and tests are graded deliverables.** Do not skip them to ship a feature faster, and keep the diagrams in `docs/` matching the code that actually exists.
