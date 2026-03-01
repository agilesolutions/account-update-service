// enums/AccountStatus.java
package com.agilesolutions.account.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Maps COBOL ACCT-ACTIVE-STATUS PIC X(1)
 * Values: 'Y' = ACTIVE, 'N' = INACTIVE
 */
@Getter
@RequiredArgsConstructor
public enum AccountStatus {Y("Y"), N("N");

    private final String code;

    @JsonCreator
    public static AccountStatus fromCode(String code) {
        for (AccountStatus status : AccountStatus.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown AccountStatus code: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}