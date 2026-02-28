// service/ValidationService.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.dto.AccountRequestDto;
import com.agilesolutions.account.domain.dto.AccountUpdateDto;
import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.exception.BusinessValidationException;
import com.agilesolutions.account.util.AccountConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation service refactored from COBOL paragraph:
 *
 *   EDIT-ACCOUNT-DATA
 *     EDIT-ACCTID
 *     EDIT-ACCT-TYPE
 *     EDIT-CREDIT-LIMIT
 *     EDIT-CASH-CREDIT-LIMIT
 *     EDIT-EXPIRY-DATE
 *     EDIT-REISSUE-DATE
 *     EDIT-OPEN-DATE
 *
 * COBOL used WS-ERROR-FLAGS and SEND-ERRMSG
 * Java uses collected violations -> BusinessValidationException
 */
@Service
@Slf4j
public class ValidationService {

    /**
     * Full validation on CREATE - mirrors COBOL EDIT-ACCOUNT-DATA paragraph
     */
    public void validateAccountData(AccountRequestDto dto) {
        List<String> errors = new ArrayList<>();

        // COBOL: EDIT-ACCTID - validate 11-digit numeric format
        validateAccountId(dto.getAccountId(), errors);

        // COBOL: EDIT-ACCT-TYPE
        validateAccountType(dto.getAccountType(), errors);

        // COBOL: EDIT-CREDIT-LIMIT
        validateCreditLimit(dto, errors);

        // COBOL: EDIT-EXPIRY-DATE / EDIT-OPEN-DATE
        validateDates(dto.getOpenDate(), dto.getExpiryDate(), dto.getReissueDate(), errors);

        throwIfErrors(errors);
    }

    /**
     * Partial validation on UPDATE - mirrors COBOL modified field detection
     */
    public void validateUpdateData(AccountUpdateDto dto, Account existing) {
        List<String> errors = new ArrayList<>();

        if (dto.getAccountType() != null) {
            validateAccountType(dto.getAccountType(), errors);
        }

        if (dto.getCreditLimit() != null && dto.getCashCreditLimit() != null) {
            if (dto.getCashCreditLimit().compareTo(dto.getCreditLimit()) > 0) {
                errors.add("Cash credit limit cannot exceed credit limit");
            }
        }

        if (dto.getExpiryDate() != null) {
            LocalDate checkOpen = existing.getOpenDate() != null
                    ? existing.getOpenDate()
                    : LocalDate.now();
            if (dto.getExpiryDate().isBefore(checkOpen)) {
                errors.add("Expiry date must be after open date");
            }
            if (dto.getExpiryDate().isBefore(LocalDate.now())) {
                errors.add("Expiry date cannot be in the past");
            }
        }

        if (dto.getReissueDate() != null && dto.getExpiryDate() != null) {
            if (dto.getReissueDate().isAfter(dto.getExpiryDate())) {
                errors.add("Reissue date must be before expiry date");
            }
        }

        throwIfErrors(errors);
    }

    // ─── Private validation helpers (COBOL sub-paragraphs) ──────────────────

    /**
     * COBOL EDIT-ACCTID:
     *   IF ACCT-ID IS NOT NUMERIC
     *     MOVE 'Y' TO WS-ACCTID-ERROR-FLG
     *   END-IF
     */
    private void validateAccountId(String accountId, List<String> errors) {
        if (accountId == null || accountId.isBlank()) {
            errors.add("Account ID is required");
            return;
        }
        if (!accountId.matches("^[0-9]{11}$")) {
            errors.add("Account ID must be 11 numeric digits");
        }
    }

    /**
     * COBOL EDIT-ACCT-TYPE:
     *   EVALUATE ACCT-TYPE-CD
     *     WHEN '1' CONTINUE
     *     WHEN '2' CONTINUE
     *     WHEN OTHER MOVE 'Y' TO WS-TYPE-ERROR-FLG
     */
    private void validateAccountType(String accountType, List<String> errors) {
        if (accountType != null && !accountType.matches("^[12]$")) {
            errors.add("Account type must be 1 (Credit) or 2 (Debit)");
        }
    }

    /**
     * COBOL EDIT-CREDIT-LIMIT:
     *   IF ACCT-CREDIT-LIMIT < ZERO
     *   IF ACCT-CASH-CREDIT-LIMIT > ACCT-CREDIT-LIMIT
     */
    private void validateCreditLimit(AccountRequestDto dto, List<String> errors) {
        if (dto.getCreditLimit() != null && dto.getCashCreditLimit() != null) {
            if (dto.getCashCreditLimit().compareTo(dto.getCreditLimit()) > 0) {
                errors.add("Cash credit limit cannot exceed total credit limit");
            }
        }
    }

    /**
     * COBOL EDIT-EXPIRY-DATE / EDIT-OPEN-DATE:
     *   IF ACCT-EXPIRY-DATE < ACCT-OPEN-DATE
     *     MOVE 'Y' TO WS-DATE-ERROR-FLG
     */
    private void validateDates(
            LocalDate openDate,
            LocalDate expiryDate,
            LocalDate reissueDate,
            List<String> errors) {

        LocalDate effectiveOpen = openDate != null ? openDate : LocalDate.now();

        if (expiryDate != null && expiryDate.isBefore(effectiveOpen)) {
            errors.add("Expiry date must be after open date");
        }

        if (reissueDate != null && expiryDate != null
                && reissueDate.isAfter(expiryDate)) {
            errors.add("Reissue date must be before expiry date");
        }

        if (openDate != null && openDate.isAfter(LocalDate.now())) {
            errors.add("Open date cannot be in the future");
        }
    }

    private void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(
                    AccountConstants.ERR_VALIDATION_FAILED,
                    "Validation failed: " + String.join("; ", errors),
                    errors
            );
        }
    }
}