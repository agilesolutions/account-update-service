package com.carddemo.exception;

import java.util.List;

/**
 * Card Update Exception
 * Maps to COBOL error conditions:
 * - RESP = DFHRESP(DUPREC) - Duplicate record
 * - RESP = DFHRESP(NOSPACE) - No space
 * - Business rule violations
 */
public class CardUpdateException extends RuntimeException {

    private final List<String> validationErrors;

    public CardUpdateException(String message) {
        super(message);
        this.validationErrors = null;
    }

    public CardUpdateException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}