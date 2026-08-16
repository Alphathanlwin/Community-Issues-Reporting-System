package com.uit.scirs.user.service;

import com.uit.scirs.auth.dto.UserDTO;
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
import com.uit.scirs.user.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        DepartmentRepository departmentRepository,
                        UserMapper userMapper,
                        PasswordEncoder passwordEncoder,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAll(RoleName role, AccountStatus accountStatus) {
        List<User> users;
        if (role != null && accountStatus != null) {
            users = userRepository.findByRoleNameAndAccountStatus(role, accountStatus);
        } else if (role != null) {
            users = userRepository.findByRoleName(role);
        } else if (accountStatus != null) {
            users = userRepository.findByAccountStatus(accountStatus);
        } else {
            users = userRepository.findAll();
        }
        return userMapper.toDTOList(users);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getCitizens() {
        return userMapper.toDTOList(userRepository.findByRoleName(RoleName.CITIZEN));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getStaff() {
        return userMapper.toDTOList(userRepository.findByRoleName(RoleName.STAFF));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getPending() {
        return userMapper.toDTOList(userRepository.findByAccountStatus(AccountStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public UserDTO getById(Long id, CurrentUser currentUser) {
        assertCanAccess(id, currentUser);
        return userMapper.toDTO(findEntity(id));
    }

    @Transactional
    public UserDTO createStaff(CreateStaffDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
        if (dto.getPhone() != null && userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateResourceException("An account with this phone number already exists.");
        }

        Department department = requireActiveDepartment(dto.getDepartmentId());

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
        return userMapper.toDTO(saved);
    }

    @Transactional
    public UserDTO approve(Long id) {
        User user = findEntity(id);
        if (user.getAccountStatus() != AccountStatus.PENDING) {
            throw new BusinessRuleException("Only pending accounts can be approved");
        }
        user.setAccountStatus(AccountStatus.APPROVED);
        User saved = userRepository.save(user);
        notificationService.notifyAccountApproved(saved);
        return userMapper.toDTO(saved);
    }

    @Transactional
    public UserDTO reject(Long id, String reason) {
        User user = findEntity(id);
        if (user.getAccountStatus() != AccountStatus.PENDING) {
            throw new BusinessRuleException("Only pending accounts can be rejected");
        }
        user.setAccountStatus(AccountStatus.REJECTED);
        User saved = userRepository.save(user);
        notificationService.notifyAccountRejected(saved, reason);
        return userMapper.toDTO(saved);
    }

    @Transactional
    public UserDTO suspend(Long id) {
        User user = findEntity(id);
        if (user.getAccountStatus() != AccountStatus.APPROVED) {
            throw new BusinessRuleException("Only approved accounts can be suspended");
        }
        user.setAccountStatus(AccountStatus.SUSPENDED);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO update(Long id, UpdateUserDTO dto, CurrentUser currentUser) {
        assertCanAccess(id, currentUser);
        User user = findEntity(id);

        if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateResourceException("An account with this phone number already exists.");
        }

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setProfileImageUrl(dto.getProfileImageUrl());

        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findEntity(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private void assertCanAccess(Long targetId, CurrentUser currentUser) {
        if (currentUser.getRole() != RoleName.ADMIN && !currentUser.getId().equals(targetId)) {
            throw new AccessDeniedException("You can only access your own account.");
        }
    }

    private Department requireActiveDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        if (!department.isActive()) {
            throw new BusinessRuleException("Staff must be assigned to an active department");
        }

        return department;
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
