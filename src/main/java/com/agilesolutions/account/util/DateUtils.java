// util/DateUtils.java
package com.agilesolutions.account.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Date utility replacing COBOL date manipulation routines:
 *
 *   EDIT-EXPIRY-DATE paragraph
 *   EDIT-OPEN-DATE   paragraph
 *   MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA
 */
public final class DateUtils {

    private DateUtils() {}

    // COBOL date format: PIC X(10) VALUE 'YYYY-MM-DD'
    private static final DateTimeFormatter COBOL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Parses COBOL-formatted date string PIC X(10)
     * Mirrors COBOL: MOVE WS-DATE-FIELD TO ACCT-OPEN-DATE
     */
    public static Optional<LocalDate> parseCobolDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equals("          ")) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(dateStr.trim(), COBOL_DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Formats date to COBOL PIC X(10) representation
     * Mirrors COBOL: MOVE ACCT-OPEN-DATE TO WS-DATE-OUT
     */
    public static String toCobolDateString(LocalDate date) {
        if (date == null) return "          "; // 10 spaces - COBOL LOW-VALUES equivalent
        return date.format(COBOL_DATE_FORMAT);
    }

    /**
     * Mirrors COBOL: MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA
     */
    public static LocalDate currentDate() {
        return LocalDate.now();
    }

    /**
     * Mirrors COBOL: EVALUATE TRUE
     *   WHEN ACCT-EXPIRY-DATE < FUNCTION CURRENT-DATE
     */
    public static boolean isExpired(LocalDate expiryDate) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Mirrors COBOL: EVALUATE TRUE
     *   WHEN ACCT-EXPIRY-DATE > ACCT-OPEN-DATE
     */
    public static boolean isAfter(LocalDate date, LocalDate reference) {
        if (date == null || reference == null) return false;
        return date.isAfter(reference);
    }
}