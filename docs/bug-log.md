# SCIRS — Bug Log

Maintained per `testing-standards.md`. The rubric rewards bugs being
identified and resolved — this is the evidence.

| ID | Description | Module | Severity | How Found | Root Cause | Fix | Commit |
|----|-------------|--------|----------|-----------|------------|-----|--------|
| BUG-01 | `AuthController.login` and `.me` returned 500 instead of 200 | Auth | High | `AuthControllerIntegrationTest` failing with `LazyInitializationException` | `User.role` is `FetchType.LAZY`; `AuthService.login()`/`.me()` were not `@Transactional`, and `spring.jpa.open-in-view=false` closes the Hibernate session before the mapper reads `user.getRole()` | Annotated both methods `@Transactional(readOnly = true)` | 3955ae6 |
| BUG-02 | `DepartmentRepository`/`CategoryRepository` failed application startup with `PropertyReferenceException: No property 'isActive' found for type 'Category'` | Department/Category | High | `DepartmentControllerIntegrationTest` failing to load the Spring context | Both entities store the flag as a field named `active` (JavaBean property `active`, getter `isActive()`); the derived query method was named `findByIsActiveTrue()`, which Spring Data parses as property `isActive` — a property that does not exist | Renamed both repository methods to `findByActiveTrue()`; corrected `database-schema.md` to match | (pending commit) |
| BUG-03 | `GET /api/categories` returned 500 instead of 200 | Category | High | `CategoryControllerIntegrationTest` failing with `LazyInitializationException` | `Category.department` is `FetchType.LAZY`; `CategoryService.getAll()`/`.getById()` were not `@Transactional`, and `spring.jpa.open-in-view=false` closes the Hibernate session before `CategoryMapper` reads `category.getDepartment().getName()` | Annotated both methods `@Transactional(readOnly = true)` | (pending commit) |
