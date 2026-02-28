package com.carddemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Card Search Request DTO
 * Corresponds to COBOL: CDEMO-CARD-NUM search in COCRDUPC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Card search request")
public class CardSearchRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    @Schema(description = "16-digit card number to search", example = "4111111111111111")
    private String cardNumber;

    @Pattern(regexp = "\\d{1,11}", message = "Account ID must be numeric")
    @Schema(description = "Account ID to cross-validate", example = "00000001001")
    private String accountId;
}