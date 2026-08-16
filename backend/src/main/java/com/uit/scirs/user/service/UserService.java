package com.uit.scirs.user.service;

import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.auth.mapper.AuthMapper;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.notification.service.NotificationService;
import com.uit.scirs.user.dto.CreateStaffDTO;
import com.uit.scirs.user.dto.UpdateUserDTO;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        DepartmentRepository departmentRepository,
                        PasswordEncoder passwordEncoder,
                        AuthMapper authMapper,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authMapper = authMapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listUsers(RoleName role, AccountStatus status) {
        List<User> users;
        if (role != null && status != null) {
            users = userRepository.findByRoleName(role).stream()
                    .filter(user -> user.getAccountStatus() == status)
                    .toList();
        } else if (role != null) {
            users = userRepository.findByRoleName(role);
        } else if (status != null) {
            users = userRepository.findByAccountStatus(status);
        } else {
            users = userRepository.findAll();
        }
        return toDTOList(users);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listCitizens() {
        return toDTOList(userRepository.findByRoleName(RoleName.CITIZEN));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listStaff() {
        return toDTOList(userRepository.findByRoleName(RoleName.STAFF));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listPending() {
        return toDTOList(userRepository.findByRoleName(RoleName.CITIZEN).stream()
                .filter(user -> user.getAccountStatus() == AccountStatus.PENDING)
                .toList());
    }

    @Transactional
    public UserDTO createStaff(CreateStaffDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
        if (dto.getPhone() != null && userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateResourceException("An account with this phone number already exists.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + dto.getDepartmentId()));
        if (!department.isActive()) {
            throw new BusinessRuleException("Staff must be assigned to an active department");
        }

        Role staffRole = roleRepository.findByName(RoleName.STAFF)
                .orElseThrow(() -> new ResourceNotFoundException("STAFF role is not seeded"));

        User staff = new User();
        staff.setFullName(dto.getFullName());
        staff.setEmail(dto.getEmail());
        staff.setPhone(dto.getPhone());
        staff.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        staff.setRole(staffRole);
        staff.setAccountStatus(AccountStatus.APPROVED);
        staff.setDepartmentId(department.getId());

        User saved = userRepository.save(staff);
        notificationService.notifyAccountApproved(saved);
        return authMapper.toUserDTO(saved);
    }

    @Transactional
    public UserDTO approve(Long id) {
        User user = findEntity(id);
        user.setAccountStatus(AccountStatus.APPROVED);
        User saved = userRepository.save(user);
        notificationService.notifyAccountApproved(saved);
        return authMapper.toUserDTO(saved);
    }

    @Transactional
    public UserDTO reject(Long id, String reason) {
        User user = findEntity(id);
        user.setAccountStatus(AccountStatus.REJECTED);
        User saved = userRepository.save(user);
        notificationService.notifyAccountRejected(saved, reason);
        return authMapper.toUserDTO(saved);
    }

    @Transactional
    public UserDTO suspend(Long id) {
        User user = findEntity(id);
        user.setAccountStatus(AccountStatus.SUSPENDED);
        return authMapper.toUserDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDTO getUser(Long id, CurrentUser currentUser) {
        assertCanAccess(id, currentUser);
        return authMapper.toUserDTO(findEntity(id));
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserDTO dto, CurrentUser currentUser) {
        assertCanAccess(id, currentUser);
        User user = findEntity(id);
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        return authMapper.toUserDTO(userRepository.save(user));
    }

    @Transactional
    public void softDelete(Long id) {
        User user = findEntity(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private void assertCanAccess(Long targetUserId, CurrentUser currentUser) {
        boolean isAdmin = currentUser.getRole() == RoleName.ADMIN;
        boolean isSelf = currentUser.getId().equals(targetUserId);
        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You can only view or edit your own profile.");
        }
    }

    private List<UserDTO> toDTOList(List<User> users) {
        return users.stream().map(authMapper::toUserDTO).toList();
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
