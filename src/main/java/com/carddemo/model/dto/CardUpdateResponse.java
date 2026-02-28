package com.carddemo.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Card Update Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credit card update response")
public class CardUpdateResponse {

    @Schema(description = "Response status code")
    private String statusCode;

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "Updated card details")
    private CardData cardData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardData {
        private String cardNumber;
        private String accountId;
        private String cvvCode;
        private String embossedName;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expirationDate;

        private String activeStatus;
        private BigDecimal creditLimit;
        private BigDecimal cashCreditLimit;
        private BigDecimal currentBalance;
        private BigDecimal currentCycleCredit;
        private BigDecimal currentCycleDebit;
        private String groupId;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
    }
}