// test/exception/GlobalExceptionHandlerTest.java
package com.agilesolutions.account.exception;

import com.agilesolutions.account.controller.AccountController;
import com.agilesolutions.account.service.AccountService;
import com.agilesolutions.account.util.AccountConstants;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests COBOL error paragraph equivalents routed through GlobalExceptionHandler
 *
 *   COBOL SEND-ERRMSG          -> BusinessValidationException -> 400
 *   COBOL FILE STATUS '23'     -> AccountNotFoundException    -> 404
 *   COBOL FILE STATUS '22'     -> DataIntegrityViolation      -> 409
 *   COBOL FILE STATUS '09'     -> OptimisticLockException     -> 409
 *   COBOL EXEC CICS NOTAUTH    -> AccessDeniedException       -> 403
 */
@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler - COBOL error paragraph tests")
class GlobalExceptionHandlerTest {

    @Autowired  private MockMvc      mockMvc;
    @MockBean   private AccountService accountService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("AccountNotFoundException -> 404 with ACCT-0001 error code")
    void testAccountNotFound_returns404WithErrorCode() throws Exception {
        when(accountService.getAccountById("99999999999"))
                .thenThrow(new AccountNotFoundException(
                        "Account not found: 99999999999",
                        AccountConstants.ERR_ACCOUNT_NOT_FOUND));

        mockMvc.perform(get("/accounts/99999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0001"))
                .andExpect(jsonPath("$.message").value("Account not found: 99999999999"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("BusinessValidationException -> 400 with validation errors list")
    void testBusinessValidation_returns400WithErrors() throws Exception {
        when(accountService.getAccountById("00001001001"))
                .thenThrow(new BusinessValidationException(
                        AccountConstants.ERR_VALIDATION_FAILED,
                        "Validation failed",
                        List.of("Expiry date must be after open date",
                                "Cash credit limit cannot exceed credit limit")));

        mockMvc.perform(get("/accounts/00001001001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0004"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0]").value("Expiry date must be after open date"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("OptimisticLockException -> 409 Conflict")
    void testOptimisticLock_returns409() throws Exception {
        when(accountService.getAccountById("00001001001"))
                .thenThrow(new OptimisticLockException("Concurrent update detected"));

        mockMvc.perform(get("/accounts/00001001001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_CONCURRENT_UPDATE"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("modified by another user")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Unexpected exception -> 500 Internal Server Error")
    void testUnexpectedException_returns500() throws Exception {
        when(accountService.getAccountById("00001001001"))
                .thenThrow(new RuntimeException("Unexpected DB failure"));

        mockMvc.perform(get("/accounts/00001001001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_INTERNAL"));
    }
}