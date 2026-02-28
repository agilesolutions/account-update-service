// test/service/AccountServiceTest.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.dto.AccountRequestDto;
import com.agilesolutions.account.domain.dto.AccountResponseDto;
import com.agilesolutions.account.domain.dto.AccountUpdateDto;
import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import com.agilesolutions.account.exception.AccountNotFoundException;
import com.agilesolutions.account.exception.BusinessValidationException;
import com.agilesolutions.account.mapper.AccountMapper;
import com.agilesolutions.account.repository.AccountRepository;
import com.agilesolutions.account.util.AccountConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests mirroring COBOL COACTUPC paragraph logic:
 *   testCreateAccount_success        -> PROCESS-ENTER-KEY (happy path)
 *   testCreateAccount_duplicate      -> EDIT-ACCTID duplicate check
 *   testUpdateAccount_inactive       -> EDIT-ACCOUNT-DATA inactive guard
 *   testGetAccount_notFound          -> FILE STATUS '23' handling
 *   testCreateAccount_overLimit      -> COMPUTE OVER-LIMIT-IND logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService - COBOL COACTUPC paragraph unit tests")
class AccountServiceTest {

    @Mock private AccountRepository   accountRepository;
    @Mock private AccountMapper       accountMapper;
    @Mock private ValidationService   validationService;
    @Mock private AuditService        auditService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private AccountRequestDto validRequest;
    private Account           sampleAccount;
    private AccountResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = AccountRequestDto.builder()
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .addrZip("10001")
                .addrState("NY")
                .addrCountry("USA")
                .addrLine1("123 Main St")
                .phoneNumber1("+12125551234")
                .studentInd("N")
                .build();

        sampleAccount = Account.builder()
                .id(1L)
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .overLimitInd("N")
                .version(0L)
                .build();

        sampleResponse = AccountResponseDto.builder()
                .id(1L)
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .availableCredit(new BigDecimal("3500.00"))
                .build();
    }

    // ─── CREATE tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("COBOL PROCESS-ENTER-KEY: successful account creation")
    void testCreateAccount_success() {
        when(accountRepository.existsByAccountId("00001001001")).thenReturn(false);
        when(accountMapper.toEntity(any(AccountRequestDto.class))).thenReturn(sampleAccount);
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountMapper.toResponseDto(any(Account.class))).thenReturn(sampleResponse);

        AccountResponseDto result = accountService.createAccount(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo("00001001001");
        assertThat(result.getCurrBal()).isEqualByComparingTo("1500.00");

        verify(accountRepository).existsByAccountId("00001001001");
        verify(validationService).validateAccountData(validRequest);
        verify(accountRepository).save(any(Account.class));
        verify(auditService).logCreate(eq("ACCOUNT"), eq("00001001001"), any());
    }

    @Test
    @DisplayName("COBOL EDIT-ACCTID: duplicate account ID rejected")
    void testCreateAccount_duplicateId_throwsConflict() {
        when(accountRepository.existsByAccountId("00001001001")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(validRequest))
                .isInstanceOf(BusinessValidationException.class)
                .satisfies(ex -> {
                    BusinessValidationException bve = (BusinessValidationException) ex;
                    assertThat(bve.getErrorCode()).isEqualTo(AccountConstants.ERR_ACCOUNT_EXISTS);
                });

        verify(accountRepository, never()).save(any());
        verify(validationService, never()).validateAccountData(any());
    }

    @Test
    @DisplayName("COBOL COMPUTE OVER-LIMIT-IND: balance exceeds credit limit")
    void testCreateAccount_overLimit_setsIndicator() {
        validRequest.setCurrBal(new BigDecimal("6000.00")); // > creditLimit 5000
        Account overLimitAccount = sampleAccount.builder()
                .currBal(new BigDecimal("6000.00"))
                .overLimitInd("N") // starts as N, should become Y
                .build();

        when(accountRepository.existsByAccountId(any())).thenReturn(false);
        when(accountMapper.toEntity(any())).thenReturn(overLimitAccount);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account saved = inv.getArgument(0);
            assertThat(saved.getOverLimitInd()).isEqualTo("Y");
            return saved;
        });
        when(accountMapper.toResponseDto(any())).thenReturn(sampleResponse);

        accountService.createAccount(validRequest);

        verify(accountRepository).save(argThat(a -> "Y".equals(a.getOverLimitInd())));
    }

    // ─── GET tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("COBOL GET-ACCT-DATA: successful account retrieval")
    void testGetAccount_success() {
        when(accountRepository.findByAccountId("00001001001"))
                .thenReturn(Optional.of(sampleAccount));
        when(accountMapper.toResponseDto(sampleAccount)).thenReturn(sampleResponse);

        AccountResponseDto result = accountService.getAccountById("00001001001");

        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo("00001001001");
        verify(accountRepository).findByAccountId("00001001001");
    }

    @Test
    @DisplayName("COBOL FILE STATUS '23': account not found throws exception")
    void testGetAccount_notFound_throwsException() {
        when(accountRepository.findByAccountId("99999999999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById("99999999999"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("99999999999")
                .satisfies(ex -> {
                    AccountNotFoundException anfe = (AccountNotFoundException) ex;
                    assertThat(anfe.getErrorCode())
                            .isEqualTo(AccountConstants.ERR_ACCOUNT_NOT_FOUND);
                });
    }

    // ─── UPDATE tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("COBOL UPDATE-ACCOUNT-INFO: successful account update")
    void testUpdateAccount_success() {
        AccountUpdateDto updateDto = AccountUpdateDto.builder()
                .accountName("UPDATED NAME")
                .creditLimit(new BigDecimal("7000.00"))
                .addrLine1("456 New St")
                .build();

        when(accountRepository.findByAccountId("00001001001"))
                .thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountMapper.toResponseDto(any(Account.class))).thenReturn(sampleResponse);
        when(auditService.serializeAccount(any())).thenReturn("{}");

        AccountResponseDto result = accountService.updateAccount("00001001001", updateDto);

        assertThat(result).isNotNull();
        verify(validationService).validateUpdateData(eq(updateDto), eq(sampleAccount));
        verify(accountRepository).save(any(Account.class));
        verify(auditService).logUpdate(eq("ACCOUNT"), eq("00001001001"), any(), any());
    }

    @Test
    @DisplayName("COBOL EDIT-ACCOUNT-DATA: inactive account update rejected")
    void testUpdateAccount_inactiveAccount_rejected() {
        sampleAccount.setActiveStatus(AccountStatus.INACTIVE);
        when(accountRepository.findByAccountId("00001001001"))
                .thenReturn(Optional.of(sampleAccount));

        AccountUpdateDto updateDto = AccountUpdateDto.builder()
                .accountName("NEW NAME")
                .build();

        assertThatThrownBy(() -> accountService.updateAccount("00001001001", updateDto))
                .isInstanceOf(BusinessValidationException.class)
                .satisfies(ex -> {
                    BusinessValidationException bve = (BusinessValidationException) ex;
                    assertThat(bve.getErrorCode()).isEqualTo(AccountConstants.ERR_ACCOUNT_INACTIVE);
                });

        verify(accountRepository, never()).save(any());
    }

    // ─── DELETE tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("COBOL logical delete: ACCT-ACTIVE-STATUS set to 'N'")
    void testDeactivateAccount_success() {
        when(accountRepository.findByAccountId("00001001001"))
                .thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(auditService.serializeAccount(any())).thenReturn("{}");

        accountService.deleteAccount("00001001001");

        verify(accountRepository).save(argThat(a ->
                a.getActiveStatus() == AccountStatus.INACTIVE));
        verify(auditService).logDelete(eq("ACCOUNT"), eq("00001001001"), any());
    }
}