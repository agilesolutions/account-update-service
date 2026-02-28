package com.carddemo.service;

import com.carddemo.exception.CardNotFoundException;
import com.carddemo.model.dto.CardUpdateRequest;
import com.carddemo.model.dto.CardUpdateResponse;
import com.carddemo.model.entity.CreditCard;
import com.carddemo.repository.CreditCardRepository;
import com.carddemo.service.impl.CardUpdateServiceImpl;
import com.carddemo.validation.CardUpdateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Update Service Tests - COBOL COCRDUPC Business Logic")
class CardUpdateServiceTest {

    @Mock
    private CreditCardRepository cardRepository;

    @Mock
    private CardUpdateValidator cardUpdateValidator;

    @InjectMocks
    private CardUpdateServiceImpl cardUpdateService;

    private static final String VALID_CARD_NUMBER = "4111111111111111";
    private static final String VALID_ACCOUNT_ID = "00000001001";
    private static final String TRANSACTION_ID = "CRDUPD-TEST001";

    private CreditCard testCard;
    private CardUpdateRequest validRequest;

    @BeforeEach
    void setUp() {
        testCard = CreditCard.builder()
                .id(1L)
                .cardNumber(VALID_CARD_NUMBER)
                .accountId(VALID_ACCOUNT_ID)
                .cvvCode("123")
                .embossedName("JOHN DOE")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .activeStatus("Y")
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1500.00"))
                .currentBalance(new BigDecimal("1250.75"))
                .currentCycleCredit(new BigDecimal("500.00"))
                .currentCycleDebit(new BigDecimal("250.25"))
                .groupId("PREMIUM")
                .version(0L)
                .build();

        validRequest = CardUpdateRequest.builder()
                .cardNumber(VALID_CARD_NUMBER)
                .accountId(VALID_ACCOUNT_ID)
                .cvvCode("456")
                .embossedName("JOHN DOE UPDATED")
                .expirationDate(LocalDate.of(2030, 12, 31))
                .activeStatus("Y")
                .creditLimit(new BigDecimal("7500.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .groupId("GOLD")
                .build();
    }

    // =========================================================
    // getCardDetails Tests - COBOL: 1000-GET-DETAILS-SCREEN
    // =========================================================

    @Test
    @DisplayName("getCardDetails - Success - Maps to COBOL READ CARD-FILE")
    void getCardDetails_Success() {
        // Given
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER))
                .thenReturn(Optional.of(testCard));

        // When
        CardUpdateResponse.CardData result = cardUpdateService.getCardDetails(VALID_CARD_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getActiveStatus()).isEqualTo("Y");
        assertThat(result.getCreditLimit()).isEqualByComparingTo("5000.00");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("getCardDetails - Not Found - Maps to COBOL: WHEN DFHRESP(NOTFND)")
    void getCardDetails_NotFound_ThrowsException() {
        // Given
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cardUpdateService.getCardDetails(VALID_CARD_NUMBER))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");

        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
    }

    // =========================================================
    // updateCard Tests - COBOL: 9000-UPDATE-CARD
    // =========================================================

    @Test
    @DisplayName("updateCard - Success - Maps to COBOL: EXEC CICS REWRITE FILE('CARDDAT')")
    void updateCard_Success() {
        // Given
        when(cardUpdateValidator.validate(any())).thenReturn(Collections.emptyList());

        // Mock existing card retrieval
        when(cardRepository.findByCardNumber(VALID_CARD_NUMBER))
                .thenReturn(Optional.of(testCard));
        // Mock card save
        when(cardRepository.save(any(CreditCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CardUpdateResponse result = cardUpdateService.updateCard(validRequest, TRANSACTION_ID);
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCardData()).isNotNull();
        assertThat(result.getCardData().getCardNumber()).isEqualTo(VALID_CARD_NUMBER);
        assertThat(result.getCardData().getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getCardData().getActiveStatus()).isEqualTo("Y");
        assertThat(result.getCardData().getCreditLimit()).isEqualByComparingTo("7500.00");
        verify(cardUpdateValidator, times(1)).validate(any());
        verify(cardRepository, times(1)).findByCardNumber(VALID_CARD_NUMBER);
        verify(cardRepository, times(1)).save(any(CreditCard.class));
    }
}

