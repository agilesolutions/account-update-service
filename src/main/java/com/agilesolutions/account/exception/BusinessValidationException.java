// exception/BusinessValidationException.java
package com.agilesolutions.account.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when COBOL EDIT-ACCOUNT-DATA sets WS-ERROR-FLAGS = 'Y'
 * and triggers SEND-ERRMSG paragraph
 */
@Getter
public class BusinessValidationException extends RuntimeException {

    private final String errorCode;
    private final List<String> validationErrors;

    public BusinessValidationException(String errorCode, String message) {
        super(message);
        this.errorCode  = errorCode;
        this.validationErrors = Collections.emptyList();
    }

    public BusinessValidationException(
            String errorCode, String message, List<String> validationErrors) {
        super(message);
        this.errorCode        = errorCode;
        this.validationErrors = validationErrors != null
                ? Collections.unmodifiableList(validationErrors)
                : Collections.emptyList();
    }
}