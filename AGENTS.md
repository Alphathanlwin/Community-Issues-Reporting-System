# SCIRS Agent Context Kit — Master Index

**Project:** Smart Community Issue Report System (SCIRS)
**Course:** CST-4105 Enterprise Applications Development using Java — Keystone Project
**Team:** Section-C, Group-II (Leader: Htoo Myat Min Eain)

**Load Order:** When starting a new AI session, load these context files in the order listed below. Each file builds on the previous one to give the agent a complete understanding of the SCIRS project.

## Context Files (Load in Order)

| # | File | Purpose |
|---|------|---------|
| 1 | `project-overview.md` | Product vision, actors, goals, business domain |
| 2 | `architecture.md` | System architecture, layers, data flow, package structure |
| 3 | `database-schema.md` | Complete database schema, entities, relationships, enums |
| 4 | `code-standards.md` | Coding conventions, naming rules, patterns, anti-patterns |
| 5 | `api-standards.md` | REST API design rules, endpoint patterns, response formats |
| 6 | `library-docs.md` | Library usage notes: Spring Boot, JPA, JWT, React, Tailwind, Leaflet |
| 7 | `ui-rules.md` | UI behaviour and layout rules for the citizen and staff frontends |
| 8 | `testing-standards.md` | Test layers, naming, coverage targets, manual test evidence |
| 9 | `build-plan.md` | Phased development roadmap and feature sequencing |
| 10 | `progress-tracker.md` | Live development progress per module and feature |

## Source Documents (Ground Truth)

This kit is derived from — and must stay consistent with — two team documents:

| Source | What it defines |
|--------|-----------------|
| `Smart Community Issue Report System — Project Proposal.pdf` | Introduction, problem statement, objectives, scope, functional + non-functional requirements, actors, tool stack, 8-week schedule |
| `Community report system features.txt` | Screen-by-screen feature list: RBAC rules, admin/citizen/staff panels, approval queues, leaderboard and scoring, department list, notification triggers, sign-up fields |

Where the two disagree, **`Community report system features.txt` wins** for *screens and behaviour* (it is the newer, more detailed team decision), and the **proposal wins** for *scope boundaries and deliverables* (it is the graded document). Resolved conflicts are recorded in `project-overview.md` § Decisions Log.

## How to Use This Kit

### Starting a New Session
Load context files 1–10 in order before beginning any work.

### Before a New Feature
1. Review `build-plan.md` for the current phase
2. Review `progress-tracker.md` for what is already done
3. Review `architecture.md` for where the feature fits
4. Review `code-standards.md` for how to write it
5. Review `api-standards.md` if building endpoints
6. Review `database-schema.md` if touching entities
7. Review `ui-rules.md` if building screens

### After Completing Work
1. Update `progress-tracker.md` with completed items
2. Note any architectural decision in `project-overview.md` § Decisions Log
3. Record test evidence per `testing-standards.md`

## Critical Rules for AI Agents

1. **NEVER guess entity fields, relationships, or enum values** — always refer to `database-schema.md`
2. **NEVER invent new architectural layers** — follow `architecture.md`
3. **NEVER return entities directly from controllers** — always map to DTOs
4. **ALWAYS use constructor injection**, never field injection
5. **NEVER invent a feature that is not in the proposal or the features file** — out-of-scope items belong in `project-overview.md` § Out of Scope
6. **ALWAYS check `progress-tracker.md`** before starting work
7. **ASK if uncertain** about a design decision rather than assuming
8. **Remember the grading rubric** (see `project-overview.md` § Evaluation Rubric) — database design, testing, and UI are each separately graded, not optional extras
