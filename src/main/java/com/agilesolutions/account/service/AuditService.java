// service/AuditService.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.entity.AuditLog;
import com.agilesolutions.account.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Audit service replaces COBOL commarea audit trail writing
 * to VSAM AUDIT-LOG file after each successful REWRITE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void logCreate(String entityType, String entityId, Account newEntity) {
        saveAuditLog(entityType, entityId, "CREATE", null, serializeAccount(newEntity));
    }

    public void logUpdate(String entityType, String entityId, String oldValue, Account newEntity) {
        saveAuditLog(entityType, entityId, "UPDATE", oldValue, serializeAccount(newEntity));
    }

    public void logDelete(String entityType, String entityId, String oldValue) {
        saveAuditLog(entityType, entityId, "DELETE", oldValue, null);
    }

    public String serializeAccount(Account account) {
        try {
            return objectMapper.writeValueAsString(account);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize account for audit: {}", e.getMessage());
            return "{}";
        }
    }

    private void saveAuditLog(
            String entityType, String entityId,
            String action, String oldValue, String newValue) {
        try {
            AuditLog log = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .changedBy(getCurrentUser())
                    .changedAt(LocalDateTime.now())
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            this.log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANONYMOUS";
    }
}