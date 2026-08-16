package com.uit.scirs.user.service;

import com.uit.scirs.auth.dto.UserDTO;
import com.uit.scirs.common.exception.BusinessRuleException;
import com.uit.scirs.common.exception.DuplicateResourceException;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.department.entity.Department;
import com.uit.scirs.department.repository.DepartmentRepository;
import com.uit.scirs.user.dto.CreateStaffDTO;
import com.uit.scirs.user.dto.RejectUserDTO;
import com.uit.scirs.user.dto.UpdateUserDTO;
import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.Role;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.mapper.UserMapper;
import com.uit.scirs.user.repository.RoleRepository;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    @Test
    void createStaff_withActiveDepartment_savesStaffWithApprovedStatus() {
        CreateStaffDTO dto = staffDto("staff@example.com", 2L);
        Department roads = department(2L, "Roads", true);
        Role staffRole = new Role(RoleName.STAFF, "Department-scoped console access");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(roads));
        when(roleRepository.findByName(RoleName.STAFF)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        UserDTO result = userService.createStaff(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        assertThat(captor.getValue().getDepartmentId()).isEqualTo(2L);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(result.getAccountStatus()).isEqualTo("APPROVED");
    }

    @Test
    void createStaff_withInactiveDepartment_throwsBusinessRuleException() {
        CreateStaffDTO dto = staffDto("staff@example.com", 2L);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department(2L, "Roads", false)));

        assertThatThrownBy(() -> userService.createStaff(dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createStaff_withUnknownDepartment_throwsResourceNotFoundException() {
        CreateStaffDTO dto = staffDto("staff@example.com", 99L);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createStaff(dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createStaff_withDuplicateEmail_throwsDuplicateResourceException() {
        CreateStaffDTO dto = staffDto("staff@example.com", 2L);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createStaff(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(departmentRepository, never()).findById(any());
    }

    @Test
    void approve_withPendingAccount_setsStatusApproved() {
        User citizen = citizen(1L, AccountStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        UserDTO result = userService.approve(1L);

        assertThat(result.getAccountStatus()).isEqualTo("APPROVED");
    }

    @Test
    void approve_withAlreadyApprovedAccount_throwsBusinessRuleException() {
        User citizen = citizen(1L, AccountStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> userService.approve(1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void reject_withPendingAccount_setsStatusRejected() {
        User citizen = citizen(1L, AccountStatus.PENDING);
        RejectUserDTO dto = new RejectUserDTO();
        dto.setReason("Invalid NRC document");

        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        UserDTO result = userService.reject(1L, dto);

        assertThat(result.getAccountStatus()).isEqualTo("REJECTED");
    }

    @Test
    void reject_withNonPendingAccount_throwsBusinessRuleException() {
        User citizen = citizen(1L, AccountStatus.APPROVED);
        RejectUserDTO dto = new RejectUserDTO();
        dto.setReason("Invalid NRC document");
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> userService.reject(1L, dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void suspend_withApprovedAccount_setsStatusSuspended() {
        User citizen = citizen(1L, AccountStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenAnswer(i -> dtoFor(i.getArgument(0)));

        UserDTO result = userService.suspend(1L);

        assertThat(result.getAccountStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void suspend_withPendingAccount_throwsBusinessRuleException() {
        User citizen = citizen(1L, AccountStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));

        assertThatThrownBy(() -> userService.suspend(1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_setsActiveFalseInsteadOfRemovingRow() {
        User citizen = citizen(1L, AccountStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.delete(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void getById_asAdmin_returnsAnyUsersRecord() {
        User citizen = citizen(5L, AccountStatus.APPROVED);
        CurrentUser admin = new CurrentUser(1L, "admin@scirs.gov", RoleName.ADMIN, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(citizen));
        when(userMapper.toDTO(citizen)).thenReturn(dtoFor(citizen));

        UserDTO result = userService.getById(5L, admin);

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void getById_asCitizenAccessingOwnRecord_returnsUser() {
        User citizen = citizen(5L, AccountStatus.APPROVED);
        CurrentUser self = new CurrentUser(5L, "citizen@example.com", RoleName.CITIZEN, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(citizen));
        when(userMapper.toDTO(citizen)).thenReturn(dtoFor(citizen));

        UserDTO result = userService.getById(5L, self);

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void getById_asCitizenAccessingAnotherUsersRecord_throwsAccessDeniedException() {
        CurrentUser other = new CurrentUser(6L, "other@example.com", RoleName.CITIZEN, null);

        assertThatThrownBy(() -> userService.getById(5L, other))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void update_withPhoneBelongingToAnotherUser_throwsDuplicateResourceException() {
        User citizen = citizen(5L, AccountStatus.APPROVED);
        citizen.setPhone("+959111111111");
        CurrentUser self = new CurrentUser(5L, "citizen@example.com", RoleName.CITIZEN, null);

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setFullName("Updated Name");
        dto.setPhone("+959222222222");

        when(userRepository.findById(5L)).thenReturn(Optional.of(citizen));
        when(userRepository.existsByPhone("+959222222222")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(5L, dto, self))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    private CreateStaffDTO staffDto(String email, Long departmentId) {
        CreateStaffDTO dto = new CreateStaffDTO();
        dto.setFullName("New Staff");
        dto.setEmail(email);
        dto.setPhone("+959123456789");
        dto.setPassword("securePass123");
        dto.setDepartmentId(departmentId);
        return dto;
    }

    private Department department(Long id, String name, boolean active) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setActive(active);
        return department;
    }

    private User citizen(Long id, AccountStatus status) {
        Role citizenRole = new Role(RoleName.CITIZEN, "Self-registering report submitter");
        User user = new User();
        user.setId(id);
        user.setFullName("Test Citizen");
        user.setEmail("citizen" + id + "@example.com");
        user.setRole(citizenRole);
        user.setAccountStatus(status);
        user.setActive(true);
        return user;
    }

    private UserDTO dtoFor(User entity) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setAccountStatus(entity.getAccountStatus().name());
        dto.setDepartmentId(entity.getDepartmentId());
        return dto;
    }
}
