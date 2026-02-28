// exception/OptimisticLockException.java
package com.agilesolutions.account.exception;

import lombok.Getter;

/**
 * Thrown on concurrent update conflict
 * Mirrors COBOL FILE STATUS '09' (concurrent record update detected)
 */
@Getter
public class OptimisticLockException extends RuntimeException {

    private final String errorCode;

    public OptimisticLockException(String message) {
        super(message);
        this.errorCode = "ERR_CONCURRENT_UPDATE";
    }
}