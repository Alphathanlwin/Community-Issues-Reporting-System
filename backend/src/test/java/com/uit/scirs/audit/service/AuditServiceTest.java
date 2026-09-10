package com.uit.scirs.audit.service;

import com.uit.scirs.audit.dto.AuditLogDTO;
import com.uit.scirs.audit.entity.AuditAction;
import com.uit.scirs.audit.entity.AuditLog;
import com.uit.scirs.audit.mapper.AuditLogMapper;
import com.uit.scirs.audit.repository.AuditLogRepository;
import com.uit.scirs.common.dto.PageResponse;
import com.uit.scirs.common.security.CurrentUser;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock AuditLogWriter auditLogWriter;
    @Mock UserRepository userRepository;
    // A real mapper — no behaviour worth stubbing.
    AuditLogMapper auditLogMapper = new AuditLogMapper();
    AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, auditLogWriter, auditLogMapper, userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(long id, String email) {
        CurrentUser principal = new CurrentUser(id, email, RoleName.ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void record_stampsActorFromSecurityContextAndPersists() {
        authenticateAs(7L, "admin@scirs.gov");
        User admin = new User();
        admin.setFullName("System Administrator");
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        auditService.record(AuditAction.USER_APPROVED, "USER", 42L, "Hnin Ei Phyu (citizen)", "PENDING → APPROVED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).write(captor.capture());
        AuditLog entry = captor.getValue();
        assertThat(entry.getActorId()).isEqualTo(7L);
        assertThat(entry.getActorName()).isEqualTo("System Administrator");
        assertThat(entry.getActorEmail()).isEqualTo("admin@scirs.gov");
        assertThat(entry.getAction()).isEqualTo(AuditAction.USER_APPROVED);
        assertThat(entry.getTargetType()).isEqualTo("USER");
        assertThat(entry.getTargetId()).isEqualTo(42L);
        assertThat(entry.getTargetLabel()).isEqualTo("Hnin Ei Phyu (citizen)");
        assertThat(entry.getDetails()).isEqualTo("PENDING → APPROVED");
    }

    @Test
    void record_withNoAuthentication_recordsSystemActor() {
        auditService.record(AuditAction.DEPARTMENT_CREATED, "DEPARTMENT", 3L, "Parks & Recreation", "Department created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).write(captor.capture());
        assertThat(captor.getValue().getActorId()).isNull();
        assertThat(captor.getValue().getActorName()).isEqualTo("System");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void record_swallowsWriterFailure() {
        authenticateAs(1L, "admin@scirs.gov");
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        doThrow(new RuntimeException("db down")).when(auditLogWriter).write(any());

        // Must not propagate — auditing can never break the audited action.
        auditService.record(AuditAction.REPORT_APPROVED, "REPORT", 9L, "RPT-1 — x", "PENDING_APPROVAL → ASSIGNED");
    }

    @Test
    void record_clampsOverlongText() {
        authenticateAs(1L, "a@b.c");
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        String longLabel = "x".repeat(400);

        auditService.record(AuditAction.REPORT_STATUS_CHANGED, "REPORT", 1L, longLabel, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogWriter).write(captor.capture());
        assertThat(captor.getValue().getTargetLabel()).hasSize(300);
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    void search_bindsWildcardAndSentinelDatesWhenNoFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> empty = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.search(isNull(), isNull(), eq("%"), any(), any(), eq(pageable))).thenReturn(empty);

        PageResponse<AuditLogDTO> result = auditService.search(null, null, "  ", null, null, pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void search_lowercasesAndWrapsSearchTerm() {
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.search(eq(AuditAction.USER_APPROVED), eq(5L), eq("%hnin%"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        auditService.search(AuditAction.USER_APPROVED, 5L, "  Hnin ", null, null, pageable);

        verify(auditLogRepository).search(eq(AuditAction.USER_APPROVED), eq(5L), eq("%hnin%"), any(), any(), eq(pageable));
    }
}
