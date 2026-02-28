package com.carddemo.service.impl;

import com.carddemo.exception.CardNotFoundException;
import com.carddemo.exception.CardUpdateException;
import com.carddemo.model.dto.CardSearchRequest;
import com.carddemo.model.dto.CardUpdateRequest;
import com.carddemo.model.dto.CardUpdateResponse;
import com.carddemo.model.entity.CreditCard;
import com.carddemo.repository.CreditCardRepository;
import com.carddemo.service.CardUpdateService;
import com.carddemo.validation.CardUpdateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Card Update Service Implementation
 *
 * COBOL Paragraph Mapping:
 * ========================
 * PROCESS-ENTER-KEY       → updateCard()
 * 1000-GET-DETAILS-SCREEN → getCardDetails()
 * 1200-SEARCH-CARD        → searchCard()
 * 1400-SEND-EDIT-ERRMSG   → validateCardData() via CardUpdateValidator
 * 9000-UPDATE-CARD        → performUpdate()
 * 9100-GET-CARD-DATA      → fetchCardFromRepository()
 * 9200-UPDATE-CARD-DATA   → applyUpdatesAndSave()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardUpdateServiceImpl implements CardUpdateService {

    private final CreditCardRepository cardRepository;
    private final CardUpdateValidator cardUpdateValidator;

    /**
     * Retrieve card details for display/update
     * Maps to COBOL: 1000-GET-DETAILS-SCREEN
     * EXEC CICS READ FILE('CARDDAT') RIDFLD(WS-CARD-RID-CARDNUM)
     */
    @Override
    @Transactional(readOnly = true)
    public CardUpdateResponse.CardData getCardDetails(String cardNumber) {
        log.info("Fetching card details for card number: {}",
                maskCardNumber(cardNumber));

        // COBOL: READ CARD-FILE INTO WS-CARD-RECORD
        CreditCard card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> {
                    log.error("Card not found: {}", maskCardNumber(cardNumber));
                    // Maps to COBOL: MOVE 'Card not found.' TO WS-ERR-MSG
                    return new CardNotFoundException(
                            "Card not found for number: " + maskCardNumber(cardNumber));
                });

        return mapToCardData(card);
    }

    /**
     * Update credit card information
     * Maps to COBOL: 9000-UPDATE-CARD paragraph
     */
    @Override
    @Transactional
    public CardUpdateResponse updateCard(CardUpdateRequest request, String transactionId) {
        log.info("Starting card update for transactionId: {}, cardNumber: {}",
                transactionId, maskCardNumber(request.getCardNumber()));

        // COBOL: PERFORM 1400-SEND-EDIT-ERRMSG - Validate all fields
        List<String> validationErrors = cardUpdateValidator.validate(request);
        if (!validationErrors.isEmpty()) {
            log.warn("Validation failed for card update: {}", validationErrors);
            throw new CardUpdateException("Validation failed: " + String.join(", ", validationErrors));
        }

        // COBOL: 9100-GET-CARD-DATA - Read existing card record
        CreditCard existingCard = cardRepository.findByCardNumber(request.getCardNumber())
                .orElseThrow(() -> new CardNotFoundException(
                        "Card not found: " + maskCardNumber(request.getCardNumber())));

        // COBOL: Verify account cross-reference
        // IF CDEMO-CARD-ACCT-ID NOT = WS-ACCT-ID
        if (request.getAccountId() != null &&
                !existingCard.getAccountId().equals(request.getAccountId())) {
            log.error("Account ID mismatch for card: {}", maskCardNumber(request.getCardNumber()));
            throw new CardUpdateException(
                    "Account ID does not match for card number provided");
        }

        // COBOL: 9200-UPDATE-CARD-DATA - Apply updates and save
        CreditCard updatedCard = applyUpdates(existingCard, request);
        CreditCard savedCard = cardRepository.save(updatedCard);

        log.info("Card updated successfully for transactionId: {}", transactionId);

        return CardUpdateResponse.builder()
                .statusCode("0000")
                .message("Card updated successfully")
                .cardData(mapToCardData(savedCard))
                .build();
    }

    /**
     * Search card by card number and optional account ID
     * Maps to COBOL: 1200-SEARCH-CARD
     */
    @Override
    @Transactional(readOnly = true)
    public CardUpdateResponse.CardData searchCard(CardSearchRequest request) {
        log.info("Searching card: {}", maskCardNumber(request.getCardNumber()));

        CreditCard card;

        if (request.getAccountId() != null && !request.getAccountId().isBlank()) {
            // COBOL: Cross-validate card number with account ID
            card = cardRepository
                    .findByCardNumberAndAccountId(request.getCardNumber(), request.getAccountId())
                    .orElseThrow(() -> new CardNotFoundException(
                            "Card not found for given card number and account ID"));
        } else {
            card = cardRepository.findByCardNumber(request.getCardNumber())
                    .orElseThrow(() -> new CardNotFoundException(
                            "Card not found: " + maskCardNumber(request.getCardNumber())));
        }

        return mapToCardData(card);
    }

    /**
     * Validate card update data
     * Maps to COBOL: 1400-SEND-EDIT-ERRMSG
     */
    @Override
    public boolean validateCardData(CardUpdateRequest request) {
        List<String> errors = cardUpdateValidator.validate(request);
        if (!errors.isEmpty()) {
            log.debug("Card validation errors: {}", errors);
            return false;
        }
        return true;
    }

    /**
     * Apply updates to existing card entity
     * Maps to COBOL: 9200-UPDATE-CARD-DATA - REWRITE CARD-FILE
     */
    private CreditCard applyUpdates(CreditCard existingCard, CardUpdateRequest request) {
        // COBOL: MOVE CORRESPONDING WS-CARD-DATA TO CARD-RECORD
        existingCard.setCvvCode(request.getCvvCode());
        existingCard.setEmbossedName(request.getEmbossedName().toUpperCase().trim());
        existingCard.setExpirationDate(request.getExpirationDate());
        existingCard.setActiveStatus(request.getActiveStatus());

        if (request.getCreditLimit() != null) {
            existingCard.setCreditLimit(request.getCreditLimit());
        }
        if (request.getCashCreditLimit() != null) {
            existingCard.setCashCreditLimit(request.getCashCreditLimit());
        }
        if (request.getGroupId() != null) {
            existingCard.setGroupId(request.getGroupId().trim());
        }

        return existingCard;
    }

    /**
     * Map entity to response DTO
     */
    private CardUpdateResponse.CardData mapToCardData(CreditCard card) {
        return CardUpdateResponse.CardData.builder()
                .cardNumber(card.getCardNumber())
                .accountId(card.getAccountId())
                .cvvCode(card.getCvvCode())
                .embossedName(card.getEmbossedName())
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .creditLimit(card.getCreditLimit())
                .cashCreditLimit(card.getCashCreditLimit())
                .currentBalance(card.getCurrentBalance())
                .currentCycleCredit(card.getCurrentCycleCredit())
                .currentCycleDebit(card.getCurrentCycleDebit())
                .groupId(card.getGroupId())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    /**
     * Mask card number for logging - PCI DSS compliance
     * COBOL equivalent: Security requirement for card data handling
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "************" + cardNumber.substring(cardNumber.length() - 4);
    }
}