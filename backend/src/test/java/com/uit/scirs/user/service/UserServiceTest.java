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
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthMapper authMapper;
    @Mock NotificationService notificationService;
    @InjectMocks UserService userService;

    @Test
    void createStaff_withoutActiveDepartment_throwsBusinessRuleException() {
        CreateStaffDTO dto = staffDto(5L);
        Department inactive = department(5L, false);

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> userService.createStaff(dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createStaff_withDuplicateEmail_throwsDuplicateResourceException() {
        CreateStaffDTO dto = staffDto(5L);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createStaff(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createStaff_withActiveDepartment_savesApprovedStaffAccount() {
        CreateStaffDTO dto = staffDto(5L);
        Department active = department(5L, true);
        Role staffRole = new Role(RoleName.STAFF, "Department-scoped console access");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(active));
        when(roleRepository.findByName(RoleName.STAFF)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(authMapper.toUserDTO(any(User.class))).thenReturn(new UserDTO());

        userService.createStaff(dto);

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        assertThat(captor.getValue().getDepartmentId()).isEqualTo(5L);
        verify(notificationService).notifyAccountApproved(captor.getValue());
    }

    @Test
    void approve_flipsStatusToApprovedAndNotifies() {
        User citizen = new User();
        citizen.setId(3L);
        citizen.setAccountStatus(AccountStatus.PENDING);

        when(userRepository.findById(3L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(authMapper.toUserDTO(any(User.class))).thenReturn(new UserDTO());

        userService.approve(3L);

        assertThat(citizen.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        verify(notificationService).notifyAccountApproved(citizen);
    }

    @Test
    void reject_flipsStatusToRejectedAndNotifiesWithReason() {
        User citizen = new User();
        citizen.setId(3L);
        citizen.setAccountStatus(AccountStatus.PENDING);

        when(userRepository.findById(3L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(authMapper.toUserDTO(any(User.class))).thenReturn(new UserDTO());

        userService.reject(3L, "Duplicate account");

        assertThat(citizen.getAccountStatus()).isEqualTo(AccountStatus.REJECTED);
        verify(notificationService).notifyAccountRejected(citizen, "Duplicate account");
    }

    @Test
    void getUser_whenCitizenRequestsAnotherUser_throwsAccessDeniedException() {
        // Ownership is checked before the record is even looked up, so no
        // repository stub is needed here.
        CurrentUser otherCitizen = new CurrentUser(99L, "other@example.com", RoleName.CITIZEN, null);

        assertThatThrownBy(() -> userService.getUser(3L, otherCitizen))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getUser_whenUnknownId_throwsResourceNotFoundException() {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());
        CurrentUser admin = new CurrentUser(1L, "admin@scirs.gov", RoleName.ADMIN, null);

        assertThatThrownBy(() -> userService.getUser(3L, admin))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateStaffDTO staffDto(Long departmentId) {
        CreateStaffDTO dto = new CreateStaffDTO();
        dto.setFullName("New Staff");
        dto.setEmail("staff@scirs.gov");
        dto.setPhone("+959123456789");
        dto.setPassword("securePass123");
        dto.setDepartmentId(departmentId);
        return dto;
    }

    private Department department(Long id, boolean active) {
        Department department = new Department();
        department.setId(id);
        department.setActive(active);
        return department;
    }
}
