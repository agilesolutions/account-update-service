// dto/AccountResponseDto.java
package com.agilesolutions.account.domain.dto;

import com.agilesolutions.account.domain.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO - maps COBOL output commarea fields
 * Mirrors COBOL ACCT-VIEW paragraph output structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account response payload")
public class AccountResponseDto {

    @Schema(description = "Internal surrogate key")
    private Long id;

    @Schema(description = "Account ID (11 chars)")
    private String accountId;

    @Schema(description = "Account name")
    private String accountName;

    @Schema(description = "Account type code")
    private String accountType;

    @Schema(description = "Account type description")
    private String accountTypeDescription;

    @Schema(description = "Account active status")
    private AccountStatus activeStatus;

    @Schema(description = "Current balance")
    private BigDecimal currBal;

    @Schema(description = "Credit limit")
    private BigDecimal creditLimit;

    @Schema(description = "Cash credit limit")
    private BigDecimal cashCreditLimit;

    @Schema(description = "Available credit (derived)")
    private BigDecimal availableCredit;

    @Schema(description = "Account open date")
    private LocalDate openDate;

    @Schema(description = "Account expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Account reissue date")
    private LocalDate reissueDate;

    @Schema(description = "Current cycle credit")
    private BigDecimal currCycleCredit;

    @Schema(description = "Current cycle debit")
    private BigDecimal currCycleDebit;

    @Schema(description = "ZIP code")
    private String addrZip;

    @Schema(description = "State")
    private String addrState;

    @Schema(description = "Country")
    private String addrCountry;

    @Schema(description = "Address line 1")
    private String addrLine1;

    @Schema(description = "Address line 2")
    private String addrLine2;

    @Schema(description = "Primary phone number")
    private String phoneNumber1;

    @Schema(description = "Secondary phone number")
    private String phoneNumber2;

    @Schema(description = "Group ID")
    private String groupId;

    @Schema(description = "Student indicator")
    private String studentInd;

    @Schema(description = "Over limit indicator")
    private String overLimitInd;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated by")
    private String updatedBy;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Optimistic lock version")
    private Long version;
}