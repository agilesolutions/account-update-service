// test/util/DateUtilsTest.java
package com.agilesolutions.account.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for COBOL date manipulation paragraph equivalents
 */
@DisplayName("DateUtils - COBOL date paragraph tests")
class DateUtilsTest {

    // ─── parseCobolDate ───────────────────────────────────────────────────────

    @Test
    @DisplayName("parseCobolDate: valid date string parsed correctly")
    void testParseCobolDate_valid() {
        Optional<LocalDate> result = DateUtils.parseCobolDate("2024-06-15");
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @ParameterizedTest(name = "parseCobolDate: [{0}] returns empty")
    @NullAndEmptySource
    @ValueSource(strings = {"          ", "INVALID", "2024/06/15", "15-06-2024"})
    @DisplayName("parseCobolDate: invalid inputs return empty Optional")
    void testParseCobolDate_invalid_returnsEmpty(String input) {
        Optional<LocalDate> result = DateUtils.parseCobolDate(input);
        assertThat(result).isEmpty();
    }

    // ─── toCobolDateString ────────────────────────────────────────────────────

    @Test
    @DisplayName("toCobolDateString: valid date formats correctly")
    void testToCobolDateString_validDate() {
        String result = DateUtils.toCobolDateString(LocalDate.of(2024, 6, 15));
        assertThat(result).isEqualTo("2024-06-15");
        assertThat(result).hasSize(10); // COBOL PIC X(10)
    }

    @Test
    @DisplayName("toCobolDateString: null returns 10 spaces (COBOL LOW-VALUES)")
    void testToCobolDateString_null_returnsTenSpaces() {
        String result = DateUtils.toCobolDateString(null);
        assertThat(result).isEqualTo("          ");
        assertThat(result).hasSize(10);
    }

    // ─── isExpired ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isExpired: past date returns true (COBOL EXPIRY-DATE < CURRENT-DATE)")
    void testIsExpired_pastDate_returnsTrue() {
        assertThat(DateUtils.isExpired(LocalDate.now().minusDays(1))).isTrue();
    }

    @Test
    @DisplayName("isExpired: future date returns false")
    void testIsExpired_futureDate_returnsFalse() {
        assertThat(DateUtils.isExpired(LocalDate.now().plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("isExpired: today returns false")
    void testIsExpired_today_returnsFalse() {
        assertThat(DateUtils.isExpired(LocalDate.now())).isFalse();
    }

    @Test
    @DisplayName("isExpired: null date returns false")
    void testIsExpired_null_returnsFalse() {
        assertThat(DateUtils.isExpired(null)).isFalse();
    }

    // ─── isAfter ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isAfter: date after reference returns true")
    void testIsAfter_dateAfterReference_returnsTrue() {
        assertThat(DateUtils.isAfter(
                LocalDate.now().plusDays(1), LocalDate.now())).isTrue();
    }

    @Test
    @DisplayName("isAfter: date before reference returns false")
    void testIsAfter_dateBeforeReference_returnsFalse() {
        assertThat(DateUtils.isAfter(
                LocalDate.now().minusDays(1), LocalDate.now())).isFalse();
    }

    @Test
    @DisplayName("isAfter: null inputs return false")
    void testIsAfter_nullInputs_returnsFalse() {
        assertThat(DateUtils.isAfter(null, LocalDate.now())).isFalse();
        assertThat(DateUtils.isAfter(LocalDate.now(), null)).isFalse();
        assertThat(DateUtils.isAfter(null, null)).isFalse();
    }

    // ─── currentDate ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("currentDate: returns today (COBOL FUNCTION CURRENT-DATE)")
    void testCurrentDate_returnsToday() {
        assertThat(DateUtils.currentDate()).isEqualTo(LocalDate.now());
    }
}