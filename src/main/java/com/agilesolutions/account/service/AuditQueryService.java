// service/AuditQueryService.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.dto.AuditLogResponseDto;
import com.agilesolutions.account.domain.dto.PagedResponseDto;
import com.agilesolutions.account.domain.entity.AuditLog;
import com.agilesolutions.account.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public PagedResponseDto<AuditLogResponseDto> getAuditLogByAccount(
            String accountId, Pageable pageable) {
        Page<AuditLog> page =
                auditLogRepository.findByEntityTypeAndEntityId("ACCOUNT", accountId, pageable);
        return toPagedResponse(page);
    }

    public PagedResponseDto<AuditLogResponseDto> getAuditLogByDateRange(
            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<AuditLog> page =
                auditLogRepository.findByChangedAtBetween(from, to, pageable);
        return toPagedResponse(page);
    }

    public PagedResponseDto<AuditLogResponseDto> getAuditLogByUser(
            String username, Pageable pageable) {
        Page<AuditLog> page =
                auditLogRepository.findByChangedBy(username, pageable);
        return toPagedResponse(page);
    }

    private PagedResponseDto<AuditLogResponseDto> toPagedResponse(Page<AuditLog> page) {
        List<AuditLogResponseDto> content = page.getContent()
                .stream()
                .map(this::toDto)
                .toList();

        return PagedResponseDto.<AuditLogResponseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private AuditLogResponseDto toDto(AuditLog log) {
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .changedBy(log.getChangedBy())
                .changedAt(log.getChangedAt())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .build();
    }
}