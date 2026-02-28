package com.carddemo.validation;

import com.carddemo.model.dto.CardUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Card Update Validator
 *
 * Migrated from COBOL COCRDUPC business validation rules:
 * ========================================================
 * 1400-SEND-EDIT-ERRMSG - Main validation paragraph
 *
 * COBOL Validation Logic mapped to Java:
 * - IF CCARD-CVV-CODE NOT NUMERIC → validateCvvCode()
 * - IF CCARD-EXPIRY-DATE = SPACES → validateExpirationDate()
 * - IF CCARD-ACTIVE-STATUS NOT = 'Y' OR 'N' → validateActiveStatus()
 * - IF CCARD-CREDIT-LIMIT NOT NUMERIC → validateCreditLimit()
 * - IF CCARD-NAME = SPACES → validateEmbossedName()
 */
@Component
@Slf4j
public class CardUpdateValidator {

    private static final String ACTIVE_STATUS_YES = "Y";
    private static final String ACTIVE_STATUS_NO = "N";
    private static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("9999999999.99");
    private static final int CARD_NUMBER_LENGTH = 16;
    private static final int CVV_LENGTH = 3;

    /**
     * Main validation method
     * Maps to COBOL: PERFORM 1400-SEND-EDIT-ERRMSG
     */
    public List<String> validate(CardUpdateRequest request) {
        List<String> errors = new ArrayList<>();

        validateCardNumber(request.getCardNumber(), errors);
        validateAccountId(request.getAccountId(), errors);
        validateCvvCode(request.getCvvCode(), errors);
        validateEmbossedName(request.getEmbossedName(), errors);
        validateExpirationDate(request.getExpirationDate(), errors);
        validateActiveStatus(request.getActiveStatus(), errors);
        validateCreditLimit(request.getCreditLimit(), errors);
        validateCashCreditLimit(request.getCashCreditLimit(), errors);
        validateCreditLimitRelationship(request.getCreditLimit(),
                request.getCashCreditLimit(), errors);

        return errors;
    }

    /**
     * Validate card number
     * COBOL: IF CCARD-NUM NOT NUMERIC OR LENGTH NOT = 16
     */
    private void validateCardNumber(String cardNumber, List<String> errors) {
        if (cardNumber == null || cardNumber.isBlank()) {
            errors.add("Card number is required");
            return;
        }
        if (!cardNumber.matches("\\d+")) {
            errors.add("Card number must be numeric");
            return;
        }
        if (cardNumber.length() != CARD_NUMBER_LENGTH) {
            errors.add("Card number must be exactly 16 digits");
        }
        // Luhn algorithm validation - PCI DSS compliance
        if (!isValidLuhn(cardNumber)) {
            errors.add("Card number failed checksum validation (Luhn)");
        }
    }

    /**
     * Validate account ID
     * COBOL: IF CDEMO-ACCT-ID NOT NUMERIC
     */
    private void validateAccountId(String accountId, List<String> errors) {
        if (accountId == null || accountId.isBlank()) {
            errors.add("Account ID is required");
            return;
        }
        if (!accountId.matches("\\d+")) {
            errors.add("Account ID must be numeric");
        }
        if (accountId.length() > 11) {
            errors.add("Account ID cannot exceed 11 digits");
        }
    }

    /**
     * Validate CVV code
     * COBOL: IF CCARD-CVV-CODE NOT NUMERIC
     *        MOVE 'CVV Code must be numeric' TO WS-ERR-MSG
     */
    private void validateCvvCode(String cvvCode, List<String> errors) {
        if (cvvCode == null || cvvCode.isBlank()) {
            errors.add("CVV code is required");
            return;
        }
        if (!cvvCode.matches("\\d+")) {
            errors.add("CVV code must be numeric");
            return;
        }
        if (cvvCode.length() != CVV_LENGTH) {
            errors.add("CVV code must be exactly 3 digits");
        }
    }

    /**
     * Validate embossed name
     * COBOL: IF CCARD-EMBOSSED-NAME = SPACES
     *        MOVE 'Embossed name cannot be blank' TO WS-ERR-MSG
     */
    private void validateEmbossedName(String embossedName, List<String> errors) {
        if (embossedName == null || embossedName.isBlank()) {
            errors.add("Embossed name cannot be blank");
            return;
        }
        if (embossedName.length() > 50) {
            errors.add("Embossed name cannot exceed 50 characters");
        }
        if (!embossedName.matches("[A-Za-z\\s\\-\\.]+")) {
            errors.add("Embossed name contains invalid characters");
        }
    }

    /**
     * Validate expiration date
     * COBOL: IF CCARD-EXPIRY-DATE = SPACES OR CCARD-EXPIRY-DATE NOT VALID
     *        MOVE 'Invalid expiry date' TO WS-ERR-MSG
     */
    private void validateExpirationDate(LocalDate expirationDate, List<String> errors) {
        if (expirationDate == null) {
            errors.add("Expiration date is required");
            return;
        }
        if (!expirationDate.isAfter(LocalDate.now())) {
            errors.add("Expiration date must be in the future");
        }
        // Max 10 years in future (reasonable card limit)
        if (expirationDate.isAfter(LocalDate.now().plusYears(10))) {
            errors.add("Expiration date cannot exceed 10 years from today");
        }
    }

    /**
     * Validate active status
     * COBOL: IF CCARD-ACTIVE-STATUS NOT = 'Y' AND
     *           CCARD-ACTIVE-STATUS NOT = 'N'
     *        MOVE 'Active status must be Y or N' TO WS-ERR-MSG
     */
    private void validateActiveStatus(String activeStatus, List<String> errors) {
        if (activeStatus == null || activeStatus.isBlank()) {
            errors.add("Active status is required");
            return;
        }
        if (!ACTIVE_STATUS_YES.equals(activeStatus) && !ACTIVE_STATUS_NO.equals(activeStatus)) {
            errors.add("Active status must be Y or N");
        }
    }

    /**
     * Validate credit limit
     * COBOL: IF CCARD-CREDIT-LIMIT NOT NUMERIC OR < 0
     *        MOVE 'Credit limit must be positive numeric' TO WS-ERR-MSG
     */
    private void validateCreditLimit(BigDecimal creditLimit, List<String> errors) {
        if (creditLimit == null) {
            errors.add("Credit limit is required");
            return;
        }
        if (creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Credit limit cannot be negative");
        }
        if (creditLimit.compareTo(MAX_CREDIT_LIMIT) > 0) {
            errors.add("Credit limit exceeds maximum allowed value");
        }
    }

    /**
     * Validate cash credit limit
     * COBOL: IF CCARD-CASH-CREDIT-LIMIT NOT NUMERIC OR < 0
     */
    private void validateCashCreditLimit(BigDecimal cashCreditLimit, List<String> errors) {
        if (cashCreditLimit == null) return; // Optional field

        if (cashCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Cash credit limit cannot be negative");
        }
        if (cashCreditLimit.compareTo(MAX_CREDIT_LIMIT) > 0) {
            errors.add("Cash credit limit exceeds maximum allowed value");
        }
    }

    /**
     * Validate cash credit limit <= total credit limit
     * COBOL: IF CCARD-CASH-CREDIT-LIMIT > CCARD-CREDIT-LIMIT
     *        MOVE 'Cash limit cannot exceed credit limit' TO WS-ERR-MSG
     */
    private void validateCreditLimitRelationship(BigDecimal creditLimit,
                                                 BigDecimal cashCreditLimit, List<String> errors) {
        if (creditLimit == null || cashCreditLimit == null) return;

        if (cashCreditLimit.compareTo(creditLimit) > 0) {
            errors.add("Cash credit limit cannot exceed total credit limit");
        }
    }

    /**
     * Luhn Algorithm for card number validation
     * PCI DSS compliance requirement
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}