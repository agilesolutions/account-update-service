package com.carddemo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Credit Card Entity
 * Migrated from COBOL COCRDUPC.cbl - CARD-RECORD copybook structure
 */
@Entity
@Table(name = "credit_cards", indexes = {
        @Index(name = "idx_card_number", columnList = "card_number", unique = true),
        @Index(name = "idx_acct_id", columnList = "account_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "card_seq")
    @SequenceGenerator(name = "card_seq", sequenceName = "card_sequence", allocationSize = 1)
    private Long id;

    // Corresponds to COBOL: CDEMO-CARD-NUM (PIC X(16))
    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber;

    // Corresponds to COBOL: CDEMO-CARD-ACCT-ID (PIC 9(11))
    @Column(name = "account_id", nullable = false, length = 11)
    private String accountId;

    // Corresponds to COBOL: CDEMO-CARD-CVV-CD (PIC 9(3))
    @Column(name = "cvv_code", nullable = false, length = 3)
    private String cvvCode;

    // Corresponds to COBOL: CDEMO-CARD-EMBOSSED-NAME (PIC X(50))
    @Column(name = "embossed_name", nullable = false, length = 50)
    private String embossedName;

    // Corresponds to COBOL: CDEMO-CARD-EXPIRAION-DATE (PIC X(10))
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    // Corresponds to COBOL: CDEMO-CARD-ACTIVE-STATUS (PIC X(1))
    @Column(name = "active_status", nullable = false, length = 1)
    private String activeStatus;

    // Corresponds to COBOL: CDEMO-CARD-CREDIT-LIMIT (PIC S9(10)V99)
    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    // Corresponds to COBOL: CDEMO-CARD-CASH-CREDIT-LIMIT (PIC S9(10)V99)
    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    // Corresponds to COBOL: CDEMO-CARD-CURR-BAL (PIC S9(10)V99)
    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    // Corresponds to COBOL: CDEMO-CARD-CURR-CYC-CREDIT (PIC S9(10)V99)
    @Column(name = "current_cycle_credit", precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;

    // Corresponds to COBOL: CDEMO-CARD-CURR-CYC-DEBIT (PIC S9(10)V99)
    @Column(name = "current_cycle_debit", precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;

    // Corresponds to COBOL: CDEMO-CARD-GROUP-ID (PIC X(10))
    @Column(name = "group_id", length = 10)
    private String groupId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}