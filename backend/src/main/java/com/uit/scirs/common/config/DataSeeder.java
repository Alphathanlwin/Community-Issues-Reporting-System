package com.uit.scirs.common.config;

import com.uit.scirs.category.entity.Category;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(RoleRepository roleRepository,
                       UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       CategoryRepository categoryRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:admin@scirs.gov}") String adminEmail,
                       @Value("${app.admin.password:Admin@12345}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedDepartments();
        seedCategories();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName, defaultDescription(roleName)));
            }
        }
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role must be seeded before the admin user"));

        User admin = new User();
        admin.setFullName("System Administrator");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setAccountStatus(AccountStatus.APPROVED);
        admin.setActive(true);

        userRepository.save(admin);
    }

    private String defaultDescription(RoleName roleName) {
        return switch (roleName) {
            case ADMIN -> "Full system access";
            case STAFF -> "Department-scoped console access";
            case CITIZEN -> "Self-registering report submitter";
        };
    }

    private void seedDepartments() {
        for (DepartmentSeed seed : DEPARTMENT_SEEDS) {
            if (departmentRepository.findByName(seed.name()).isPresent()) {
                continue;
            }

            Department department = seed.legacyName() != null
                    ? departmentRepository.findByName(seed.legacyName()).orElse(null)
                    : null;

            if (department == null) {
                department = new Department();
            }

            department.setName(seed.name());
            department.setDescription(seed.description());
            department.setContactEmail(seed.contactEmail());
            departmentRepository.save(department);
        }
    }

    private void seedCategories() {
        for (CategorySeed seed : CATEGORY_SEEDS) {
            if (categoryRepository.findByName(seed.name()).isPresent()) {
                continue;
            }

            Department department = departmentRepository.findByName(seed.departmentName())
                    .orElseThrow(() -> new IllegalStateException(
                            "Departments must be seeded before categories"));

            Category category = seed.legacyName() != null
                    ? categoryRepository.findByName(seed.legacyName()).orElse(null)
                    : null;

            if (category == null) {
                category = new Category();
            }

            category.setName(seed.name());
            category.setDescription(seed.description());
            category.setDepartment(department);
            category.setIcon(seed.icon());
            category.setColorHex(seed.colorHex());
            category.setSeverityWeight(seed.severityWeight());
            categoryRepository.save(category);
        }
    }

    // legacyName lets a pre-existing row (from before the YCDC-aligned rename) be renamed in
    // place instead of duplicated, so existing reports/staff/history keep their foreign keys.
    private record DepartmentSeed(String legacyName, String name, String description, String contactEmail) {
    }

    private static final List<DepartmentSeed> DEPARTMENT_SEEDS = List.of(
            new DepartmentSeed("Roads", "Roads & Bridges Department",
                    "Maintains roads, footpaths and bridges across the city.",
                    "roads.bridges@scirs.gov"),
            new DepartmentSeed("Buildings", "Buildings Department",
                    "Inspects and maintains public buildings and unsafe structures.",
                    "buildings@scirs.gov"),
            new DepartmentSeed("Water", "Water & Sanitation Department",
                    "Manages the piped water supply network and public sanitation facilities.",
                    "water.sanitation@scirs.gov"),
            new DepartmentSeed(null, "Drainage Management Department",
                    "Maintains storm drains and culverts to prevent street flooding.",
                    "drainage@scirs.gov"),
            new DepartmentSeed("Sanitation", "Urban Environmental Conservation & Cleansing Department",
                    "Handles waste collection, street cleaning and illegal dumping.",
                    "cleansing@scirs.gov"),
            new DepartmentSeed("Parks", "Playgrounds, Parks & Gardens Department",
                    "Maintains public parks, playgrounds and street trees.",
                    "parks.gardens@scirs.gov"),
            new DepartmentSeed("Electricity", "Yangon Electricity Supply Corporation (YESC)",
                    "Operates street lighting and the low-voltage power distribution network.",
                    "yesc@scirs.gov"));

    private record CategorySeed(String legacyName, String name, String description, String departmentName,
                                 String icon, String colorHex, int severityWeight) {
    }

    // severityWeight: 1 (minor) – 5 (severe) — feeds PriorityService's automatic
    // priority scoring on report approval (see report/service/PriorityService.java).
    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed("Pothole / Damaged Road", "Pothole / Damaged Road Surface",
                    "A pothole or other damage to the road surface.",
                    "Roads & Bridges Department", "road", "#F97316", 4),
            new CategorySeed(null, "Damaged Footpath or Pedestrian Bridge",
                    "Broken paving, missing slabs or a damaged pedestrian bridge.",
                    "Roads & Bridges Department", "footprints", "#FB923C", 3),
            new CategorySeed("Damaged Public Building", "Unsafe or Damaged Public Building",
                    "Structural damage or an unsafe condition in a public building.",
                    "Buildings Department", "building", "#6B7280", 4),
            new CategorySeed(null, "Illegal or Unsafe Construction",
                    "Construction work that is unpermitted or poses a safety risk.",
                    "Buildings Department", "hard-hat", "#9CA3AF", 3),
            new CategorySeed(null, "Water Pipe Leak or Burst Main",
                    "A leaking or burst water pipe affecting the public supply.",
                    "Water & Sanitation Department", "droplet", "#3B82F6", 5),
            new CategorySeed(null, "Water Supply Failure",
                    "No water supply reaching a property or area.",
                    "Water & Sanitation Department", "droplet-off", "#60A5FA", 4),
            new CategorySeed(null, "Blocked Drain or Clogged Culvert",
                    "A blocked storm drain or culvert restricting water flow.",
                    "Drainage Management Department", "waves", "#0EA5E9", 4),
            new CategorySeed(null, "Street Flooding",
                    "Standing or flowing water on a street after rainfall.",
                    "Drainage Management Department", "cloud-rain", "#0284C7", 5),
            new CategorySeed("Garbage / Sanitation", "Uncollected Garbage",
                    "Household or street waste that has not been collected on schedule.",
                    "Urban Environmental Conservation & Cleansing Department", "trash", "#22C55E", 3),
            new CategorySeed(null, "Illegal Dumping",
                    "Waste or debris dumped in an unauthorised public location.",
                    "Urban Environmental Conservation & Cleansing Department", "trash-2", "#15803D", 4),
            new CategorySeed("Park & Public Space", "Damaged Park or Playground Equipment",
                    "Broken or unsafe equipment in a public park or playground.",
                    "Playgrounds, Parks & Gardens Department", "tree", "#16A34A", 2),
            new CategorySeed(null, "Fallen Tree or Overgrown Vegetation",
                    "A fallen tree or vegetation obstructing a public area.",
                    "Playgrounds, Parks & Gardens Department", "tree-deciduous", "#4D7C0F", 3),
            new CategorySeed("Street Lighting / Power Outage", "Street Light Outage",
                    "A street light that is not working.",
                    "Yangon Electricity Supply Corporation (YESC)", "lightbulb", "#EAB308", 3),
            new CategorySeed(null, "Power Outage or Exposed Cable",
                    "A power outage or an exposed/damaged electrical cable.",
                    "Yangon Electricity Supply Corporation (YESC)", "bolt", "#CA8A04", 5));
}
