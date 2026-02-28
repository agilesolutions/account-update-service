package com.carddemo.exception;

/**
 * Card Not Found Exception
 * Maps to COBOL: RESP = DFHRESP(NOTFND) condition handling
 * COBOL: WHEN DFHRESP(NOTFND)
 *        MOVE 'Card record not found' TO WS-ERR-MSG
 */
public class CardNotFoundException extends RuntimeException {

    private final String cardIdentifier;

    public CardNotFoundException(String message) {
        super(message);
        this.cardIdentifier = null;
    }

    public CardNotFoundException(String message, String cardIdentifier) {
        super(message);
        this.cardIdentifier = cardIdentifier;
    }

    public String getCardIdentifier() {
        return cardIdentifier;
    }
}