// test/controller/AccountControllerTest.java
package com.agilesolutions.account.controller;

import com.agilesolutions.account.domain.dto.*;
import com.agilesolutions.account.domain.enums.AccountStatus;
import com.agilesolutions.account.exception.AccountNotFoundException;
import com.agilesolutions.account.exception.GlobalExceptionHandler;
import com.agilesolutions.account.service.AccountService;
import com.agilesolutions.account.util.AccountConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AccountController - REST API endpoint tests")
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean
    private AccountService accountService;

    private ObjectMapper objectMapper;
    private AccountResponseDto sampleResponse;
    private AccountRequestDto  sampleRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = AccountResponseDto.builder()
                .id(1L)
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .accountTypeDescription("Credit")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .availableCredit(new BigDecimal("3500.00"))
                .openDate(LocalDate.now().minusDays(30))
                .version(0L)
                .build();

        sampleRequest = AccountRequestDto.builder()
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .addrLine1("123 Main St")
                .addrState("NY")
                .addrCountry("USA")
                .addrZip("10001")
                .phoneNumber1("+12125551234")
                .studentInd("N")
                .build();
    }

    // ─── POST /accounts ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /accounts: admin creates account - 201 Created")
    void testCreateAccount_asAdmin_returns201() throws Exception {
        when(accountService.createAccount(any(AccountRequestDto.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("00001001001"))
                .andExpect(jsonPath("$.data.accountType").value("1"))
                .andExpect(jsonPath("$.data.availableCredit").value(3500.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /accounts: user role forbidden - 403")
    void testCreateAccount_asUser_returns403() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(accountService, never()).createAccount(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /accounts: invalid accountId - 400 Bad Request")
    void testCreateAccount_invalidAccountId_returns400() throws Exception {
        sampleRequest.setAccountId("INVALID"); // not 11 digits

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ─── GET /accounts/{accountId} ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /accounts/{id}: account found - 200 OK")
    void testGetAccount_found_returns200() throws Exception {
        when(accountService.getAccountById("00001001001")).thenReturn(sampleResponse);

        mockMvc.perform(get("/accounts/00001001001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("00001001001"))
                .andExpect(jsonPath("$.data.activeStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /accounts/{id}: account not found - 404")
    void testGetAccount_notFound_returns404() throws Exception {
        when(accountService.getAccountById("99999999999"))
                .thenThrow(new AccountNotFoundException(
                        "Account not found: 99999999999",
                        AccountConstants.ERR_ACCOUNT_NOT_FOUND));

        mockMvc.perform(get("/accounts/99999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0001"));
    }

    @Test
    @DisplayName("GET /accounts/{id}: unauthenticated - 401")
    void testGetAccount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/accounts/00001001001"))
                .andExpect(status().isUnauthorized());
    }

    // ─── PUT /accounts/{accountId} ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /accounts/{id}: successful update - 200 OK")
    void testUpdateAccount_success_returns200() throws Exception {
        AccountUpdateDto updateDto = AccountUpdateDto.builder()
                .accountName("UPDATED NAME")
                .creditLimit(new BigDecimal("7000.00"))
                .build();

        when(accountService.updateAccount(eq("00001001001"), any(AccountUpdateDto.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/accounts/00001001001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── DELETE /accounts/{accountId} ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /accounts/{id}: logical deactivation - 204 No Content")
    void testDeactivateAccount_success_returns204() throws Exception {
        doNothing().when(accountService).deleteAccount("00001001001");

        mockMvc.perform(delete("/accounts/00001001001").with(csrf()))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount("00001001001");
    }

    // ─── GET /accounts ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /accounts: paginated list - 200 OK")
    void testGetAllAccounts_returns200() throws Exception {
        PagedResponseDto<AccountResponseDto> pagedResponse =
                PagedResponseDto.<AccountResponseDto>builder()
                        .content(java.util.List.of(sampleResponse))
                        .page(0).size(20)
                        .totalElements(1).totalPages(1)
                        .last(true)
                        .build();

        when(accountService.getAllAccounts(any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/accounts")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].accountId").value("00001001001"));
    }
}