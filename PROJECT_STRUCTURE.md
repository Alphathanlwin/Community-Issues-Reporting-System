# SCIRS — Folder Structure Guide (Beginner Edition)

This file explains **every folder and what it's for**, written for someone who has never
worked in a project this size before. Read it top to bottom once, then use it as a map
whenever you get lost.

> This is a Java Spring Boot backend project (the frontend hasn't been built yet — see
> "Where's the frontend?" at the bottom).

---

## 1. The 30-second overview

This project follows a pattern called **layered architecture**. Every request that hits
the server flows through the same four layers, in the same order, every time:

```
Browser / Postman
      │
      ▼
Controller   ← "What URL was hit? What HTTP method? What JSON came in?"
      │
      ▼
Service      ← "What are the business rules? Is this allowed? What do we compute?"
      │
      ▼
Repository   ← "Talk to the database."
      │
      ▼
PostgreSQL database
```

Keep this picture in your head — almost every folder below exists to hold one piece of
one layer, for one feature.

---

## 2. Top-level layout

```
Community-Issues-Reporting-System Backend/
├── backend/          ← the actual Spring Boot (Java) application — the code that runs
├── context-kit/       ← the project's "rulebook" — design decisions, standards, plans
├── docs/               ← test case log and bug log (graded deliverables)
├── .idea/, .vscode/    ← editor settings (IntelliJ / VS Code) — ignore these
├── CIRS.pen            ← a UI design file (opened with a design tool, not code)
├── CLAUDE.md            ← instructions read automatically by the AI assistant
├── AGENTS.md            ← index/table of contents for context-kit/
└── README.md             ← currently just the project title
```

**`backend/`** is the only folder with code that runs. Everything else is
documentation, config, or design files. If you're asked "where is X built?", the
answer is always inside `backend/`.

---

## 3. Inside `backend/` — the Spring Boot app

```
backend/
├── pom.xml              ← the project's ingredient list (see below)
└── src/
    ├── main/
    │   ├── java/com/uit/scirs/    ← ALL the actual application code lives here
    │   └── resources/
    │       └── application.properties   ← configuration (DB connection, ports, etc.)
    └── test/
        ├── java/com/uit/scirs/    ← automated tests, mirrors the main/ folder exactly
        └── resources/
            └── application-test.properties   ← config used only while running tests
```

### `pom.xml` — what is it?

This project uses **Maven**, a tool that manages Java dependencies (external libraries
the project needs, like Spring Boot itself). `pom.xml` is Maven's config file — it lists
which libraries to download and how to build/run the project. You almost never need to
read it in detail; just know it's the reason `./mvnw spring-boot:run` works.

### Why `com/uit/scirs`?

Java packages mirror folder paths. `com.uit.scirs` is just this project's unique
namespace (University of Information Technology → SCIRS project) — every class lives
somewhere under `backend/src/main/java/com/uit/scirs/`.

### The "package-by-feature" pattern

Open `backend/src/main/java/com/uit/scirs/` and you'll see folders like `auth/`,
`category/`, `department/`, `report/`, `user/`, `common/`. This project groups code
**by feature**, not by layer — so all the code related to "categories" lives together,
instead of all controllers living together separately from all services.

Every feature folder repeats the same internal shape:

| Subfolder | Layer | Beginner explanation |
|---|---|---|
| `controller/` | Controller | Defines the URLs (e.g. `GET /api/categories`). Reads the incoming request, calls the service, and sends back a response. **Contains no business logic.** |
| `service/` | Service | The "brain" of the feature. Business rules live here (e.g. "a category must belong to an active department"). |
| `repository/` | Repository | Talks to PostgreSQL. Usually just an interface — Spring Data JPA writes the actual SQL for you. |
| `entity/` | — | A Java class that maps directly to a database table (e.g. `Category.java` ↔ the `categories` table). |
| `dto/` | — | "Data Transfer Object" — a plain class used to send/receive JSON over the API. Entities are **never** sent directly to the browser; they're converted to DTOs first (this keeps internal DB details hidden and prevents security leaks). |
| `mapper/` | — | Small helper class that converts Entity ⇄ DTO. |

So for example, `backend/src/main/java/com/uit/scirs/category/` has:

```
category/
├── controller/CategoryController.java   ← handles /api/categories requests
├── service/CategoryService.java         ← business rules ("category needs an active department")
├── repository/CategoryRepository.java   ← fetches/saves Category rows in Postgres
├── entity/Category.java                 ← maps to the `categories` table
├── dto/CategoryDTO.java, CreateCategoryDTO.java, UpdateCategoryDTO.java
└── mapper/CategoryMapper.java           ← Category (entity) ⇄ CategoryDTO (JSON)
```

The same pattern repeats for `auth/`, `department/`, `report/`, and (soon) `user/`,
`notification/`, `score/`, `feedback/`, `dashboard/`.

### `common/` — shared code used by every feature

```
common/
├── config/       ← app-wide setup: CORS rules, async config, startup data seeding
├── security/     ← login/JWT machinery (JwtUtil, SecurityConfig, the auth filter)
├── exception/    ← custom errors + the global handler that turns them into JSON responses
├── integration/  ← talking to things outside the app, like file storage
└── util/         ← small shared helper classes (e.g. generating a report code like RPT-2026-001)
```

Think of `common/` as the toolbox every feature folder is allowed to reach into.
Nothing feature-specific belongs here.

### `backend/src/test/`

This mirrors `main/` folder-for-folder. For every class in `main/`, there's usually a
matching test class here — e.g. `category/service/CategoryServiceTest.java` tests
`CategoryService.java`. Running `./mvnw test` runs everything in this folder.

---

## 4. `context-kit/` — the project's rulebook (not code)

This is documentation the team (and the AI assistant) reads **before** writing any
code, so decisions stay consistent across sessions. As a beginner, skim these once to
understand *why* the code looks the way it does:

| File | What it tells you |
|---|---|
| `project-overview.md` | What SCIRS is, who uses it, what it does |
| `architecture.md` | The exact layer diagram shown in §1, plus the full package structure |
| `database-schema.md` | Every table, column, and relationship — the single source of truth |
| `code-standards.md` | Naming rules, formatting, patterns to follow |
| `api-standards.md` | URL naming, status codes, request/response shape rules |
| `library-docs.md` | Notes on Spring/JPA/JWT/React usage specifics |
| `ui-rules.md` | Rules for how screens should look and behave |
| `testing-standards.md` | What "well-tested" means for this project |
| `build-plan.md` | The 8-week phased roadmap |
| `progress-tracker.md` | What's actually done vs. still to do, right now |

## 5. `docs/` — graded evidence

```
docs/
├── test-cases.md   ← a running log of test cases (manual + automated), required for grading
└── bug-log.md       ← every bug found + how it was fixed
```

## 6. Other root files

| File | Purpose |
|---|---|
| `CIRS.pen` | A design file for the [Pencil](https://pencil.dev) design tool — UI mockups, not code. Opened with a special tool, not a text editor. |
| `CLAUDE.md` | Instructions the AI assistant reads automatically at the start of every session (which files to load, non-negotiable rules, RBAC summary). |
| `AGENTS.md` | Table of contents / reading order for `context-kit/`. |
| `.idea/`, `.vscode/` | Editor-specific settings (IntelliJ IDEA, VS Code). Not part of the app. |

---

## 7. Where's the frontend?

`CLAUDE.md` describes a planned `/frontend` folder (React 19 + TypeScript + Tailwind),
but **it doesn't exist yet** — per `context-kit/progress-tracker.md`, frontend scaffolding
is still unchecked. Right now this repository is backend-only; you can exercise the API
with Postman or `curl` while the frontend is being built.

---

## 8. A concrete example: tracing one request

To tie it all together, here's what happens when the browser calls
`GET /api/categories`:

1. `category/controller/CategoryController.java` receives the HTTP request.
2. It calls `category/service/CategoryService.java` — no logic in the controller itself.
3. The service calls `category/repository/CategoryRepository.java` to fetch rows from
   the `categories` table in PostgreSQL.
4. The service gets back `Category` **entities**, and uses
   `category/mapper/CategoryMapper.java` to turn them into `CategoryDTO` objects.
5. The controller wraps the DTOs in a `ResponseEntity` and returns JSON to the browser.

Every feature in this codebase follows this exact same five-step flow — once you've
understood it for `category/`, you understand it for `report/`, `department/`, and
everything still to come.
