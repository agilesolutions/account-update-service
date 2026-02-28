// dto/AccountUpdateDto.java
package com.agilesolutions.account.domain.dto;

import com.agilesolutions.account.domain.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update DTO - maps COBOL PROCESS-ENTER-KEY paragraph
 * where only modified fields are sent (COBOL screen field change detection)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account partial update payload")
public class AccountUpdateDto {

    @Schema(description = "Account name", example = "JANE DOE")
    @Size(max = 25, message = "Account name must not exceed 25 characters")
    private String accountName;

    @Schema(description = "Account type", example = "1")
    @Pattern(regexp = "^[12]$", message = "Account type must be 1 or 2")
    private String accountType;

    @Schema(description = "Active status")
    private AccountStatus activeStatus;

    @Schema(description = "Credit limit")
    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;

    @Schema(description = "Cash credit limit")
    @DecimalMin(value = "0.00", message = "Cash credit limit cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal cashCreditLimit;

    @Schema(description = "Expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Reissue date")
    private LocalDate reissueDate;

    @Schema(description = "ZIP code")
    @Size(max = 10)
    private String addrZip;

    @Schema(description = "State")
    @Size(max = 20)
    private String addrState;

    @Schema(description = "Country")
    @Size(max = 20)
    private String addrCountry;

    @Schema(description = "Address line 1")
    @Size(max = 50)
    private String addrLine1;

    @Schema(description = "Address line 2")
    @Size(max = 50)
    private String addrLine2;

    @Schema(description = "Primary phone number")
    @Pattern(regexp = "^[+0-9\\-() ]{0,15}$")
    private String phoneNumber1;

    @Schema(description = "Secondary phone number")
    @Pattern(regexp = "^[+0-9\\-() ]{0,15}$")
    private String phoneNumber2;

    @Schema(description = "Group ID")
    @Size(max = 10)
    private String groupId;

    @Schema(description = "Student indicator Y/N")
    @Pattern(regexp = "^[YN]$")
    private String studentInd;
}