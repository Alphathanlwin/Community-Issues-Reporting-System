# 8. SCIRS — Testing Standards

Testing & Debugging is **10% of the final grade**, and Technical Implementation (30%) is judged partly on whether bugs were found and fixed. Tests are a deliverable, not an optional extra.

---

## Test Layers

| Layer | Tool | What it covers | Where |
|-------|------|----------------|-------|
| Unit — service | JUnit 5 + Mockito | Business rules in isolation, repositories mocked | `src/test/java/.../{module}/service/` |
| Slice — repository | `@DataJpaTest` + H2 or Testcontainers | Derived queries, custom `@Query`, constraints | `src/test/java/.../{module}/repository/` |
| Slice — controller | `@WebMvcTest` + MockMvc | Status codes, validation errors, role gates | `src/test/java/.../{module}/controller/` |
| Integration | `@SpringBootTest` + MockMvc | Full request → DB flows for critical paths | `src/test/java/.../integration/` |
| Manual / API | Postman collection | Demo script, exported as a deliverable | `docs/postman/` |
| Manual / UI | Test-case table | Every screen, both shells, mobile + desktop | `docs/test-cases.md` |

---

## Naming Convention

```java
@Test
void createReport_whenAccountNotApproved_throwsBusinessRuleException() { ... }

@Test
void updateStatus_fromClosedToInProgress_throwsInvalidStatusTransitionException() { ... }
```

Pattern: `methodUnderTest_condition_expectedResult`. Arrange–Act–Assert, with a blank line between the three blocks.

---

## Priority Test Cases (Write These First)

These map directly to the business rules in `project-overview.md`. If time is short, these are the ones that must exist.

### Auth & accounts
1. Citizen registration creates a `PENDING` account with role `CITIZEN`
2. Registration with a `role` field in the body still produces `CITIZEN` (privilege-escalation guard)
3. Duplicate email → 409; duplicate NRC → 409
4. Login with a `PENDING` account → 403
5. Login with wrong password → 401
6. Protected endpoint without a token → 401
7. Citizen calling an admin endpoint → 403
8. Admin approving an account flips status to `APPROVED` and creates a notification

### Reports
9. A `PENDING` citizen cannot submit a report
10. A submitted report starts at `PENDING_APPROVAL` with a unique report code
11. Approval auto-assigns the department from the category
12. Approval awards `+10` points exactly once (re-approval does not double-award)
13. Denial requires a reason and awards `−5`
14. Every allowed transition succeeds; every disallowed transition throws (parameterised test over the transition matrix)
15. `RESOLVED` without a completion photo is rejected
16. Every status change writes exactly one `report_status_history` row
17. Every status change creates exactly one notification for the reporter

### Access control
18. Citizen A cannot fetch citizen B's report by id → 403
19. Staff in the Roads department cannot fetch a Water department report → 403
20. `/api/reports/map` excludes `PENDING_APPROVAL` and `REJECTED` reports for citizens

### Feedback & scoring
21. Feedback on a non-resolved report is rejected
22. Second feedback on the same report → 409
23. Feedback from a non-reporter → 403
24. Leaderboard order matches the sum of point transactions

### Uploads
25. A non-image file is rejected (415)
26. A file over 5 MB is rejected (413)
27. The stored filename differs from the uploaded filename

---

## Service Test Pattern

```java
@ExtendWith(MockitoExtension.class)
class ReportWorkflowServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock NotificationService notificationService;
    @Mock ScoreService scoreService;
    @InjectMocks ReportWorkflowService workflowService;

    @Test
    void approve_setsStatusAssignedAndRoutesToCategoryDepartment() {
        Department roads = department(2L, "Roads");
        Category pothole = category(1L, "Pothole / Damaged Road", roads);
        Report report = report(10L, ReportStatus.PENDING_APPROVAL, pothole);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));

        ReportDTO result = workflowService.approve(10L, adminUser());

        assertThat(result.getStatus()).isEqualTo("ASSIGNED");
        assertThat(result.getDepartmentId()).isEqualTo(2L);
        verify(scoreService).award(report.getReporter(), PointReason.REPORT_APPROVED, report);
        verify(notificationService).notifyStatusChange(report);
    }
}
```

## Controller Test Pattern

```java
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ReportService reportService;

    @Test
    @WithMockUser(roles = "CITIZEN")
    void createReport_withBlankTitle_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/reports").contentType(APPLICATION_JSON).content("{\"title\":\"\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.title").exists());
    }
}
```

---

## Test Data

- Build entities with small private factory methods or an `ObjectMother` class in `src/test/java/.../support/` — never copy-paste 20-line builders into every test
- Integration tests use `@Transactional` so each test rolls back
- Never point a test at the development database; use H2 or a Testcontainers PostgreSQL instance configured in `src/test/resources/application-test.properties`

---

## Manual Test Evidence (For the Report and Presentation)

Maintain `docs/test-cases.md` as a table — this is what the grader reads:

| ID | Module | Scenario | Steps | Expected | Actual | Pass/Fail | Date |
|----|--------|----------|-------|----------|--------|-----------|------|

Cover, at minimum: every citizen screen on a real phone-sized viewport, every console screen at 1280 px, all three roles' permission boundaries, and the full happy-path lifecycle from sign-up to feedback.

## Bug Log

Maintain `docs/bug-log.md` with: ID, description, module, severity, how it was found, root cause, fix, and the commit hash. The rubric explicitly rewards bugs being "identified and resolved" — a visible log is the evidence.

---

## Definition of Done (Per Feature)

A feature is not done until all of these are true:

- [ ] Endpoint(s) implemented following `api-standards.md`
- [ ] Service unit tests for the happy path and every business rule
- [ ] Controller test for validation and role gates
- [ ] Frontend screen implemented per `ui-rules.md`, including loading, empty, and error states
- [ ] Manual test row added to `docs/test-cases.md`
- [ ] Postman request added to the collection
- [ ] `progress-tracker.md` updated
