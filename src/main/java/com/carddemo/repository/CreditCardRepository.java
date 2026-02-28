package com.carddemo.repository;

import com.carddemo.model.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Credit Card Repository
 * Replaces COBOL VSAM/DB2 file operations in COCRDUPC
 * - READ CARD-FILE → findByCardNumber()
 * - REWRITE CARD-FILE → save()
 */
@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    /**
     * Find card by card number - replaces COBOL READ CARD-FILE
     * COBOL: EXEC CICS READ FILE('CARDDAT')
     *         RIDFLD(WS-CARD-RID-CARDNUM)
     */
    Optional<CreditCard> findByCardNumber(String cardNumber);

    /**
     * Find card by card number and account ID - cross validation
     * COBOL: IF CDEMO-CARD-ACCT-ID NOT = WS-ACCT-ID
     */
    @Query("SELECT c FROM CreditCard c WHERE c.cardNumber = :cardNumber AND c.accountId = :accountId")
    Optional<CreditCard> findByCardNumberAndAccountId(
            @Param("cardNumber") String cardNumber,
            @Param("accountId") String accountId
    );

    /**
     * Check if card exists by card number
     */
    boolean existsByCardNumber(String cardNumber);
}