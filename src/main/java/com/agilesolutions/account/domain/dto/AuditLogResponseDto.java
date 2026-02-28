// dto/AuditLogResponseDto.java
package com.agilesolutions.account.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit log entry response")
public class AuditLogResponseDto {

    @Schema(description = "Audit log entry ID")
    private Long id;

    @Schema(description = "Entity type (e.g. ACCOUNT)")
    private String entityType;

    @Schema(description = "Entity ID (account number)")
    private String entityId;

    @Schema(description = "Action performed: CREATE, UPDATE, DELETE")
    private String action;

    @Schema(description = "User who made the change")
    private String changedBy;

    @Schema(description = "Timestamp of change")
    private LocalDateTime changedAt;

    @Schema(description = "Previous state (JSON)")
    private String oldValue;

    @Schema(description = "New state (JSON)")
    private String newValue;

    @Schema(description = "IP address of requester")
    private String ipAddress;
}