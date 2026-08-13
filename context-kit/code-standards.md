# 4. SCIRS — Code Standards

## General Principles

| Principle | Details |
|-----------|---------|
| Clean architecture | Over quick implementation |
| Enterprise style | Over tutorial style |
| Separation of concerns | Each layer has exactly one job |
| Single responsibility | Each class does one thing well |
| Maintainability | Code should be readable six months later |
| Consistency | Same patterns everywhere, no special cases |

---

## Dependency Injection

**DO — Constructor injection**

```java
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final CategoryRepository categoryRepository;

    public ReportService(ReportRepository reportRepository,
                         ReportMapper reportMapper,
                         CategoryRepository categoryRepository) {
        this.reportRepository = reportRepository;
        this.reportMapper = reportMapper;
        this.categoryRepository = categoryRepository;
    }
}
```

**DON'T — Field injection**

```java
@Service
public class ReportService {
    @Autowired
    private ReportRepository reportRepository; // NEVER do this
}
```

---

## Controller Standards

```java
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CITIZEN')")
    public ResponseEntity<ReportDTO> getReport(@PathVariable Long id,
                                               @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(reportService.getReportById(id, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ReportDTO> createReport(@Valid @RequestBody CreateReportDTO dto,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        ReportDTO created = reportService.createReport(dto, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

**Controller rules**
- Annotate with `@RestController` and `@RequestMapping` for the base path
- Return `ResponseEntity<>` with explicit status codes
- Use `@Valid` on every request body
- Use `@PreAuthorize` for role gates; pass `CurrentUser` down for ownership checks
- **Never** read a user id from the request body — always from the authenticated principal
- **Never** put business logic here
- **Never** call repositories directly
- **Never** catch exceptions here (the global handler does it)

---

## Service Standards

```java
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final NotificationService notificationService;

    // constructor omitted for brevity

    public ReportDTO getReportById(Long id, CurrentUser currentUser) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        assertCanView(report, currentUser);
        return reportMapper.toDTO(report);
    }

    @Transactional
    public ReportDTO createReport(CreateReportDTO dto, Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (citizen.getAccountStatus() != AccountStatus.APPROVED) {
            throw new BusinessRuleException("Your account is not yet approved to submit reports.");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Report report = reportMapper.toEntity(dto);
        report.setReporter(citizen);
        report.setCategory(category);
        report.setStatus(ReportStatus.PENDING_APPROVAL);
        report.setReportCode(reportCodeGenerator.next());

        Report saved = reportRepository.save(report);
        statusHistoryService.record(saved, null, ReportStatus.PENDING_APPROVAL, citizen, null);
        notificationService.notifyAdminsOfNewReport(saved);

        return reportMapper.toDTO(saved);
    }
}
```

**Service rules**
- Annotate with `@Service`; `@Transactional` on every write method
- All business logic lives here — workflow transitions, routing, scoring, ownership
- Accept and return **DTOs**, never entities
- Use mapper classes for conversion
- Throw custom business exceptions, never generic `RuntimeException`
- May call multiple repositories and other services; avoid circular dependencies
- **Never** touch `HttpServletRequest`, `HttpSession`, or `ResponseEntity`

### Ownership check pattern

Every service that returns or mutates a user-owned record must call an assertion helper:

```java
private void assertCanView(Report report, CurrentUser user) {
    switch (user.getRole()) {
        case ADMIN -> { /* full access */ }
        case STAFF -> {
            if (!report.getDepartment().getId().equals(user.getDepartmentId())) {
                throw new AccessDeniedException("This report belongs to another department.");
            }
        }
        case CITIZEN -> {
            if (!report.getReporter().getId().equals(user.getId())) {
                throw new AccessDeniedException("You can only view your own reports.");
            }
        }
    }
}
```

---

## Repository Standards

```java
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporterId(Long reporterId);

    List<Report> findByDepartmentIdAndStatus(Long departmentId, ReportStatus status);

    Optional<Report> findByReportCode(String reportCode);

    long countByStatus(ReportStatus status);

    @Query("""
           SELECT r.category.name AS category, COUNT(r) AS total
           FROM Report r
           GROUP BY r.category.name
           """)
    List<CategoryVolumeProjection> countGroupedByCategory();
}
```

**Repository rules**
- Extend `JpaRepository<Entity, Long>`, annotate with `@Repository`
- Prefer Spring Data derived query methods; use `@Query` only when derivation is insufficient
- Return `Optional<>` for single-result lookups
- **Never** put business logic here
- Aggregations return **projections or DTOs**, never full entity lists

---

## Entity Standards

```java
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", nullable = false, unique = true)
    private String reportCode;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING_APPROVAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportPriority priority = ReportPriority.NORMAL;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // constructors, getters, setters
}
```

**Entity rules**
- `@Entity` + `@Table(name = "...")` on every entity
- `@Id` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `FetchType.LAZY` for **all** relationships
- `@Enumerated(EnumType.STRING)` for **all** enums
- Coordinates use `BigDecimal`, never `double`
- Set `cascade` and `orphanRemoval` explicitly
- **Never** return an entity from a controller

---

## DTO Standards

```java
public class ReportDTO {
    private Long id;
    private String reportCode;
    private String title;
    private String description;
    private String status;
    private String priority;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressText;
    private Long categoryId;
    private String categoryName;
    private Long departmentId;
    private String departmentName;
    private Long reporterId;
    private String reporterName;
    private List<ReportImageDTO> images;
    private LocalDateTime createdAt;
    // getters, setters
}
```

```java
public class CreateReportDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    private String addressText;
    // getters, setters
}
```

**DTO rules**
- Separate DTOs for request (`CreateReportDTO`, `UpdateReportStatusDTO`) and response (`ReportDTO`)
- Jakarta validation annotations on request DTOs
- **Never** expose `passwordHash`, internal flags, or NRC numbers in a response DTO returned to a non-admin
- DTOs are plain Java objects — no JPA annotations
- Enums are serialised as their **string name** in DTOs

---

## Mapper Standards

```java
@Component
public class ReportMapper {

    public ReportDTO toDTO(Report entity) {
        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setReportCode(entity.getReportCode());
        dto.setTitle(entity.getTitle());
        dto.setStatus(entity.getStatus().name());
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        if (entity.getDepartment() != null) {
            dto.setDepartmentId(entity.getDepartment().getId());
            dto.setDepartmentName(entity.getDepartment().getName());
        }
        return dto;
    }

    public Report toEntity(CreateReportDTO dto) {
        Report entity = new Report();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setAddressText(dto.getAddressText());
        return entity;
    }

    public List<ReportDTO> toDTOList(List<Report> entities) {
        return entities.stream().map(this::toDTO).toList();
    }
}
```

**Mapper rules**
- Annotate with `@Component` so it can be injected
- Manual mapping (no MapStruct unless explicitly requested)
- One mapper per feature module
- Methods: `toDTO()`, `toEntity()`, `toDTOList()`
- Always null-check optional relationships

---

## Exception Handling Standards

```java
public class ResourceNotFoundException extends RuntimeException { /* ... */ }
public class DuplicateResourceException extends RuntimeException { /* ... */ }
public class BusinessRuleException extends RuntimeException { /* ... */ }
public class InvalidStatusTransitionException extends RuntimeException { /* ... */ }
public class AccountNotApprovedException extends RuntimeException { /* ... */ }
public class FileStorageException extends RuntimeException { /* ... */ }
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleTransition(InvalidStatusTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({BusinessRuleException.class, AccountNotApprovedException.class})
    public ResponseEntity<ErrorResponse> handleBusinessRule(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // collect field errors into ErrorResponse.errors, return 400
    }
}
```

**Exception rules**
- Never catch exceptions in controllers
- Custom exceptions and the handler live in `common/exception/`
- Always return the same `ErrorResponse` shape (see `api-standards.md`)
- Error messages are user-facing: clear, specific, and never leaking stack traces or SQL

---

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Package | lowercase, feature-based | `com.uit.scirs.report` |
| Class | PascalCase | `ReportService` |
| Interface | PascalCase | `ReportRepository` |
| Method | camelCase | `getReportById()` |
| Variable | camelCase | `reportCode` |
| Constant | UPPER_SNAKE_CASE | `MAX_IMAGE_SIZE_BYTES` |
| DTO class | PascalCase + `DTO` suffix | `ReportDTO`, `CreateReportDTO` |
| Entity class | PascalCase, singular | `Report`, `PointTransaction` |
| Table name | snake_case, plural | `reports`, `point_transactions` |
| Column name | snake_case | `report_code`, `created_at` |
| Enum | PascalCase | `ReportStatus`, `RoleName` |
| Enum values | UPPER_SNAKE_CASE | `PENDING_APPROVAL`, `REPORT_RESOLVED` |
| REST endpoints | kebab-case, plural nouns | `/api/report-comments` |
| React component | PascalCase | `ReportDetailPage.tsx` |
| React hook | camelCase, `use` prefix | `useReports.ts` |

---

## Anti-Patterns to Avoid

| Anti-Pattern | Why It's Bad |
|--------------|--------------|
| Returning entities from controllers | Exposes internal structure, causes lazy-load serialisation errors |
| Business logic in controllers | Violates separation of concerns |
| Business logic in repositories | Repositories are data access only |
| Field injection (`@Autowired` on a field) | Harder to test, hides dependencies |
| Catching exceptions in controllers | Inconsistent error responses |
| Trusting a user id from the request body | Lets any citizen impersonate another user |
| Role check without an ownership check | A citizen could read another citizen's report by id |
| Changing `report.status` directly in any service other than `ReportWorkflowService` | Bypasses transition validation and history logging |
| `EAGER` fetch on relationships | N+1 queries and huge payloads |
| `EnumType.ORDINAL` | Reordering an enum silently corrupts data |
| Hardcoded department or category ids | Breaks when seed data changes — look them up by name |
| God services | Violates single responsibility; split by use case |
| Circular dependencies between modules | Spring context startup failure |
| Ignoring `Optional` from repositories | NullPointerException at runtime |
