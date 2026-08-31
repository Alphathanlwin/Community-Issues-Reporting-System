package com.uit.scirs.common.config;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.feedback.entity.Feedback;
import com.uit.scirs.feedback.repository.FeedbackRepository;
import com.uit.scirs.notification.entity.Notification;
import com.uit.scirs.notification.entity.NotificationType;
import com.uit.scirs.notification.repository.NotificationRepository;
import com.uit.scirs.report.entity.ImageType;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.report.entity.ReportComment;
import com.uit.scirs.report.entity.ReportImage;
import com.uit.scirs.report.entity.ReportPriority;
import com.uit.scirs.report.entity.ReportStatus;
import com.uit.scirs.report.entity.ReportStatusHistory;
import com.uit.scirs.report.repository.ReportCommentRepository;
import com.uit.scirs.report.repository.ReportImageRepository;
import com.uit.scirs.report.repository.ReportRepository;
import com.uit.scirs.report.repository.ReportStatusHistoryRepository;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.entity.PointTransaction;
import com.uit.scirs.score.repository.PointTransactionRepository;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo/mock data for local development — realistic citizens, staff, and
 * reports across every status so the frontend has something to render.
 *
 * Disabled by default. Enable once with `app.mock-data.enabled=true`
 * (e.g. `SEED_MOCK_DATA=true` env var) and run the app a single time; it is
 * idempotent (guarded by a marker citizen email) so re-running is harmless.
 * Never enable this against a real production database.
 */
@Component
@Order(2)
public class MockDataSeeder implements CommandLineRunner {

    // Presence of the first mock report is the completion marker — citizens/staff
    // are created individually idempotent (skip-if-exists) so a run interrupted
    // partway through (e.g. by a transient DB error) can simply be re-run.
    private static final String MARKER_REPORT_CODE = "RPT-2026-000001";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final ReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final ReportStatusHistoryRepository statusHistoryRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationRepository notificationRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public MockDataSeeder(RoleRepository roleRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           CategoryRepository categoryRepository,
                           ReportRepository reportRepository,
                           ReportImageRepository reportImageRepository,
                           ReportStatusHistoryRepository statusHistoryRepository,
                           ReportCommentRepository reportCommentRepository,
                           FeedbackRepository feedbackRepository,
                           NotificationRepository notificationRepository,
                           PointTransactionRepository pointTransactionRepository,
                           PasswordEncoder passwordEncoder,
                           @org.springframework.beans.factory.annotation.Value("${app.mock-data.enabled:false}") boolean enabled) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.reportRepository = reportRepository;
        this.reportImageRepository = reportImageRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.reportCommentRepository = reportCommentRepository;
        this.feedbackRepository = feedbackRepository;
        this.notificationRepository = notificationRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || reportRepository.existsByReportCode(MARKER_REPORT_CODE)) {
            return;
        }

        Role citizenRole = roleRepository.findByName(RoleName.CITIZEN).orElseThrow();
        Role staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();
        User admin = userRepository.findByEmail("admin@scirs.gov").orElseThrow(
                () -> new IllegalStateException("DataSeeder must run before MockDataSeeder"));

        Map<String, Department> departments = new HashMap<>();
        departmentRepository.findAll().forEach(d -> departments.put(d.getName(), d));

        Map<String, Category> categories = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categories.put(c.getName(), c));

        List<User> citizens = seedCitizens(citizenRole);
        Map<String, User> staffByDepartment = seedStaff(staffRole, departments);

        seedReports(admin, citizens, staffByDepartment, categories);
    }

    // ---------------------------------------------------------------- Citizens --

    private List<User> seedCitizens(Role citizenRole) {
        record CitizenSpec(String name, String email, String phone, String dob, String nrc, AccountStatus status) {
        }

        List<CitizenSpec> specs = List.of(
                new CitizenSpec("Nyein Chan Aung", "citizen1@example.com", "+95912345001", "1998-03-14", "12/YAKANA(N)123456", AccountStatus.APPROVED),
                new CitizenSpec("Su Su Hlaing", "citizen2@example.com", "+95912345002", "1995-07-22", "12/MAHASA(N)234567", AccountStatus.APPROVED),
                new CitizenSpec("Kyaw Zin Thant", "citizen3@example.com", "+95912345003", "2000-11-05", "9/PATHATA(N)345678", AccountStatus.APPROVED),
                new CitizenSpec("Hnin Wai Yan", "citizen4@example.com", "+95912345004", "1992-01-30", "12/AHLANA(N)456789", AccountStatus.APPROVED),
                new CitizenSpec("Thiha Zaw", "citizen5@example.com", "+95912345005", "1999-09-09", "7/YAKAKHA(N)567890", AccountStatus.APPROVED),
                new CitizenSpec("Ei Ei Phyo", "citizen6@example.com", "+95912345006", "1997-05-18", "12/DAGANA(N)678901", AccountStatus.APPROVED),
                new CitizenSpec("Aung Kyaw Min", "citizen7@example.com", "+95912345007", "1994-12-02", "5/MAYAKA(N)789012", AccountStatus.APPROVED),
                new CitizenSpec("May Thazin", "citizen8@example.com", "+95912345008", "2001-04-27", "12/OUKATA(N)890123", AccountStatus.PENDING),
                new CitizenSpec("Zaw Zaw Oo", "citizen9@example.com", "+95912345009", "1990-08-16", "12/TAMANA(N)901234", AccountStatus.SUSPENDED)
        );

        List<User> saved = new ArrayList<>();
        for (CitizenSpec s : specs) {
            User existing = userRepository.findByEmail(s.email()).orElse(null);
            if (existing != null) {
                saved.add(existing);
                continue;
            }
            User u = new User();
            u.setFullName(s.name());
            u.setEmail(s.email());
            u.setPhone(s.phone());
            u.setPasswordHash(passwordEncoder.encode("Citizen@12345"));
            u.setRole(citizenRole);
            u.setAccountStatus(s.status());
            u.setDateOfBirth(LocalDate.parse(s.dob()));
            u.setNrcNumber(s.nrc());
            u.setActive(true);
            saved.add(userRepository.save(u));
        }
        return saved;
    }

    // ------------------------------------------------------------------- Staff --

    private Map<String, User> seedStaff(Role staffRole, Map<String, Department> departments) {
        record StaffSpec(String name, String email, String phone, String department) {
        }

        List<StaffSpec> specs = List.of(
                new StaffSpec("Mya Mya Win", "staff.electricity@scirs.gov", "+95912346001", "Electricity"),
                new StaffSpec("Ko Ko Lwin", "staff.roads@scirs.gov", "+95912346002", "Roads"),
                new StaffSpec("Thet Htar San", "staff.water@scirs.gov", "+95912346003", "Water"),
                new StaffSpec("Zaw Myo Aung", "staff.sanitation@scirs.gov", "+95912346004", "Sanitation"),
                new StaffSpec("Nandar Hlaing", "staff.parks@scirs.gov", "+95912346005", "Parks"),
                new StaffSpec("Aung Aung", "staff.buildings@scirs.gov", "+95912346006", "Buildings")
        );

        Map<String, User> byDepartment = new HashMap<>();
        for (StaffSpec s : specs) {
            User existing = userRepository.findByEmail(s.email()).orElse(null);
            if (existing != null) {
                byDepartment.put(s.department(), existing);
                continue;
            }
            User u = new User();
            u.setFullName(s.name());
            u.setEmail(s.email());
            u.setPhone(s.phone());
            u.setPasswordHash(passwordEncoder.encode("Staff@12345"));
            u.setRole(staffRole);
            u.setAccountStatus(AccountStatus.APPROVED);
            u.setDepartmentId(departments.get(s.department()).getId());
            u.setActive(true);
            byDepartment.put(s.department(), userRepository.save(u));
        }
        return byDepartment;
    }

    // ----------------------------------------------------------------- Reports --

    private record ReportSpec(String title, String description, String category, int reporterIdx,
                               double lat, double lng, String address, ReportPriority priority,
                               ReportStatus status, String rejectionReason) {
    }

    private void seedReports(User admin, List<User> citizens, Map<String, User> staffByDepartment,
                              Map<String, Category> categories) {

        List<ReportSpec> specs = List.of(
                // -- PENDING_APPROVAL (awaiting admin review) --
                new ReportSpec("Streetlight out on Pyay Road", "The streetlight near the Hledan junction has been off for a week, making the crossing dangerous at night.", "Street Lighting / Power Outage", 0, 16.8265, 96.1296, "Pyay Road, Kamayut", ReportPriority.HIGH, ReportStatus.PENDING_APPROVAL, null),
                new ReportSpec("Large pothole near Thanlyin bridge approach", "A deep pothole has formed on the approach road, already caused two motorbike falls.", "Pothole / Damaged Road", 1, 16.7250, 96.2586, "Thanlyin Bridge Approach", ReportPriority.URGENT, ReportStatus.PENDING_APPROVAL, null),
                new ReportSpec("Overflowing drain behind 42nd Street", "Drainage water has been pooling for days and starting to smell.", "Water Leakage / Drainage", 2, 16.7756, 96.1614, "42nd Street, Botahtaung", ReportPriority.NORMAL, ReportStatus.PENDING_APPROVAL, null),
                new ReportSpec("Garbage not collected for a week", "Bins along the street are overflowing and attracting stray dogs.", "Garbage / Sanitation", 3, 16.8661, 96.1951, "Yuzana Garden Street, North Okkalapa", ReportPriority.NORMAL, ReportStatus.PENDING_APPROVAL, null),
                new ReportSpec("Broken swing at Kandawgyi playground", "One of the children's swings is broken and has a sharp edge exposed.", "Park & Public Space", 4, 16.8143, 96.1660, "Kandawgyi Park", ReportPriority.LOW, ReportStatus.PENDING_APPROVAL, null),

                // -- REJECTED --
                new ReportSpec("Noisy air conditioner next door", "Neighbour's AC unit is loud at night.", "Damaged Public Building", 5, 16.8010, 96.1420, "Golden Valley, Bahan", ReportPriority.LOW, ReportStatus.REJECTED, "Not a public infrastructure issue — please contact ward administration for neighbour disputes."),
                new ReportSpec("Slow wifi at community library", "Wifi speed at the public library is too slow.", "Damaged Public Building", 6, 16.8480, 96.1350, "Community Library, Insein", ReportPriority.LOW, ReportStatus.REJECTED, "Out of scope for this system — not a physical infrastructure defect."),
                new ReportSpec("Duplicate pothole report", "Same pothole already reported by another citizen this week.", "Pothole / Damaged Road", 0, 16.8398, 96.1381, "Bogyoke Road, Sanchaung", ReportPriority.NORMAL, ReportStatus.REJECTED, "Duplicate of an existing open report (RPT-2026-000002)."),

                // -- ASSIGNED (approved, routed, awaiting staff pickup) --
                new ReportSpec("Power outage across whole block", "No electricity since last night across the entire block of 5 buildings.", "Street Lighting / Power Outage", 1, 16.7967, 96.1550, "Thin Gan Gyun, Tarmwe", ReportPriority.URGENT, ReportStatus.ASSIGNED, null),
                new ReportSpec("Cracked pavement outside school", "Pavement slabs are cracked and lifting, a trip hazard for schoolchildren.", "Pothole / Damaged Road", 2, 16.8556, 96.1279, "Basic Education School Road, Hlaing", ReportPriority.HIGH, ReportStatus.ASSIGNED, null),
                new ReportSpec("Leaking water pipe flooding footpath", "A burst pipe is flooding the footpath along the main road.", "Water Leakage / Drainage", 3, 16.7890, 96.1720, "Strand Road, Botahtaung", ReportPriority.HIGH, ReportStatus.ASSIGNED, null),
                new ReportSpec("Illegal dumping near market", "Construction debris dumped illegally beside the wet market entrance.", "Garbage / Sanitation", 4, 16.7712, 96.1601, "Thein Gyi Market, Latha", ReportPriority.NORMAL, ReportStatus.ASSIGNED, null),
                new ReportSpec("Damaged fence at People's Park", "The perimeter fence has collapsed after recent storms.", "Park & Public Space", 5, 16.7960, 96.1390, "People's Park, Dagon", ReportPriority.NORMAL, ReportStatus.ASSIGNED, null),
                new ReportSpec("Crumbling ceiling at township office", "Plaster is falling from the ceiling in the public waiting hall.", "Damaged Public Building", 6, 16.8710, 96.1550, "Township Administration Office, Insein", ReportPriority.HIGH, ReportStatus.ASSIGNED, null),

                // -- IN_PROGRESS (staff actively working) --
                new ReportSpec("Flickering streetlights along university avenue", "Multiple streetlights flicker on and off, likely a wiring fault.", "Street Lighting / Power Outage", 0, 16.8460, 96.1330, "University Avenue Road, Kamayut", ReportPriority.NORMAL, ReportStatus.IN_PROGRESS, null),
                new ReportSpec("Sinkhole forming on side street", "A small sinkhole is growing after heavy rain, needs urgent patching.", "Pothole / Damaged Road", 1, 16.8300, 96.1550, "Inya Road, Bahan", ReportPriority.URGENT, ReportStatus.IN_PROGRESS, null),
                new ReportSpec("Blocked storm drain causing flooding", "Storm drain is fully blocked with debris, road floods every time it rains.", "Water Leakage / Drainage", 2, 16.8100, 96.1180, "Baho Road, Sanchaung", ReportPriority.HIGH, ReportStatus.IN_PROGRESS, null),
                new ReportSpec("Overflowing public bins at bus stop", "Public bins near the bus stop overflow daily.", "Garbage / Sanitation", 3, 16.7830, 96.1500, "Merchant Street, Pabedan", ReportPriority.NORMAL, ReportStatus.IN_PROGRESS, null),
                new ReportSpec("Broken bench and litter at riverside park", "Several benches are broken and litter has piled up.", "Park & Public Space", 4, 16.7700, 96.1650, "Riverside Park, Botahtaung", ReportPriority.LOW, ReportStatus.IN_PROGRESS, null),
                new ReportSpec("Damaged staircase at public clinic", "Concrete staircase steps are cracked and unsafe for elderly patients.", "Damaged Public Building", 5, 16.8600, 96.1700, "North Okkalapa Public Clinic", ReportPriority.HIGH, ReportStatus.IN_PROGRESS, null),

                // -- RESOLVED (fixed, awaiting citizen close-out / feedback) --
                new ReportSpec("Dark alley near bus stop now lit", "Streetlight was out for two weeks near the bus stop, reported for safety.", "Street Lighting / Power Outage", 6, 16.8330, 96.1410, "Yankin Road, Yankin", ReportPriority.HIGH, ReportStatus.RESOLVED, null),
                new ReportSpec("Pothole on main junction repaired", "Deep pothole at the junction was causing traffic slowdowns.", "Pothole / Damaged Road", 0, 16.8050, 96.1500, "Kabar Aye Pagoda Road, Bahan", ReportPriority.HIGH, ReportStatus.RESOLVED, null),
                new ReportSpec("Leaking pipe near school fixed", "Pipe leak was flooding the school entrance during rainy season.", "Water Leakage / Drainage", 1, 16.8480, 96.1740, "Thingangyun Road, Thingangyun", ReportPriority.NORMAL, ReportStatus.RESOLVED, null),
                new ReportSpec("Garbage collection restored on schedule", "Garbage had piled up for over a week before this report.", "Garbage / Sanitation", 2, 16.8600, 96.2000, "North Okkalapa Ward 3", ReportPriority.NORMAL, ReportStatus.RESOLVED, null),
                new ReportSpec("Playground equipment repaired", "Broken slide at the neighbourhood playground has been fixed.", "Park & Public Space", 3, 16.8180, 96.1600, "Aung San Stadium Park, Mingalar Taung Nyunt", ReportPriority.LOW, ReportStatus.RESOLVED, null),
                new ReportSpec("Public restroom repairs completed", "Plumbing and door lock at the public restroom were repaired.", "Damaged Public Building", 4, 16.7950, 96.1450, "City Hall Public Restroom, Kyauktada", ReportPriority.NORMAL, ReportStatus.RESOLVED, null),

                // -- CLOSED (confirmed fixed, terminal) --
                new ReportSpec("Faulty transformer replaced", "Old transformer was sparking intermittently at night.", "Street Lighting / Power Outage", 5, 16.8390, 96.1210, "Hlaing Campus Road, Hlaing", ReportPriority.URGENT, ReportStatus.CLOSED, null),
                new ReportSpec("Road resurfacing completed", "Damaged road surface near the roundabout has been resurfaced.", "Pothole / Damaged Road", 6, 16.7980, 96.1650, "Tarmwe Roundabout, Tarmwe", ReportPriority.HIGH, ReportStatus.CLOSED, null),
                new ReportSpec("Drainage cleared before rainy season", "Clogged drain was fully cleared and inspected.", "Water Leakage / Drainage", 0, 16.8700, 96.1500, "Insein Road, Insein", ReportPriority.NORMAL, ReportStatus.CLOSED, null),
                new ReportSpec("New bins installed at market", "Extra bins were installed after repeated overflow complaints.", "Garbage / Sanitation", 1, 16.7770, 96.1580, "Latha Market, Latha", ReportPriority.NORMAL, ReportStatus.CLOSED, null)
        );

        for (int i = 0; i < specs.size(); i++) {
            createReport(specs.get(i), i + 1, admin, citizens, staffByDepartment, categories);
        }
    }

    private void createReport(ReportSpec spec, int sequence, User admin, List<User> citizens,
                               Map<String, User> staffByDepartment, Map<String, Category> categories) {

        Category category = categories.get(spec.category());
        Department department = category.getDepartment();
        User reporter = citizens.get(spec.reporterIdx());
        User staff = staffByDepartment.get(department.getName());

        Report report = new Report();
        String reportCode = String.format("RPT-2026-%06d", sequence);
        while (reportRepository.existsByReportCode(reportCode)) {
            sequence++;
            reportCode = String.format("RPT-2026-%06d", sequence);
        }
        report.setReportCode(reportCode);
        report.setTitle(spec.title());
        report.setDescription(spec.description());
        report.setCategory(category);
        report.setReporter(reporter);
        report.setLatitude(BigDecimal.valueOf(spec.lat()));
        report.setLongitude(BigDecimal.valueOf(spec.lng()));
        report.setAddressText(spec.address());
        report.setPriority(spec.priority());
        report.setStatus(ReportStatus.PENDING_APPROVAL);

        boolean routed = spec.status() != ReportStatus.PENDING_APPROVAL && spec.status() != ReportStatus.REJECTED;
        boolean assignedStaff = spec.status() == ReportStatus.IN_PROGRESS
                || spec.status() == ReportStatus.RESOLVED || spec.status() == ReportStatus.CLOSED;

        if (routed) {
            report.setDepartment(department);
            report.setApprovedAt(java.time.LocalDateTime.now());
            report.setApprovedBy(admin);
        }
        if (assignedStaff) {
            report.setAssignedStaff(staff);
        }
        if (spec.status() == ReportStatus.RESOLVED || spec.status() == ReportStatus.CLOSED) {
            report.setResolvedAt(java.time.LocalDateTime.now());
        }
        if (spec.status() == ReportStatus.CLOSED) {
            report.setClosedAt(java.time.LocalDateTime.now());
        }
        if (spec.status() == ReportStatus.REJECTED) {
            report.setRejectionReason(spec.rejectionReason());
        }
        report.setStatus(spec.status());

        report = reportRepository.save(report);

        // Report photo — always present, uploaded by the citizen at creation.
        ReportImage photo = new ReportImage();
        photo.setReport(report);
        photo.setImageUrl("https://picsum.photos/seed/scirs-" + sequence + "/800/600");
        photo.setImageType(ImageType.REPORT_PHOTO);
        photo.setUploadedBy(reporter);
        reportImageRepository.save(photo);

        // Status history — mirrors the transitions ReportWorkflowService would have recorded.
        addHistory(report, null, ReportStatus.PENDING_APPROVAL, reporter, null);

        if (spec.status() == ReportStatus.REJECTED) {
            addHistory(report, ReportStatus.PENDING_APPROVAL, ReportStatus.REJECTED, admin, spec.rejectionReason());
        } else if (routed) {
            addHistory(report, ReportStatus.PENDING_APPROVAL, ReportStatus.ASSIGNED, admin, "Approved and routed to " + department.getName());

            if (assignedStaff) {
                addHistory(report, ReportStatus.ASSIGNED, ReportStatus.IN_PROGRESS, staff, "Picked up for investigation.");
            }
            if (spec.status() == ReportStatus.RESOLVED || spec.status() == ReportStatus.CLOSED) {
                ReportImage resolutionPhoto = new ReportImage();
                resolutionPhoto.setReport(report);
                resolutionPhoto.setImageUrl("https://picsum.photos/seed/scirs-" + sequence + "-fixed/800/600");
                resolutionPhoto.setImageType(ImageType.RESOLUTION_PHOTO);
                resolutionPhoto.setUploadedBy(staff);
                reportImageRepository.save(resolutionPhoto);

                addHistory(report, ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED, staff, "Fix completed, resolution photo attached.");
            }
            if (spec.status() == ReportStatus.CLOSED) {
                addHistory(report, ReportStatus.RESOLVED, ReportStatus.CLOSED, admin, "Citizen confirmed the issue is resolved.");
            }
        }

        // Internal staff comment for anything past approval.
        if (routed) {
            ReportComment comment = new ReportComment();
            comment.setReport(report);
            comment.setAuthor(staff);
            comment.setBody("Site visit scheduled — coordinating with " + department.getName() + " crew.");
            reportCommentRepository.save(comment);
        }

        // Feedback — only on resolved/closed reports, and only some of them
        // (so the frontend also shows the "give feedback" empty state).
        boolean giveFeedback = (spec.status() == ReportStatus.RESOLVED || spec.status() == ReportStatus.CLOSED)
                && sequence % 2 == 0;
        if (giveFeedback) {
            Feedback feedback = new Feedback();
            feedback.setReport(report);
            feedback.setCitizen(reporter);
            feedback.setRating(3 + (sequence % 3));
            feedback.setComment("Thanks for the quick fix, much appreciated!");
            feedbackRepository.save(feedback);
            awardPoints(reporter, report, 5, PointReason.FEEDBACK_GIVEN);
        }

        // Notifications mirroring what NotificationService would have sent.
        notify(reporter, report, NotificationType.STATUS_CHANGED,
                "Report " + report.getReportCode() + " update",
                "Your report \"" + report.getTitle() + "\" is now " + spec.status().name() + ".",
                sequence % 3 != 0);

        if (routed) {
            notify(staff, report, NotificationType.NEW_REPORT,
                    "New report routed to " + department.getName(),
                    "\"" + report.getTitle() + "\" was assigned to your department.", sequence % 2 == 0);
        }
        if (spec.priority() == ReportPriority.URGENT && routed) {
            notify(staff, report, NotificationType.URGENT_REPORT,
                    "Urgent report needs attention",
                    "\"" + report.getTitle() + "\" is marked URGENT and awaiting action.", false);
        }
        if (spec.status() == ReportStatus.REJECTED) {
            notify(reporter, report, NotificationType.REPORT_REJECTED,
                    "Report rejected", "Your report was rejected: " + spec.rejectionReason(), sequence % 2 == 0);
        }
        if (routed) {
            notify(reporter, report, NotificationType.REPORT_APPROVED,
                    "Report approved", "Your report \"" + report.getTitle() + "\" was approved and routed to "
                            + department.getName() + ".", true);
        }
        if (spec.status() == ReportStatus.RESOLVED || spec.status() == ReportStatus.CLOSED) {
            notify(reporter, report, NotificationType.REPORT_COMPLETED,
                    "Report resolved", "Your report \"" + report.getTitle() + "\" has been marked resolved.",
                    sequence % 2 != 0);
        }
        if (spec.status() == ReportStatus.IN_PROGRESS && sequence % 5 == 0) {
            notify(staff, report, NotificationType.REPORT_WAITING_TOO_LONG,
                    "Report waiting too long", "\"" + report.getTitle() + "\" has been open past the SLA window.", false);
        }

        // Point ledger — mirrors PointReason business rules.
        if (spec.status() == ReportStatus.REJECTED) {
            awardPoints(reporter, report, -5, PointReason.REPORT_REJECTED);
        } else if (routed) {
            awardPoints(reporter, report, 10, PointReason.REPORT_APPROVED);
            if (spec.status() == ReportStatus.RESOLVED || spec.status() == ReportStatus.CLOSED) {
                awardPoints(reporter, report, 20, PointReason.REPORT_RESOLVED);
            }
        }
    }

    private void addHistory(Report report, ReportStatus oldStatus, ReportStatus newStatus, User changedBy, String remarks) {
        ReportStatusHistory history = new ReportStatusHistory();
        history.setReport(report);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setRemarks(remarks);
        statusHistoryRepository.save(history);
    }

    private void notify(User recipient, Report report, NotificationType type, String title, String message, boolean read) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setReport(report);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRead(read);
        notificationRepository.save(n);
    }

    private void awardPoints(User user, Report report, int points, PointReason reason) {
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setReport(report);
        tx.setPoints(points);
        tx.setReason(reason);
        pointTransactionRepository.save(tx);

        user.setScorePoints(pointTransactionRepository.sumPointsByUserId(user.getId()));
        userRepository.save(user);
    }
}
