package com.carddemo.controller;

import com.carddemo.exception.CardNotFoundException;
import com.carddemo.exception.GlobalExceptionHandler;
import com.carddemo.model.dto.CardUpdateRequest;
import com.carddemo.model.dto.CardUpdateResponse;
import com.carddemo.service.CardUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Update Controller Tests - Migrated from COBOL COCRDUPC")
class CardUpdateControllerTest {

    @Mock
    private CardUpdateService cardUpdateService;

    @InjectMocks
    private CardUpdateController cardUpdateController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String VALID_CARD_NUMBER = "4111111111111111";
    private static final String VALID_ACCOUNT_ID = "00000001001";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(cardUpdateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /cards/{cardNumber} - Success - Maps to COBOL: 1000-GET-DETAILS-SCREEN")
    void getCardDetails_Success() throws Exception {
        // Given
        CardUpdateResponse.CardData mockCardData = buildMockCardData();
        when(cardUpdateService.getCardDetails(VALID_CARD_NUMBER)).thenReturn(mockCardData);

        // When & Then
        mockMvc.perform(get("/api/v1/cards/{cardNumber}", VALID_CARD_NUMBER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardNumber").value(VALID_CARD_NUMBER))
                .andExpect(jsonPath("$.data.accountId").value(VALID_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.activeStatus").value("Y"));

        verify(cardUpdateService, times(1)).getCardDetails(VALID_CARD_NUMBER);
    }

    @Test
    @DisplayName("GET /cards/{cardNumber} - Not Found - Maps to COBOL: RESP=DFHRESP(NOTFND)")
    void getCardDetails_NotFound() throws Exception {
        // Given
        when(cardUpdateService.getCardDetails(VALID_CARD_NUMBER))
                .thenThrow(new CardNotFoundException("Card not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/cards/{cardNumber}", VALID_CARD_NUMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /cards/{cardNumber} - Success - Maps to COBOL: 9000-UPDATE-CARD")
    void updateCard_Success() throws Exception {
        // Given
        CardUpdateRequest request = buildValidUpdateRequest();
        CardUpdateResponse mockResponse = CardUpdateResponse.builder()
                .statusCode("0000")
                .message("Card updated successfully")
                .cardData(buildMockCardData())
                .build();

        when(cardUpdateService.updateCard(any(CardUpdateRequest.class), anyString()))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/cards/{cardNumber}", VALID_CARD_NUMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("0000"));

        verify(cardUpdateService, times(1)).updateCard(any(), anyString());
    }

    @Test
    @DisplayName("PUT /cards/{cardNumber} - Invalid Request - Maps to COBOL: 1400-SEND-EDIT-ERRMSG")
    void updateCard_ValidationFailure() throws Exception {
        // Given - invalid request with missing required fields
        CardUpdateRequest invalidRequest = CardUpdateRequest.builder()
                .cardNumber(VALID_CARD_NUMBER)
                .accountId(VALID_ACCOUNT_ID)
                // Missing required fields: cvvCode, embossedName, etc.
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/cards/{cardNumber}", VALID_CARD_NUMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /cards/{cardNumber} - Card number path/body mismatch")
    void updateCard_CardNumberMismatch() throws Exception {
        // Given
        CardUpdateRequest request = buildValidUpdateRequest();

        // When & Then - path has different card number
        mockMvc.perform(put("/api/v1/cards/{cardNumber}", "9999999999999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Helper methods
    private CardUpdateResponse.CardData buildMockCardData() {
        return CardUpdateResponse.CardData.builder()
                .cardNumber(VALID_CARD_NUMBER)
                .accountId(VALID_ACCOUNT_ID)
                .cvvCode("123")
                .embossedName("JOHN DOE")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .activeStatus("Y")
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1500.00"))
                .currentBalance(new BigDecimal("1250.75"))
                .groupId("PREMIUM")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CardUpdateRequest buildValidUpdateRequest() {
        return CardUpdateRequest.builder()
                .cardNumber(VALID_CARD_NUMBER)
                .accountId(VALID_ACCOUNT_ID)
                .cvvCode("123")
                .embossedName("JOHN DOE")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .activeStatus("Y")
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1500.00"))
                .groupId("PREMIUM")
                .build();
    }
}