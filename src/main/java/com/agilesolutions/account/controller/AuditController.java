// controller/AuditController.java
package com.agilesolutions.account.controller;

import com.agilesolutions.account.domain.dto.ApiResponseDto;
import com.agilesolutions.account.domain.dto.AuditLogResponseDto;
import com.agilesolutions.account.domain.dto.PagedResponseDto;
import com.agilesolutions.account.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Audit log controller - exposes COBOL VSAM audit-trail as queryable REST API
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Log", description = "COBOL audit trail exposed as REST API")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/account/{accountId}")
    @Operation(
            summary     = "Get audit history for account",
            description = "Returns full audit trail for an account (replaces VSAM audit log browse)"
    )
    public ResponseEntity<ApiResponseDto<PagedResponseDto<AuditLogResponseDto>>> getAccountAuditLog(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponseDto<AuditLogResponseDto> result =
                auditQueryService.getAuditLogByAccount(
                        accountId, PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "changedAt")));

        return ResponseEntity.ok(ApiResponseDto.success("Audit log retrieved", result));
    }

    @GetMapping("/range")
    @Operation(summary = "Get audit log by date range")
    public ResponseEntity<ApiResponseDto<PagedResponseDto<AuditLogResponseDto>>> getAuditLogByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponseDto<AuditLogResponseDto> result =
                auditQueryService.getAuditLogByDateRange(
                        from, to, PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "changedAt")));

        return ResponseEntity.ok(ApiResponseDto.success("Audit log retrieved", result));
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "Get audit log by user")
    public ResponseEntity<ApiResponseDto<PagedResponseDto<AuditLogResponseDto>>> getAuditLogByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponseDto<AuditLogResponseDto> result =
                auditQueryService.getAuditLogByUser(
                        username, PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "changedAt")));

        return ResponseEntity.ok(ApiResponseDto.success("Audit log retrieved", result));
    }
}