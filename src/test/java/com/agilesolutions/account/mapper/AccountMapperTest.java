// test/repository/AccountRepositoryTest.java
package com.agilesolutions.account.repository;

import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository slice tests using H2 in-memory database
 * Tests COBOL VSAM file operation equivalents
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AccountRepository - VSAM file operation tests")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    private Account activeCredit;
    private Account activeDebit;
    private Account inactiveCredit;
    private Account overLimitAccount;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        activeCredit = accountRepository.save(Account.builder()
                .accountId("00001000001")
                .accountName("ACTIVE CREDIT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(60))
                .expiryDate(LocalDate.now().plusYears(2))
                .overLimitInd("N")
                .studentInd("N")
                .build());

        activeDebit = accountRepository.save(Account.builder()
                .accountId("00002000001")
                .accountName("ACTIVE DEBIT")
                .accountType("2")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("500.00"))
                .creditLimit(new BigDecimal("0.00"))
                .cashCreditLimit(new BigDecimal("0.00"))
                .openDate(LocalDate.now().minusDays(30))
                .overLimitInd("N")
                .studentInd("Y")
                .build());

        inactiveCredit = accountRepository.save(Account.builder()
                .accountId("00003000001")
                .accountName("INACTIVE ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.INACTIVE)
                .currBal(new BigDecimal("0.00"))
                .creditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(365))
                .overLimitInd("N")
                .studentInd("N")
                .build());

        overLimitAccount = accountRepository.save(Account.builder()
                .accountId("00004000001")
                .accountName("OVER LIMIT ACCT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("7000.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(90))
                .overLimitInd("Y")
                .studentInd("N")
                .build());
    }

    // ─── findByAccountId (COBOL READ ACCTDAT KEY = ACCT-ID) ─────────────────

    @Test
    @DisplayName("findByAccountId: existing account found")
    void testFindByAccountId_exists_returnsAccount() {
        Optional<Account> result = accountRepository.findByAccountId("00001000001");

        assertThat(result).isPresent();
        assertThat(result.get().getAccountName()).isEqualTo("ACTIVE CREDIT");
        assertThat(result.get().getAccountType()).isEqualTo("1");
        assertThat(result.get().getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByAccountId: non-existent returns empty (COBOL FILE STATUS '23')")
    void testFindByAccountId_notExists_returnsEmpty() {
        Optional<Account> result = accountRepository.findByAccountId("99999999999");
        assertThat(result).isEmpty();
    }

    // ─── existsByAccountId (COBOL duplicate key check) ───────────────────────

    @Test
    @DisplayName("existsByAccountId: true for existing account")
    void testExistsByAccountId_exists_returnsTrue() {
        assertThat(accountRepository.existsByAccountId("00001000001")).isTrue();
    }

    @Test
    @DisplayName("existsByAccountId: false for non-existent account")
    void testExistsByAccountId_notExists_returnsFalse() {
        assertThat(accountRepository.existsByAccountId("00009999999")).isFalse();
    }

    // ─── findByAccountIdAndActiveStatus ──────────────────────────────────────

    @Test
    @DisplayName("findByAccountIdAndActiveStatus: active account found")
    void testFindByAccountIdAndActiveStatus_active_found() {
        Optional<Account> result = accountRepository
                .findByAccountIdAndActiveStatus("00001000001", AccountStatus.ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.get().getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByAccountIdAndActiveStatus: wrong status returns empty")
    void testFindByAccountIdAndActiveStatus_wrongStatus_empty() {
        Optional<Account> result = accountRepository
                .findByAccountIdAndActiveStatus("00001000001", AccountStatus.INACTIVE);

        assertThat(result).isEmpty();
    }

    // ─── findByAccountTypeAndActiveStatus (COBOL keyed browse) ───────────────

    @Test
    @DisplayName("findByAccountTypeAndActiveStatus: returns matching type and status")
    void testFindByTypeAndStatus_returnsCorrectRecords() {
        Page<Account> result = accountRepository.findByAccountTypeAndActiveStatus(
                "1", AccountStatus.ACTIVE,
                PageRequest.of(0, 10, Sort.by("accountId")));

        assertThat(result.getContent())
                .hasSize(2)  // activeCredit + overLimitAccount
                .allMatch(a -> "1".equals(a.getAccountType()))
                .allMatch(a -> AccountStatus.ACTIVE == a.getActiveStatus());
    }

    @Test
    @DisplayName("findByActiveStatus: inactive accounts only")
    void testFindByActiveStatus_inactive_returnsInactiveOnly() {
        Page<Account> result = accountRepository.findByActiveStatus(
                AccountStatus.INACTIVE,
                PageRequest.of(0, 10));

        assertThat(result.getContent())
                .hasSize(1)
                .allMatch(a -> AccountStatus.INACTIVE == a.getActiveStatus());
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo("00003000001");
    }

    // ─── findAccountsOverLimit (COBOL OVER-LIMIT-IND logic) ──────────────────

    @Test
    @DisplayName("findAccountsOverLimit: returns only accounts with currBal > creditLimit")
    void testFindAccountsOverLimit_returnsCorrectAccounts() {
        Page<Account> result = accountRepository.findAccountsOverLimit(
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo("00004000001");
        assertThat(result.getContent().get(0).getCurrBal())
                .isGreaterThan(result.getContent().get(0).getCreditLimit());
    }

    // ─── findByAccountNameContaining (COBOL name search) ─────────────────────

    @Test
    @DisplayName("findByAccountNameContaining: case-insensitive partial match")
    void testFindByAccountNameContaining_caseInsensitive() {
        Page<Account> result = accountRepository.findByAccountNameContaining(
                "active",
                PageRequest.of(0, 10));

        // Matches "ACTIVE CREDIT", "ACTIVE DEBIT", "INACTIVE ACCOUNT"
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("findByAccountNameContaining: exact substring match")
    void testFindByAccountNameContaining_exactSubstring() {
        Page<Account> result = accountRepository.findByAccountNameContaining(
                "CREDIT",
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo("00001000001");
    }

    @Test
    @DisplayName("findByAccountNameContaining: no match returns empty page")
    void testFindByAccountNameContaining_noMatch_returnsEmpty() {
        Page<Account> result = accountRepository.findByAccountNameContaining(
                "NONEXISTENT",
                PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ─── findByBalanceRange ───────────────────────────────────────────────────

    @Test
    @DisplayName("findByBalanceRange: returns accounts within balance range")
    void testFindByBalanceRange_returnsCorrectAccounts() {
        Page<Account> result = accountRepository.findByBalanceRange(
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                PageRequest.of(0, 10));

        // Matches: activeCredit (1500), activeDebit (500)
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(a -> a.getCurrBal().compareTo(new BigDecimal("500.00")) >= 0
                        && a.getCurrBal().compareTo(new BigDecimal("2000.00")) <= 0);
    }

    // ─── Optimistic locking (@Version) ───────────────────────────────────────

    @Test
    @DisplayName("@Version: version increments on each save (COBOL REWRITE equivalent)")
    void testVersioning_incrementsOnSave() {
        Account account = accountRepository.findByAccountId("00001000001").orElseThrow();
        assertThat(account.getVersion()).isZero();

        account.setAccountName("MODIFIED NAME");
        Account updated = accountRepository.saveAndFlush(account);
        assertThat(updated.getVersion()).isEqualTo(1L);

        updated.setAccountName("MODIFIED AGAIN");
        Account updated2 = accountRepository.saveAndFlush(updated);
        assertThat(updated2.getVersion()).isEqualTo(2L);
    }

    // ─── Pagination and sorting ───────────────────────────────────────────────

    @Test
    @DisplayName("findAll: paginated and sorted by accountId ascending")
    void testFindAll_paginatedSortedByAccountId() {
        Page<Account> firstPage = accountRepository.findAll(
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "accountId")));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent().get(0).getAccountId()).isEqualTo("00001000001");
        assertThat(firstPage.getContent().get(1).getAccountId()).isEqualTo("00002000001");
    }

    @Test
    @DisplayName("findAll: second page returns remaining records")
    void testFindAll_secondPage_returnsRemainder() {
        Page<Account> secondPage = accountRepository.findAll(
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "accountId")));

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.isLast()).isTrue();
        assertThat(secondPage.getContent().get(0).getAccountId()).isEqualTo("00003000001");
    }
}