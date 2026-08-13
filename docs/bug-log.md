# SCIRS — Bug Log

Maintained per `testing-standards.md`. The rubric rewards bugs being
identified and resolved — this is the evidence.

| ID | Description | Module | Severity | How Found | Root Cause | Fix | Commit |
|----|-------------|--------|----------|-----------|------------|-----|--------|
| BUG-01 | `AuthController.login` and `.me` returned 500 instead of 200 | Auth | High | `AuthControllerIntegrationTest` failing with `LazyInitializationException` | `User.role` is `FetchType.LAZY`; `AuthService.login()`/`.me()` were not `@Transactional`, and `spring.jpa.open-in-view=false` closes the Hibernate session before the mapper reads `user.getRole()` | Annotated both methods `@Transactional(readOnly = true)` | 3955ae6 |
