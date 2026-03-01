// controller/AccountController.java
package com.agilesolutions.account.controller;

import com.agilesolutions.account.domain.dto.*;
import com.agilesolutions.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing COBOL COACTUPC screen transactions as API endpoints
 *
 * COBOL transaction COACTUPC screens mapped to:
 *   - CICS SEND MAP (display)   -> GET endpoints
 *   - CICS RECEIVE MAP (input)  -> POST/PUT/PATCH endpoints
 *   - PF3 (Clear/Back)          -> Not needed (stateless REST)
 *   - PF4 (Update/Confirm)      -> PUT /accounts/{id}
 *   - ENTER key (Submit)        -> POST /accounts
 *   - PF5 (Delete/Deactivate)   -> DELETE /accounts/{id}
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management",
        description = "Account CRUD operations refactored from COBOL COACTUPC.cbl")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    // ─── COBOL: PROCESS-ENTER-KEY (CREATE) ──────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Create account",
            description = "Replaces COBOL PROCESS-ENTER-KEY paragraph + WRITE ACCTDAT"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (COBOL SEND-ERRMSG)"),
            @ApiResponse(responseCode = "409", description = "Account already exists"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> createAccount(
            @Valid @RequestBody AccountRequestDto requestDto) {

        log.info("POST /accounts - accountId={}", requestDto.getAccountId());
        AccountResponseDto response = accountService.createAccount(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Account created successfully", response));
    }

    // ─── COBOL: GET-ACCT-DATA (READ) ────────────────────────────────────────
    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(
            summary     = "Get account by ID",
            description = "Replaces COBOL GET-ACCT-DATA / READ ACCTDAT paragraph"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found (COBOL NOT-FOUND path)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> getAccount(
            @Parameter(description = "11-digit account ID", example = "00001001001")
            @PathVariable String accountId) {

        log.debug("GET /accounts/{}", accountId);
        AccountResponseDto response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponseDto.success("Account retrieved", response));
    }

    // ─── COBOL: PROCESS-ENTER-KEY (UPDATE) + REWRITE ACCTDAT ────────────────
    @PutMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Full account update",
            description = "Replaces COBOL UPDATE-ACCOUNT-INFO + REWRITE ACCTDAT paragraph"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict")
    })
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> updateAccount(
            @Parameter(description = "11-digit account ID", example = "00001001001")
            @PathVariable String accountId,
            @Valid @RequestBody AccountUpdateDto updateDto) {

        log.info("PUT /accounts/{}", accountId);
        AccountResponseDto response = accountService.updateAccount(accountId, updateDto);
        return ResponseEntity.ok(ApiResponseDto.success("Account updated successfully", response));
    }

    // ─── COBOL: PROCESS-ENTER-KEY (PARTIAL UPDATE / PF4 screen fields) ──────
    @PatchMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Partial account update",
            description = "Replaces COBOL screen-field-level change detection in PROCESS-ENTER-KEY"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account patched"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> patchAccount(
            @PathVariable String accountId,
            @RequestBody AccountUpdateDto updateDto) {

        log.info("PATCH /accounts/{}", accountId);
        AccountResponseDto response = accountService.updateAccount(accountId, updateDto);
        return ResponseEntity.ok(ApiResponseDto.success("Account patched successfully", response));
    }

    // ─── COBOL: ACCT-ACTIVE-STATUS = 'N' (logical delete) ───────────────────
    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Deactivate account",
            description = "Replaces COBOL logical delete: MOVE 'N' TO ACCT-ACTIVE-STATUS + REWRITE"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deactivated"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<Void> deactivateAccount(
            @Parameter(description = "11-digit account ID", example = "00001001001")
            @PathVariable String accountId) {

        log.info("DELETE /accounts/{}", accountId);
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    // ─── COBOL: Browse/Scroll through ACCTDAT (paginated list) ──────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(
            summary     = "List all accounts (paginated)",
            description = "Replaces COBOL START / READ NEXT browse logic on ACCTDAT"
    )
    public ResponseEntity<ApiResponseDto<PagedResponseDto<AccountResponseDto>>> getAllAccounts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "accountId")
            @RequestParam(defaultValue = "accountId") String sortBy,
            @Parameter(description = "Sort direction", example = "ASC")
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PagedResponseDto<AccountResponseDto> response =
                accountService.getAllAccounts(PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponseDto.success("Accounts retrieved", response));
    }

    // ─── COBOL: Conditional READ with key qualifiers ─────────────────────────
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(
            summary     = "Search accounts",
            description = "Multi-field search replacing COBOL keyed READ with qualifiers"
    )
    public ResponseEntity<ApiResponseDto<PagedResponseDto<AccountResponseDto>>> searchAccounts(
            @Parameter(description = "Account type: 1 or 2")
            @RequestParam(required = false) String accountType,
            @Parameter(description = "Active status: ACTIVE or INACTIVE")
            @RequestParam(required = false) String activeStatus,
            @Parameter(description = "Account name (partial match)")
            @RequestParam(required = false) String accountName,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "accountId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PagedResponseDto<AccountResponseDto> response =
                accountService.searchAccounts(
                        accountType, activeStatus, accountName,
                        PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponseDto.success("Search results", response));
    }

    // ─── COBOL: EVALUATE ACCT-OVER-LIMIT-IND = 'Y' ──────────────────────────
    @GetMapping("/over-limit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Get over-limit accounts",
            description = "Replaces COBOL EVALUATE ACCT-OVER-LIMIT-IND WHEN 'Y' branch"
    )
    public ResponseEntity<ApiResponseDto<PagedResponseDto<AccountResponseDto>>> getOverLimitAccounts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponseDto<AccountResponseDto> response =
                accountService.getOverLimitAccounts(
                        PageRequest.of(page, size, Sort.by("accountId")));

        return ResponseEntity.ok(ApiResponseDto.success("Over-limit accounts", response));
    }
}