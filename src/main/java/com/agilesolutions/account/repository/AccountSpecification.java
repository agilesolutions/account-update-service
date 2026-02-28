// repository/AccountSpecification.java
package com.agilesolutions.account.repository;

import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications replacing COBOL dynamic READ with key qualifiers
 * and conditional WHERE clauses in ACCTDAT browse logic
 */
public class AccountSpecification {

    private AccountSpecification() {}

    public static Specification<Account> buildSearchSpec(
            String accountId,
            String accountType,
            AccountStatus activeStatus,
            String accountName,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            LocalDate expiryFrom,
            LocalDate expiryTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // COBOL: READ ACCTDAT KEY = ACCT-ID
            if (StringUtils.hasText(accountId)) {
                predicates.add(cb.like(root.get("accountId"),
                        accountId.trim() + "%"));
            }

            // COBOL: EVALUATE ACCT-TYPE-CD
            if (StringUtils.hasText(accountType)) {
                predicates.add(cb.equal(root.get("accountType"), accountType));
            }

            // COBOL: IF ACCT-ACTIVE-STATUS = 'Y'
            if (activeStatus != null) {
                predicates.add(cb.equal(root.get("activeStatus"), activeStatus));
            }

            // COBOL: SEARCH ACCT-ENTITY-CD (name contains)
            if (StringUtils.hasText(accountName)) {
                predicates.add(cb.like(
                        cb.upper(root.get("accountName")),
                        "%" + accountName.trim().toUpperCase() + "%"));
            }

            // COBOL: IF ACCT-CURR-BAL >= WS-MIN-BAL
            if (minBalance != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("currBal"), minBalance));
            }

            // COBOL: IF ACCT-CURR-BAL <= WS-MAX-BAL
            if (maxBalance != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("currBal"), maxBalance));
            }

            // COBOL: IF ACCT-EXPIRY-DATE >= WS-EXPIRY-FROM
            if (expiryFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("expiryDate"), expiryFrom));
            }

            // COBOL: IF ACCT-EXPIRY-DATE <= WS-EXPIRY-TO
            if (expiryTo != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("expiryDate"), expiryTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}