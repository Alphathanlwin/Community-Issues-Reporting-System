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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
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
        for (String name : DEPARTMENT_NAMES) {
            if (departmentRepository.findByName(name).isEmpty()) {
                Department department = new Department();
                department.setName(name);
                department.setDescription(name + " department");
                department.setContactEmail(name.toLowerCase() + "@scirs.gov");
                departmentRepository.save(department);
            }
        }
    }

    private void seedCategories() {
        CATEGORY_TO_DEPARTMENT.forEach((categoryName, spec) -> {
            if (categoryRepository.findByName(categoryName).isPresent()) {
                return;
            }

            Department department = departmentRepository.findByName(spec.departmentName())
                    .orElseThrow(() -> new IllegalStateException(
                            "Departments must be seeded before categories"));

            Category category = new Category();
            category.setName(categoryName);
            category.setDescription(categoryName);
            category.setDepartment(department);
            category.setIcon(spec.icon());
            category.setColorHex(spec.colorHex());
            categoryRepository.save(category);
        });
    }

    private static final List<String> DEPARTMENT_NAMES =
            List.of("Electricity", "Roads", "Water", "Sanitation", "Parks", "Buildings");

    private record CategorySeed(String departmentName, String icon, String colorHex) {
    }

    private static final Map<String, CategorySeed> CATEGORY_TO_DEPARTMENT = Map.of(
            "Street Lighting / Power Outage", new CategorySeed("Electricity", "bolt", "#EAB308"),
            "Pothole / Damaged Road", new CategorySeed("Roads", "road", "#F97316"),
            "Water Leakage / Drainage", new CategorySeed("Water", "droplet", "#3B82F6"),
            "Garbage / Sanitation", new CategorySeed("Sanitation", "trash", "#22C55E"),
            "Park & Public Space", new CategorySeed("Parks", "tree", "#16A34A"),
            "Damaged Public Building", new CategorySeed("Buildings", "building", "#6B7280"));
}
