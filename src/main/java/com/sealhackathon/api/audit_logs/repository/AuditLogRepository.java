package com.sealhackathon.api.audit_logs.repository;

import com.sealhackathon.api.audit_logs.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    Page<AuditLog> findByTargetTableAndTargetIdOrderByCreatedAtDesc(
            String targetTable, Integer targetId, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
