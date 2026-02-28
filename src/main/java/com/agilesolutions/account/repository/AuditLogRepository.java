// repository/AuditLogRepository.java
package com.agilesolutions.account.repository;

import com.agilesolutions.account.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityTypeAndEntityId(
            String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByChangedAtBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByChangedBy(String changedBy, Pageable pageable);
}