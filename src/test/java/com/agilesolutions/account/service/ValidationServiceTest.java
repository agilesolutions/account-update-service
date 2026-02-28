// test/service/ValidationServiceTest.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.dto.AccountRequestDto;
import com.agilesolutions.account.domain.dto.AccountUpdateDto;
import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import com.agilesolutions.account.exception.BusinessValidationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Validation tests directly mapping COBOL EDIT-ACCOUNT-DATA sub-paragraphs:
 *
 *   EDIT-ACCTID           -> testValidateAccountId_*
 *   EDIT-ACCT-TYPE        -> testValidateAccountType_*
 *   EDIT-CREDIT-LIMIT     -> testValidateCreditLimit_*
 *   EDIT-EXPIRY-DATE      -> testValidateDates_*
 */
@DisplayName("ValidationService - COBOL EDIT-ACCOUNT-DATA paragraph tests")
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ─── COBOL EDIT-ACCTID tests ─────────────────────────────────────────────

    @ParameterizedTest(name = "Invalid accountId [{0}] should fail EDIT-ACCTID")
    @ValueSource(strings = {"", "ABC12345678", "1234567890", "123456789012", "1234 56789"})
    @DisplayName("COBOL EDIT-ACCTID: invalid ID formats rejected")
    void testValidateAccountId_invalid(String accountId) {
        AccountRequestDto dto = buildValidRequest();
        dto.setAccountId(accountId.isEmpty() ? null : accountId);

        assertThatThrownBy(() -> validationService.validateAccountData(dto))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    @DisplayName("COBOL EDIT-ACCTID: valid 11-digit numeric ID accepted")
    void testValidateAccountId_valid() {
        AccountRequestDto dto = buildValidRequest();
        dto.setAccountId("00001001001");
        assertThatCode(() -> validationService.validateAccountData(dto))
                .doesNotThrowAnyException();
    }

    // ─── COBOL EDIT-ACCT-TYPE tests ──────────────────────────────────────────

    @ParameterizedTest(name = "Invalid accountType [{0}] should fail EDIT-ACCT-TYPE")
    @ValueSource(strings = {"0", "3", "A", "10"})
    @DisplayName("COBOL EDIT-ACCT-TYPE: invalid types rejected")
    void testValidateAccountType_invalid(String accountType) {
        AccountRequestDto dto = buildValidRequest();
        dto.setAccountType(accountType);

        assertThatThrownBy(() -> validationService.validateAccountData(dto))
                .isInstanceOf(BusinessValidationException.class);
    }

    @ParameterizedTest(name = "Valid accountType [{0}]")
    @ValueSource(strings = {"1", "2"})
    @DisplayName("COBOL EDIT-ACCT-TYPE: valid type codes accepted")
    void testValidateAccountType_valid(String accountType) {
        AccountRequestDto dto = buildValidRequest();
        dto.setAccountType(accountType);
        assertThatCode(() -> validationService.validateAccountData(dto))
                .doesNotThrowAnyException();
    }

    // ─── COBOL EDIT-CREDIT-LIMIT tests ───────────────────────────────────────

    @Test
    @DisplayName("COBOL EDIT-CREDIT-LIMIT: cash limit exceeds credit limit rejected")
    void testValidateCreditLimit_cashExceedsCredit() {
        AccountRequestDto dto = buildValidRequest();
        dto.setCreditLimit(new BigDecimal("1000.00"));
        dto.setCashCreditLimit(new BigDecimal("2000.00")); // > creditLimit

        assertThatThrownBy(() -> validationService.validateAccountData(dto))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Cash credit limit cannot exceed");
    }

    @Test
    @DisplayName("COBOL EDIT-CREDIT-LIMIT: cash limit equal to credit limit accepted")
    void testValidateCreditLimit_cashEqualsCredit() {
        AccountRequestDto dto = buildValidRequest();
        dto.setCreditLimit(new BigDecimal("2000.00"));
        dto.setCashCreditLimit(new BigDecimal("2000.00"));
        assertThatCode(() -> validationService.validateAccountData(dto))
                .doesNotThrowAnyException();
    }

    // ─── COBOL EDIT-EXPIRY-DATE tests ────────────────────────────────────────

    @Test
    @DisplayName("COBOL EDIT-EXPIRY-DATE: expiry before open date rejected")
    void testValidateDates_expiryBeforeOpenDate() {
        AccountRequestDto dto = buildValidRequest();
        dto.setOpenDate(LocalDate.now().minusDays(10));
        dto.setExpiryDate(LocalDate.now().minusDays(20)); // before open date

        assertThatThrownBy(() -> validationService.validateAccountData(dto))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Expiry date must be after open date");
    }

    @Test
    @DisplayName("COBOL EDIT-OPEN-DATE: future open date rejected")
    void testValidateDates_futureOpenDate() {
        AccountRequestDto dto = buildValidRequest();
        dto.setOpenDate(LocalDate.now().plusDays(5));

        assertThatThrownBy(() -> validationService.validateAccountData(dto))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Open date cannot be in the future");
    }

    @Test
    @DisplayName("COBOL EDIT-REISSUE-DATE: reissue after expiry rejected")
    void testValidateDates_reissueAfterExpiry() {
        AccountRequestDto dto = buildValidRequest();
        dto.setOpenDate(LocalDate.now().minusDays(30));
        dto.setExpiryDate(LocalDate.now().plusYears(1));
        dto.setReissueDate(LocalDate.now().plusYears(2)); // after expiry

        assertThatThrownBy(() -> validationService.validateAccountData(dto))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Reissue date must be before expiry date");
    }

    // ─── Update validation tests ──────────────────────────────────────────────

    @Test
    @DisplayName("COBOL UPDATE EDIT-EXPIRY-DATE: valid update passes")
    void testValidateUpdateData_validUpdate() {
        AccountUpdateDto dto = AccountUpdateDto.builder()
                .expiryDate(LocalDate.now().plusYears(2))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .build();

        Account existing = Account.builder()
                .accountId("00001001001")
                .openDate(LocalDate.now().minusDays(30))
                .activeStatus(AccountStatus.ACTIVE)
                .build();

        assertThatCode(() -> validationService.validateUpdateData(dto, existing))
                .doesNotThrowAnyException();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AccountRequestDto buildValidRequest() {
        return AccountRequestDto.builder()
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .studentInd("N")
                .build();
    }
}