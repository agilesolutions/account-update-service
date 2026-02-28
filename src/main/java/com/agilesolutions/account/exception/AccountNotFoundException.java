// exception/AccountNotFoundException.java
package com.agilesolutions.account.exception;

import lombok.Getter;

/**
 * Thrown when COBOL READ ACCTDAT returns FILE STATUS '23' (NOT FOUND)
 */
@Getter
public class AccountNotFoundException extends RuntimeException {

    private final String errorCode;

    public AccountNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}