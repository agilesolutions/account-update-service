// util/AccountConstants.java
package com.agilesolutions.account.util;

/**
 * Constants replacing COBOL 01-level WORKING-STORAGE literals
 *
 * COBOL equivalents:
 *   01 WS-MISC-STORAGE.
 *     05 WS-RETURN-CODE          PIC X(04) VALUE SPACES.
 *     05 WS-ACCTID-ERROR-FLG     PIC X(1)  VALUE 'N'.
 *     05 WS-DATE-ERROR-FLG       PIC X(1)  VALUE 'N'.
 */
public final class AccountConstants {

    private AccountConstants() {}

    // ─── File Status codes (COBOL FILE STATUS equivalents) ──────────────────
    public static final String FS_SUCCESS       = "00"; // COBOL FILE STATUS '00'
    public static final String FS_NOT_FOUND     = "23"; // COBOL FILE STATUS '23'
    public static final String FS_DUPLICATE_KEY = "22"; // COBOL FILE STATUS '22'
    public static final String FS_LOCKED        = "09"; // COBOL FILE STATUS '09'

    // ─── Application error codes (WS-RETURN-CODE values) ────────────────────
    public static final String ERR_ACCOUNT_NOT_FOUND   = "ACCT-0001";
    public static final String ERR_ACCOUNT_EXISTS      = "ACCT-0002";
    public static final String ERR_ACCOUNT_INACTIVE    = "ACCT-0003";
    public static final String ERR_VALIDATION_FAILED   = "ACCT-0004";
    public static final String ERR_CONCURRENT_UPDATE   = "ACCT-0005";
    public static final String ERR_INTERNAL            = "ACCT-9999";

    // ─── Indicator values (COBOL PIC X(1) flag fields) ──────────────────────
    public static final String IND_YES = "Y"; // COBOL 'Y'
    public static final String IND_NO  = "N"; // COBOL 'N'

    // ─── Account type codes (COBOL ACCT-TYPE-CD values) ─────────────────────
    public static final String ACCT_TYPE_CREDIT = "1"; // COBOL ACCT-TYPE-CD = '1'
    public static final String ACCT_TYPE_DEBIT  = "2"; // COBOL ACCT-TYPE-CD = '2'

    // ─── Active status codes (COBOL ACCT-ACTIVE-STATUS values) ─────────────
    public static final String STATUS_ACTIVE   = "Y"; // COBOL 'Y'
    public static final String STATUS_INACTIVE = "N"; // COBOL 'N'

    // ─── Field lengths (COBOL PIC clauses) ──────────────────────────────────
    public static final int ACCT_ID_LENGTH    = 11;  // PIC X(11)
    public static final int ACCT_NAME_LENGTH  = 25;  // PIC X(25)
    public static final int ACCT_TYPE_LENGTH  = 1;   // PIC X(1)
    public static final int ADDR_ZIP_LENGTH   = 10;  // PIC X(10)
    public static final int ADDR_STATE_LENGTH = 20;  // PIC X(20)
    public static final int PHONE_LENGTH      = 15;  // PIC X(15)

    // ─── Pagination defaults ─────────────────────────────────────────────────
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE     = 100;
}