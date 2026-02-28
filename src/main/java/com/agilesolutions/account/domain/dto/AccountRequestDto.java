// dto/AccountRequestDto.java
package com.agilesolutions.account.domain.dto;

import com.agilesolutions.account.domain.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO mapping COBOL DFHCOMMAREA input fields
 * from COACTUPC paragraphs:
 *   - PROCESS-ENTER-KEY
 *   - EDIT-ACCOUNT-DATA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account create/update request payload")
public class AccountRequestDto {

    @Schema(description = "Account ID (11 chars)", example = "00001001001")
    @NotBlank(message = "Account ID is required")
    @Size(max = 11, message = "Account ID must not exceed 11 characters")
    @Pattern(regexp = "^[0-9]{11}$", message = "Account ID must be 11 numeric digits")
    private String accountId;

    @Schema(description = "Account holder name", example = "JOHN DOE")
    @Size(max = 25, message = "Account name must not exceed 25 characters")
    private String accountName;

    @Schema(description = "Account type: 1=Credit, 2=Debit", example = "1")
    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "^[12]$", message = "Account type must be 1 or 2")
    private String accountType;

    @Schema(description = "Account active status", example = "ACTIVE")
    @NotNull(message = "Active status is required")
    private AccountStatus activeStatus;

    @Schema(description = "Current balance", example = "1500.00")
    @DecimalMin(value = "0.00", message = "Current balance cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Balance format invalid")
    private BigDecimal currBal;

    @Schema(description = "Credit limit", example = "5000.00")
    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Credit limit format invalid")
    private BigDecimal creditLimit;

    @Schema(description = "Cash credit limit", example = "2000.00")
    @DecimalMin(value = "0.00", message = "Cash credit limit cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Cash credit limit format invalid")
    private BigDecimal cashCreditLimit;

    @Schema(description = "Account open date", example = "2024-01-15")
    private LocalDate openDate;

    @Schema(description = "Account expiry date", example = "2026-01-15")
    private LocalDate expiryDate;

    @Schema(description = "Account reissue date", example = "2025-01-15")
    private LocalDate reissueDate;

    @Schema(description = "Current cycle credit", example = "200.00")
    private BigDecimal currCycleCredit;

    @Schema(description = "Current cycle debit", example = "100.00")
    private BigDecimal currCycleDebit;

    @Schema(description = "ZIP code", example = "10001")
    @Size(max = 10, message = "ZIP code must not exceed 10 characters")
    private String addrZip;

    @Schema(description = "State", example = "NY")
    @Size(max = 20, message = "State must not exceed 20 characters")
    private String addrState;

    @Schema(description = "Country", example = "USA")
    @Size(max = 20, message = "Country must not exceed 20 characters")
    private String addrCountry;

    @Schema(description = "Address line 1", example = "123 Main St")
    @Size(max = 50, message = "Address line 1 must not exceed 50 characters")
    private String addrLine1;

    @Schema(description = "Address line 2", example = "Apt 4B")
    @Size(max = 50, message = "Address line 2 must not exceed 50 characters")
    private String addrLine2;

    @Schema(description = "Primary phone number", example = "+12125551234")
    @Pattern(regexp = "^[+0-9\\-() ]{0,15}$", message = "Invalid phone number format")
    private String phoneNumber1;

    @Schema(description = "Secondary phone number", example = "+12125555678")
    @Pattern(regexp = "^[+0-9\\-() ]{0,15}$", message = "Invalid phone number format")
    private String phoneNumber2;

    @Schema(description = "Group ID", example = "GRP001")
    @Size(max = 10, message = "Group ID must not exceed 10 characters")
    private String groupId;

    @Schema(description = "Student indicator: Y/N", example = "N")
    @Pattern(regexp = "^[YN]$", message = "Student indicator must be Y or N")
    private String studentInd;
}