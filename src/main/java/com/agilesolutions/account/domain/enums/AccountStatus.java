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
public enum AccountStatus {
    ACTIVE("Y", "Active"),
    INACTIVE("N", "Inactive");

    private final String code;
    private final String description;

    @JsonCreator
    public static AccountStatus fromCode(String code) {
        for (AccountStatus status : values()) {
            if (status.code.equalsIgnoreCase(code) ||
                    status.name().equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown account status: " + code);
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}