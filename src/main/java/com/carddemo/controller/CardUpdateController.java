package com.carddemo.controller;

import com.carddemo.model.dto.ApiResponse;
import com.carddemo.model.dto.CardSearchRequest;
import com.carddemo.model.dto.CardUpdateRequest;
import com.carddemo.model.dto.CardUpdateResponse;
import com.carddemo.service.CardUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Card Update REST Controller
 *
 * Replaces COBOL CICS screen interactions:
 * - BMS Map COCRDUPM → REST API endpoints
 * - EXEC CICS SEND MAP → HTTP Response
 * - EXEC CICS RECEIVE MAP → HTTP Request body
 * - EXEC CICS RETURN → HTTP Response
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Card Update", description = "Credit Card Update Operations - Migrated from COBOL COCRDUPC")
public class CardUpdateController {

    private final CardUpdateService cardUpdateService;

    /**
     * GET card details for update
     * Maps to COBOL: 1000-GET-DETAILS-SCREEN
     * Replaces: EXEC CICS SEND MAP('COCRDUPD') MAPSET('COCRDUPM')
     */
    @GetMapping("/{cardNumber}")
    @Operation(
            summary = "Get card details",
            description = "Retrieves credit card details for update. Migrated from COBOL 1000-GET-DETAILS-SCREEN"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Card found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found")
    public ResponseEntity<ApiResponse<CardUpdateResponse.CardData>> getCardDetails(
            @PathVariable
            @Parameter(description = "16-digit card number", example = "4111111111111111")
            String cardNumber) {

        String transactionId = generateTransactionId();
        log.info("GET card details request - transactionId: {}", transactionId);

        CardUpdateResponse.CardData cardData = cardUpdateService.getCardDetails(cardNumber);

        return ResponseEntity.ok(
                ApiResponse.success(cardData, "Card details retrieved successfully", transactionId)
        );
    }

    /**
     * PUT update credit card
     * Maps to COBOL: 9000-UPDATE-CARD via PROCESS-ENTER-KEY
     * Replaces: EXEC CICS REWRITE FILE('CARDDAT')
     */
    @PutMapping("/{cardNumber}")
    @Operation(
            summary = "Update credit card",
            description = "Updates credit card information. Migrated from COBOL 9000-UPDATE-CARD paragraph"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Card updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent update conflict")
    public ResponseEntity<ApiResponse<CardUpdateResponse>> updateCard(
            @PathVariable
            @Parameter(description = "16-digit card number")
            String cardNumber,
            @Valid @RequestBody CardUpdateRequest request) {

        // Ensure path variable matches request body
        if (!cardNumber.equals(request.getCardNumber())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Card number in path must match request body", null)
            );
        }

        String transactionId = generateTransactionId();
        log.info("PUT update card request - transactionId: {}", transactionId);

        CardUpdateResponse response = cardUpdateService.updateCard(request, transactionId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Card updated successfully", transactionId)
        );
    }

    /**
     * POST search card
     * Maps to COBOL: 1200-SEARCH-CARD
     * Replaces: EXEC CICS READ FILE('CARDDAT') with search criteria
     */
    @PostMapping("/search")
    @Operation(
            summary = "Search card",
            description = "Search card by number and optional account ID. Migrated from COBOL 1200-SEARCH-CARD"
    )
    public ResponseEntity<ApiResponse<CardUpdateResponse.CardData>> searchCard(
            @Valid @RequestBody CardSearchRequest request) {

        String transactionId = generateTransactionId();
        log.info("POST search card request - transactionId: {}", transactionId);

        CardUpdateResponse.CardData cardData = cardUpdateService.searchCard(request);

        return ResponseEntity.ok(
                ApiResponse.success(cardData, "Card found successfully", transactionId)
        );
    }

    /**
     * POST validate card data
     * Maps to COBOL: 1400-SEND-EDIT-ERRMSG
     * Used for pre-validation before update
     */
    @PostMapping("/validate")
    @Operation(
            summary = "Validate card data",
            description = "Validates card update fields without saving. Migrated from COBOL 1400-SEND-EDIT-ERRMSG"
    )
    public ResponseEntity<ApiResponse<Boolean>> validateCardData(
            @Valid @RequestBody CardUpdateRequest request) {

        String transactionId = generateTransactionId();
        log.info("POST validate card data - transactionId: {}", transactionId);

        boolean isValid = cardUpdateService.validateCardData(request);

        return ResponseEntity.ok(
                ApiResponse.success(isValid,
                        isValid ? "Card data is valid" : "Card data has validation errors",
                        transactionId)
        );
    }

    /**
     * Generate transaction ID
     * Maps to COBOL: EXEC CICS ASSIGN EIBTRNID - CICS transaction ID
     */
    private String generateTransactionId() {
        return "CRDUPD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}