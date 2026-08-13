# 6. SCIRS — Library & Framework Usage Notes

Project-specific usage patterns. Refer to official docs for complete API references — this file captures how these libraries are used *in SCIRS*.

---

## Spring Boot

### Application Entry Point

```java
@SpringBootApplication
public class ScirsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScirsApplication.class, args);
    }
}
```

### Key Configuration (`application.properties`)

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/scirs_db
spring.datasource.username=<db_user>
spring.datasource.password=<db_password>

# Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=<secret_key>
jwt.expiration=86400000

# File upload
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=20MB
app.storage.provider=local            # local | supabase
app.storage.local-path=./uploads
app.supabase.url=<supabase_url>
app.supabase.bucket=report-images
app.supabase.service-key=<service_key>

# Mail (SendGrid SMTP)
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=<sendgrid_api_key>
app.mail.from=noreply@scirs.gov
app.mail.enabled=true

# Server
server.port=8080
```

> Never commit real secrets. Use `application-local.properties` (git-ignored) or environment variables.

### Common Spring Annotations Used

| Annotation | Layer | Purpose |
|-----------|-------|---------|
| `@SpringBootApplication` | Main | Entry point, auto-config |
| `@RestController` | Controller | REST endpoint class |
| `@RequestMapping` | Controller | Base path |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping` / `@DeleteMapping` | Controller | HTTP handlers |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | Controller | Bind inputs |
| `@RequestPart` | Controller | Multipart upload parts |
| `@Valid` | Controller | Trigger DTO validation |
| `@PreAuthorize` | Controller | Method-level role check |
| `@AuthenticationPrincipal` | Controller | Inject the current user |
| `@Service` | Service | Business logic bean |
| `@Transactional` | Service | Transaction boundary |
| `@Async` | Service | Non-blocking email sending |
| `@Repository` | Repository | Data access bean |
| `@Query` | Repository | Custom JPQL |
| `@Entity` / `@Table` / `@Id` / `@GeneratedValue` / `@Column` | Entity | JPA mapping |
| `@ManyToOne` / `@OneToMany` / `@OneToOne` / `@JoinColumn` | Entity | Relationships |
| `@Enumerated` | Entity | Enum → string column |
| `@CreationTimestamp` / `@UpdateTimestamp` | Entity | Auto timestamps |
| `@Component` | Mapper / Util | General Spring bean |
| `@RestControllerAdvice` / `@ExceptionHandler` | Exception | Global error handling |
| `@Scheduled` | Job | "Report waiting too long" sweeper |

### Seeding Data on Startup

```java
@Component
public class DataSeeder implements CommandLineRunner {

    // roles → departments → categories → admin user
    @Override
    public void run(String... args) {
        seedRoles();
        seedDepartments();   // Electricity, Roads, Water, Sanitation, Parks, Buildings
        seedCategories();    // each mapped to its default department
        seedAdminUser();     // only if no ADMIN exists
    }
}
```

The seeder must be **idempotent** — check for existence before inserting, so restarts never duplicate rows.

---

## Spring Data JPA

```java
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporterId(Long reporterId);
    List<Report> findByDepartmentIdAndStatus(Long departmentId, ReportStatus status);
    long countByStatus(ReportStatus status);
    List<Report> findByStatusAndCreatedAtBefore(ReportStatus status, LocalDateTime cutoff);
}
```

### Derived Query Keywords Used in This Project

| Keyword | SQL | Example |
|---------|-----|---------|
| `findBy` | SELECT ... WHERE | `findByEmail(String email)` |
| `existsBy` | SELECT EXISTS | `existsByNrcNumber(String nrc)` |
| `countBy` | SELECT COUNT | `countByAccountStatus(AccountStatus s)` |
| `And` / `Or` | AND / OR | `findByDepartmentIdAndStatus()` |
| `Between` | BETWEEN | `findByCreatedAtBetween()` |
| `Before` / `After` | < / > | `findByCreatedAtBefore()` |
| `IsTrue` / `IsFalse` | = true / false | `findByIsActiveTrue()` |
| `OrderBy` | ORDER BY | `findByRecipientIdOrderByCreatedAtDesc()` |
| `Top`/`First` | LIMIT | `findTop10ByRoleNameOrderByCreatedAtDesc()` |

### Aggregation with projections

```java
public interface DepartmentPerformanceProjection {
    String getDepartmentName();
    Long   getTotalReports();
    Long   getResolvedReports();
    Double getAvgResolutionHours();
}

@Query("""
       SELECT d.name AS departmentName,
              COUNT(r) AS totalReports,
              SUM(CASE WHEN r.status IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END) AS resolvedReports,
              AVG(FUNCTION('EXTRACT', EPOCH FROM (r.resolvedAt - r.createdAt)) / 3600) AS avgResolutionHours
       FROM Report r JOIN r.department d
       GROUP BY d.name
       """)
List<DepartmentPerformanceProjection> findDepartmentPerformance();
```

Dashboards must use aggregate queries like this — **never** load every report into memory and count in Java.

---

## Hibernate (JPA)

```java
// Many-to-One
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;

// One-to-Many
@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ReportImage> images = new ArrayList<>();

// One-to-One
@OneToOne(mappedBy = "report", cascade = CascadeType.ALL)
private Feedback feedback;

// Enum
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private ReportStatus status;
```

### Key Hibernate Rules for This Project
- `ddl-auto=update` during development; generate a final SQL script for the submission
- Always `FetchType.LAZY` — use `@EntityGraph` or a `JOIN FETCH` query where a relationship is genuinely needed
- Always `EnumType.STRING`
- Set cascade and `orphanRemoval` explicitly
- Watch for N+1 on the report list: use `@EntityGraph(attributePaths = {"category", "department", "reporter"})` on `findAll`-style queries

---

## Spring Security + JWT

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JWT Flow in This Project
1. `POST /api/auth/login` with email + password
2. `AuthService` verifies the BCrypt hash and checks `accountStatus == APPROVED`
3. JWT is generated with claims: `sub` (email), `uid`, `role`, `deptId`, `iat`, `exp`
4. Client stores the token and sends `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` validates the token and builds a `CurrentUser` principal with authority `ROLE_<role>`
6. `@PreAuthorize` handles role gates; services handle ownership gates

---

## File Upload & Storage

```java
public interface FileStorageService {
    String store(MultipartFile file, String folder);   // returns a public URL
    void delete(String url);
}
```

Validation performed **before** storing, inside `FileStorageService`:
- Content type must be `image/jpeg`, `image/png`, or `image/webp`
- Size ≤ 5 MB
- Filename regenerated as `{uuid}.{ext}` — never reuse the client filename
- Reject files whose magic bytes do not match the declared content type

Controller pattern for report submission:

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ReportDTO> createReport(
        @Valid @RequestPart("data") CreateReportDTO dto,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @AuthenticationPrincipal CurrentUser currentUser) { ... }
```

---

## Email Notifications

```java
@Service
public class EmailService {

    @Async
    public void sendStatusChangeEmail(String to, Report report) { ... }
}
```

Rules: email sending is `@Async` and wrapped in try/catch — a mail failure is logged but must **never** roll back a status change. The in-app `notifications` row is the source of truth; email is best-effort. Enable `@EnableAsync` in `common/config`.

---

## Jakarta Validation

```java
public class CitizenRegisterDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number")
    private String phone;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "NRC is required")
    private String nrcNumber;
}
```

---

## React 19 + TypeScript

### Project Conventions
- Functional components only
- `.tsx` for components, `.ts` for utilities and types
- Component files: PascalCase (`ReportListPage.tsx`)
- Hook files: camelCase with `use` prefix (`useReports.ts`)
- API calls isolated in `services/` — never `fetch` directly inside a component
- DTO types in `types/` mirror the backend DTOs exactly

### API Service Pattern

```typescript
// services/reportService.ts
import { api } from './api';
import type { ReportDTO, CreateReportDTO } from '../types/report';

export const reportService = {
  getMyReports: (): Promise<ReportDTO[]> => api.get('/reports/my'),

  create: (data: CreateReportDTO, images: File[]): Promise<ReportDTO> => {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    images.forEach(img => form.append('images', img));
    return api.postForm('/reports', form);
  },

  updateStatus: (id: number, status: string, remarks?: string): Promise<ReportDTO> =>
    api.patch(`/reports/${id}/status`, { status, remarks }),
};
```

`services/api.ts` centralises the base URL, JWT header injection, and error handling (401 → clear token and redirect to login).

### Auth Context
`context/AuthContext.tsx` holds `{ user, token, login(), logout(), isRole() }`. A `ProtectedRoute` wrapper checks both authentication and the allowed roles, and redirects citizens to the citizen shell and staff/admin to the desktop shell.

---

## Tailwind CSS + shadcn/ui

- Tailwind utility classes for layout and spacing; shadcn/ui for Button, Card, Dialog, Table, Select, Badge, Toast, Tabs
- Mobile-first: unprefixed utilities target mobile, `md:` and `lg:` scale up to the staff desktop UI
- Status colours are defined once as Tailwind theme tokens and reused everywhere (see `ui-rules.md`)
- No inline `style={{}}` except for dynamic map-pin colours coming from `category.colorHex`

---

## Leaflet (Map)

```typescript
// components/map/ReportMap.tsx
<MapContainer center={[16.8409, 96.1735]} zoom={13} className="h-full w-full">
  <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
  <MarkerClusterGroup>
    {pins.map(pin => (
      <Marker key={pin.id} position={[pin.latitude, pin.longitude]} icon={iconFor(pin)}>
        <Popup>{/* report code, category, status badge, link to detail */}</Popup>
      </Marker>
    ))}
  </MarkerClusterGroup>
</MapContainer>
```

Rules:
- Pin colour comes from the category, pin shape/badge from the status
- Clustering is required on the staff full-screen map view
- The map fetches from `/api/reports/map` (slim payload), never `/api/reports`
- Re-fetch on filter change and on map bounds change (debounced ~400 ms)
- GPS capture on the citizen report form uses `navigator.geolocation.getCurrentPosition`, with a draggable marker fallback if permission is denied

---

## Recharts (Dashboard Graphs)

| Chart | Where | Data source |
|-------|-------|-------------|
| Bar chart — monthly reports per department | Staff dashboard | `/api/dashboard/staff` |
| Pie chart — workload share per department | Departments page | `/api/dashboard/departments` |
| Bar chart — issue volume by category | Admin dashboard | `/api/dashboard/categories` |
| Line chart — average resolution time trend | Departments page | `/api/dashboard/departments` |

Charts must handle the empty state ("No data available for the selected period") rather than rendering a blank box.
