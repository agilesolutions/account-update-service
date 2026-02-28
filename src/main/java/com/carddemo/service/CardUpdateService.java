package com.carddemo.service;

import com.carddemo.model.dto.CardSearchRequest;
import com.carddemo.model.dto.CardUpdateRequest;
import com.carddemo.model.dto.CardUpdateResponse;

/**
 * Card Update Service Interface
 * Migrated from COBOL COCRDUPC paragraph logic
 */
public interface CardUpdateService {

    /**
     * Retrieve card for update - maps to COBOL: 1000-GET-DETAILS-SCREEN
     */
    CardUpdateResponse.CardData getCardDetails(String cardNumber);

    /**
     * Update credit card - maps to COBOL: 9000-UPDATE-CARD
     */
    CardUpdateResponse updateCard(CardUpdateRequest request, String transactionId);

    /**
     * Search and validate card - maps to COBOL: 1200-SEARCH-CARD
     */
    CardUpdateResponse.CardData searchCard(CardSearchRequest request);

    /**
     * Validate card data - maps to COBOL: 1400-SEND-EDIT-ERRMSG
     */
    boolean validateCardData(CardUpdateRequest request);
}