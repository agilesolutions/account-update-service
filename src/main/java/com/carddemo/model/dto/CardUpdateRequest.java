package com.carddemo.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Card Update Request DTO
 * Maps to COBOL working storage fields in COCRDUPC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credit card update request payload")
public class CardUpdateRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    @Schema(description = "16-digit card number", example = "4111111111111111")
    private String cardNumber;

    @NotBlank(message = "Account ID is required")
    @Pattern(regexp = "\\d{1,11}", message = "Account ID must be numeric up to 11 digits")
    @Schema(description = "Account identifier", example = "00000001001")
    private String accountId;

    @NotBlank(message = "CVV code is required")
    @Pattern(regexp = "\\d{3}", message = "CVV must be 3 digits")
    @Schema(description = "Card CVV code", example = "123")
    private String cvvCode;

    @NotBlank(message = "Embossed name is required")
    @Size(max = 50, message = "Embossed name cannot exceed 50 characters")
    @Schema(description = "Name embossed on card", example = "JOHN DOE")
    private String embossedName;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Card expiration date", example = "2028-12-31")
    private LocalDate expirationDate;

    @NotBlank(message = "Active status is required")
    @Pattern(regexp = "[YN]", message = "Active status must be Y or N")
    @Schema(description = "Card active status (Y/N)", example = "Y")
    private String activeStatus;

    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    @DecimalMax(value = "9999999999.99", message = "Credit limit exceeds maximum allowed")
    @Schema(description = "Credit limit", example = "5000.00")
    private BigDecimal creditLimit;

    @DecimalMin(value = "0.00", message = "Cash credit limit cannot be negative")
    @DecimalMax(value = "9999999999.99", message = "Cash credit limit exceeds maximum allowed")
    @Schema(description = "Cash credit limit", example = "1500.00")
    private BigDecimal cashCreditLimit;

    @Size(max = 10, message = "Group ID cannot exceed 10 characters")
    @Schema(description = "Card group identifier", example = "PREMIUM")
    private String groupId;
}