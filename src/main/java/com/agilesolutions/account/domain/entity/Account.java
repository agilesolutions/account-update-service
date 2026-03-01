// entity/Account.java
package com.agilesolutions.account.domain.entity;

import com.agilesolutions.account.domain.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps COBOL ACCOUNT-RECORD from COACTUP.CPY / COACTVWS.CPY
 * Fields directly correspond to COBOL group items and elementary items
 */
@Entity
@Table(name = "accounts", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // COBOL: ACCT-ID PIC X(11)
    @Column(name = "account_id", nullable = false, unique = true, length = 11)
    private String accountId;

    // COBOL: ACCT-ENTITY-CD PIC X(25) (account name)
    @Column(name = "account_name", length = 25)
    private String accountName;

    // COBOL: ACCT-TYPE-CD PIC X(1)
    @Column(name = "account_type", nullable = false, length = 1)
    private String accountType;

    // COBOL: ACCT-ACTIVE-STATUS PIC X(1)
    @Enumerated(EnumType.STRING)
    @Column(name = "active_status", nullable = false, length = 1)
    private AccountStatus activeStatus;

    // COBOL: ACCT-CURR-BAL PIC S9(10)V99 COMP-3
    @Column(name = "curr_bal", precision = 10, scale = 2)
    private BigDecimal currBal;

    // COBOL: ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3
    @Column(name = "credit_limit", precision = 10, scale = 2)
    private BigDecimal creditLimit;

    // COBOL: ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3
    @Column(name = "cash_credit_limit", precision = 10, scale = 2)
    private BigDecimal cashCreditLimit;

    // COBOL: ACCT-OPEN-DATE PIC X(10)
    @Column(name = "open_date")
    private LocalDate openDate;

    // COBOL: ACCT-EXPIRY-DATE PIC X(10)
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // COBOL: ACCT-REISSUE-DATE PIC X(10)
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    // COBOL: ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3
    @Column(name = "curr_cycle_credit", precision = 10, scale = 2)
    private BigDecimal currCycleCredit;

    // COBOL: ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3
    @Column(name = "curr_cycle_debit", precision = 10, scale = 2)
    private BigDecimal currCycleDebit;

    // COBOL: ACCT-ADDR-ZIP PIC X(10)
    @Column(name = "addr_zip", length = 10)
    private String addrZip;

    // COBOL: ACCT-ADDR-STATE PIC X(20)
    @Column(name = "addr_state", length = 20)
    private String addrState;

    // COBOL: ACCT-ADDR-COUNTRY PIC X(20)
    @Column(name = "addr_country", length = 20)
    private String addrCountry;

    // COBOL: ACCT-ADDR-LINE-1 PIC X(50)
    @Column(name = "addr_line1", length = 50)
    private String addrLine1;

    // COBOL: ACCT-ADDR-LINE-2 PIC X(50)
    @Column(name = "addr_line2", length = 50)
    private String addrLine2;

    // COBOL: ACCT-PHONE-NUMBER-1 PIC X(15)
    @Column(name = "phone_number_1", length = 15)
    private String phoneNumber1;

    // COBOL: ACCT-PHONE-NUMBER-2 PIC X(15)
    @Column(name = "phone_number_2", length = 15)
    private String phoneNumber2;

    // COBOL: ACCT-GROUP-ID PIC X(10)
    @Column(name = "group_id", length = 10)
    private String groupId;

    // COBOL: ACCT-STUDENT-IND PIC X(1)
    @Column(name = "student_ind", length = 1)
    private String studentInd;

    // COBOL: ACCT-OVER-LIMIT-IND PIC X(1)
    @Column(name = "over_limit_ind", length = 1)
    private String overLimitInd;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // COBOL: Derived field - ACCT-AVAIL-CREDIT
    @Transient
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null || currBal == null) return BigDecimal.ZERO;
        return creditLimit.subtract(currBal);
    }
}