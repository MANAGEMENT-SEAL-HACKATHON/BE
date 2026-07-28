package com.sealhackathon.api.audit_logs.service.impl;

import com.sealhackathon.api.audit_logs.entity.AuditLog;
import com.sealhackathon.api.audit_logs.repository.AuditLogRepository;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private HackathonRepository hackathonRepository;

    @InjectMocks private AuditLogServiceImpl service;

    @Test
    void list_byAction_returnsPageMap() {
        AuditLog log = AuditLog.builder().id(1).action("X").build();
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 50), 1);
        when(auditLogRepository.findByActionOrderByCreatedAtDesc(eq("X"), any(Pageable.class))).thenReturn(page);

        Map<String, Object> body = service.list(10, null, "X", 0, 50);

        assertThat(body.get("items")).isEqualTo(List.of(log));
        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("size")).isEqualTo(50);
        assertThat(body.get("total")).isEqualTo(1L);
        verify(auditLogRepository).findByActionOrderByCreatedAtDesc(eq("X"), any(Pageable.class));
    }

    @Test
    void list_byHackathonId_whenOwner_queriesTargetTable() {
        User owner = User.builder().id(10).build();
        Hackathon h = Hackathon.builder().id(5).createdBy(owner).build();
        when(hackathonRepository.findById(5)).thenReturn(Optional.of(h));
        Page<AuditLog> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(auditLogRepository.findByTargetTableAndTargetIdOrderByCreatedAtDesc(
                eq("hackathons"), eq(5), any(Pageable.class))).thenReturn(page);

        Map<String, Object> body = service.list(10, 5, null, 0, 20);

        assertThat(body.get("total")).isEqualTo(0L);
        verify(auditLogRepository).findByTargetTableAndTargetIdOrderByCreatedAtDesc(
                eq("hackathons"), eq(5), any(Pageable.class));
    }

    @Test
    void list_byHackathonId_whenNotOwner_throwsForbidden() {
        User other = User.builder().id(99).build();
        Hackathon h = Hackathon.builder().id(5).createdBy(other).build();
        when(hackathonRepository.findById(5)).thenReturn(Optional.of(h));

        assertThatThrownBy(() -> service.list(10, 5, null, 0, 50))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void list_default_byUserId() {
        Page<AuditLog> page = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(eq(10), any(Pageable.class))).thenReturn(page);

        Map<String, Object> body = service.list(10, null, null, 1, 10);

        assertThat(body.get("page")).isEqualTo(1);
        assertThat(body.get("size")).isEqualTo(10);
        verify(auditLogRepository).findByUserIdOrderByCreatedAtDesc(eq(10), any(Pageable.class));
    }
}
