// repository/AccountRepository.java
package com.agilesolutions.account.repository;

import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository replacing COBOL VSAM file READ/WRITE/REWRITE operations
 * on ACCTDAT (account master file)
 */
@Repository
public interface AccountRepository extends
        JpaRepository<Account, Long>,
        JpaSpecificationExecutor<Account> {

    // Replaces: READ ACCTDAT INTO WS-ACCOUNT-MASTER-RECORD
    Optional<Account> findByAccountId(String accountId);

    boolean existsByAccountId(String accountId);

    // Replaces: READ ACCTDAT WITH LOCK (optimistic lock via @Version)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId AND a.activeStatus = :status")
    Optional<Account> findByAccountIdAndActiveStatus(
            @Param("accountId") String accountId,
            @Param("activeStatus") AccountStatus status);

    // Search with pagination - replaces COBOL browse/scroll logic
    Page<Account> findByAccountTypeAndActiveStatus(
            String accountType,
            AccountStatus activeStatus,
            Pageable pageable);

    Page<Account> findByActiveStatus(AccountStatus activeStatus, Pageable pageable);

    // Find accounts over credit limit - replaces COBOL OVER-LIMIT-IND logic
    @Query("SELECT a FROM Account a WHERE a.currBal > a.creditLimit AND a.creditLimit > 0")
    Page<Account> findAccountsOverLimit(Pageable pageable);

    // Fuzzy search on account name
    @Query("SELECT a FROM Account a WHERE UPPER(a.accountName) LIKE UPPER(CONCAT('%', :name, '%'))")
    Page<Account> findByAccountNameContaining(@Param("name") String name, Pageable pageable);

    // Balance range query
    @Query("SELECT a FROM Account a WHERE a.currBal BETWEEN :min AND :max")
    Page<Account> findByBalanceRange(
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max,
            Pageable pageable);
}