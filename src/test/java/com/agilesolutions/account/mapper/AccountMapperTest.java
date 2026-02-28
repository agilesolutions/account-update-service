// test/mapper/AccountMapperTest.java
package com.agilesolutions.account.mapper;

import com.agilesolutions.account.domain.dto.AccountRequestDto;
import com.agilesolutions.account.domain.dto.AccountResponseDto;
import com.agilesolutions.account.domain.dto.AccountUpdateDto;
import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AccountMapper.
 *
 * Each test section maps directly to a COBOL MOVE statement group:
 *
 *   toEntity()             -> COBOL: MOVE commarea fields TO WS-ACCOUNT-MASTER-RECORD
 *   toResponseDto()        -> COBOL: MOVE WS-ACCOUNT-MASTER-RECORD TO output commarea
 *   updateEntityFromDto()  -> COBOL: MOVE modified screen fields TO WS-ACCOUNT-MASTER-RECORD
 *   resolveAccountTypeDesc -> COBOL: EVALUATE ACCT-TYPE-CD WHEN '1' WHEN '2'
 *   getAvailableCredit     -> COBOL: COMPUTE AVAIL-CREDIT = CREDIT-LIMIT - CURR-BAL
 */
@DisplayName("AccountMapper - COBOL MOVE statement and COMPUTE equivalents")
class AccountMapperTest {

    // Use MapStruct factory directly - no Spring context needed
    private final AccountMapper mapper = Mappers.getMapper(AccountMapper.class);

    // =========================================================================
    // toEntity() - AccountRequestDto -> Account
    // COBOL: MOVE commarea input fields TO WS-ACCOUNT-MASTER-RECORD
    // =========================================================================

    @Nested
    @DisplayName("toEntity() - COBOL MOVE commarea -> WS-ACCOUNT-MASTER-RECORD")
    class ToEntityTests {

        @Test
        @DisplayName("All request fields are mapped to entity correctly")
        void toEntity_allFields_mappedCorrectly() {
            AccountRequestDto dto = buildFullRequest();
            Account entity = mapper.toEntity(dto);

            // Identity fields - COBOL PIC X(11) / PIC X(25)
            assertThat(entity.getAccountId())
                    .as("COBOL: MOVE ACCT-ID TO WS-ACCT-ID")
                    .isEqualTo("00001001001");
            assertThat(entity.getAccountName())
                    .as("COBOL: MOVE ACCT-ENTITY-CD TO WS-ACCT-NAME")
                    .isEqualTo("JOHN DOE");

            // Type and status - COBOL PIC X(1)
            assertThat(entity.getAccountType())
                    .as("COBOL: MOVE ACCT-TYPE-CD TO WS-ACCT-TYPE")
                    .isEqualTo("1");
            assertThat(entity.getActiveStatus())
                    .as("COBOL: MOVE ACCT-ACTIVE-STATUS TO WS-ACTIVE-STATUS")
                    .isEqualTo(AccountStatus.ACTIVE);

            // Monetary fields - COBOL PIC S9(10)V99 COMP-3
            assertThat(entity.getCurrBal())
                    .as("COBOL: MOVE ACCT-CURR-BAL TO WS-CURR-BAL")
                    .isEqualByComparingTo("1500.00");
            assertThat(entity.getCreditLimit())
                    .as("COBOL: MOVE ACCT-CREDIT-LIMIT TO WS-CREDIT-LIMIT")
                    .isEqualByComparingTo("5000.00");
            assertThat(entity.getCashCreditLimit())
                    .as("COBOL: MOVE ACCT-CASH-CREDIT-LIMIT TO WS-CASH-CREDIT-LIMIT")
                    .isEqualByComparingTo("2000.00");

            // Date fields - COBOL PIC X(10)
            assertThat(entity.getOpenDate())
                    .as("COBOL: MOVE ACCT-OPEN-DATE TO WS-OPEN-DATE")
                    .isEqualTo(LocalDate.now().minusDays(30));
            assertThat(entity.getExpiryDate())
                    .as("COBOL: MOVE ACCT-EXPIRY-DATE TO WS-EXPIRY-DATE")
                    .isEqualTo(LocalDate.now().plusYears(2));
            assertThat(entity.getReissueDate())
                    .as("COBOL: MOVE ACCT-REISSUE-DATE TO WS-REISSUE-DATE")
                    .isEqualTo(LocalDate.now().plusYears(1));

            // Address fields - COBOL PIC X(50) / PIC X(20) / PIC X(10)
            assertThat(entity.getAddrLine1())
                    .as("COBOL: MOVE ACCT-ADDR-LINE-1 TO WS-ADDR-LINE-1")
                    .isEqualTo("123 Main St");
            assertThat(entity.getAddrLine2())
                    .as("COBOL: MOVE ACCT-ADDR-LINE-2 TO WS-ADDR-LINE-2")
                    .isEqualTo("Apt 4B");
            assertThat(entity.getAddrState())
                    .as("COBOL: MOVE ACCT-ADDR-STATE TO WS-ADDR-STATE")
                    .isEqualTo("NY");
            assertThat(entity.getAddrCountry())
                    .as("COBOL: MOVE ACCT-ADDR-COUNTRY TO WS-ADDR-COUNTRY")
                    .isEqualTo("USA");
            assertThat(entity.getAddrZip())
                    .as("COBOL: MOVE ACCT-ADDR-ZIP TO WS-ADDR-ZIP")
                    .isEqualTo("10001");

            // Phone fields - COBOL PIC X(15)
            assertThat(entity.getPhoneNumber1())
                    .as("COBOL: MOVE ACCT-PHONE-NUMBER-1 TO WS-PHONE-1")
                    .isEqualTo("+12125551234");
            assertThat(entity.getPhoneNumber2())
                    .as("COBOL: MOVE ACCT-PHONE-NUMBER-2 TO WS-PHONE-2")
                    .isEqualTo("+12125555678");

            // Indicator and group fields - COBOL PIC X(1) / PIC X(10)
            assertThat(entity.getGroupId())
                    .as("COBOL: MOVE ACCT-GROUP-ID TO WS-GROUP-ID")
                    .isEqualTo("GRP001");
            assertThat(entity.getStudentInd())
                    .as("COBOL: MOVE ACCT-STUDENT-IND TO WS-STUDENT-IND")
                    .isEqualTo("N");
        }

        @Test
        @DisplayName("Audit fields are never populated by mapper (server-managed)")
        void toEntity_auditFields_alwaysIgnored() {
            AccountRequestDto dto = buildFullRequest();
            Account entity = mapper.toEntity(dto);

            // These fields are managed by @CreatedBy / @CreatedDate / @Version
            // COBOL equivalent: populated by CICS COMMAREA server, not by screen input
            assertThat(entity.getId())
                    .as("Surrogate key must never come from request")
                    .isNull();
            assertThat(entity.getCreatedBy())
                    .as("COBOL: set by CICS ASSIGN USERID, not commarea")
                    .isNull();
            assertThat(entity.getCreatedAt())
                    .as("COBOL: set by CICS system clock, not commarea")
                    .isNull();
            assertThat(entity.getUpdatedBy())
                    .as("COBOL: set by CICS ASSIGN USERID on REWRITE")
                    .isNull();
            assertThat(entity.getUpdatedAt())
                    .as("COBOL: set by CICS system clock on REWRITE")
                    .isNull();
            assertThat(entity.getVersion())
                    .as("Optimistic lock version never from request")
                    .isNull();
        }

        @Test
        @DisplayName("Over-limit indicator is ignored (computed by service, not mapper)")
        void toEntity_overLimitInd_alwaysIgnored() {
            AccountRequestDto dto = buildFullRequest();
            Account entity = mapper.toEntity(dto);

            // COBOL: COMPUTE OVER-LIMIT-IND is done in service paragraph,
            // not during the MOVE commarea -> record operation
            assertThat(entity.getOverLimitInd())
                    .as("COBOL COMPUTE OVER-LIMIT-IND is done in service, not mapper")
                    .isNull();
        }

        @Test
        @DisplayName("Null optional fields in request produce null entity fields")
        void toEntity_nullOptionalFields_producesNullEntityFields() {
            AccountRequestDto dto = AccountRequestDto.builder()
                    .accountId("00001001001")
                    .accountType("1")
                    .activeStatus(AccountStatus.ACTIVE)
                    // All optional fields omitted
                    .build();

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getAccountId()).isEqualTo("00001001001");
            assertThat(entity.getAccountName()).isNull();
            assertThat(entity.getCurrBal()).isNull();
            assertThat(entity.getCreditLimit()).isNull();
            assertThat(entity.getCashCreditLimit()).isNull();
            assertThat(entity.getOpenDate()).isNull();
            assertThat(entity.getExpiryDate()).isNull();
            assertThat(entity.getReissueDate()).isNull();
            assertThat(entity.getAddrLine1()).isNull();
            assertThat(entity.getAddrLine2()).isNull();
            assertThat(entity.getAddrState()).isNull();
            assertThat(entity.getAddrCountry()).isNull();
            assertThat(entity.getAddrZip()).isNull();
            assertThat(entity.getPhoneNumber1()).isNull();
            assertThat(entity.getPhoneNumber2()).isNull();
            assertThat(entity.getGroupId()).isNull();
            assertThat(entity.getStudentInd()).isNull();
        }

        @Test
        @DisplayName("AccountStatus ACTIVE maps to entity enum correctly")
        void toEntity_activeStatus_ACTIVE_mappedToEnum() {
            AccountRequestDto dto = buildMinimalRequest();
            dto.setActiveStatus(AccountStatus.ACTIVE);

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("AccountStatus INACTIVE maps to entity enum correctly")
        void toEntity_activeStatus_INACTIVE_mappedToEnum() {
            AccountRequestDto dto = buildMinimalRequest();
            dto.setActiveStatus(AccountStatus.INACTIVE);

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getActiveStatus()).isEqualTo(AccountStatus.INACTIVE);
        }

        @Test
        @DisplayName("BigDecimal monetary values preserve scale")
        void toEntity_bigDecimalFields_preserveScale() {
            AccountRequestDto dto = buildMinimalRequest();
            dto.setCurrBal(new BigDecimal("1234.56"));
            dto.setCreditLimit(new BigDecimal("9999.99"));
            dto.setCashCreditLimit(new BigDecimal("0.01"));

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getCurrBal()).isEqualByComparingTo("1234.56");
            assertThat(entity.getCreditLimit()).isEqualByComparingTo("9999.99");
            assertThat(entity.getCashCreditLimit()).isEqualByComparingTo("0.01");
        }

        @Test
        @DisplayName("Zero monetary values map correctly (COBOL ZERO equivalent)")
        void toEntity_zeroMonetaryValues_mappedCorrectly() {
            AccountRequestDto dto = buildMinimalRequest();
            dto.setCurrBal(BigDecimal.ZERO);
            dto.setCreditLimit(BigDecimal.ZERO);
            dto.setCashCreditLimit(BigDecimal.ZERO);

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getCurrBal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(entity.getCreditLimit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(entity.getCashCreditLimit()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debit account type '2' maps correctly")
        void toEntity_accountTypeDebit_mappedCorrectly() {
            AccountRequestDto dto = buildMinimalRequest();
            dto.setAccountType("2");

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getAccountType()).isEqualTo("2");
        }

        @Test
        @DisplayName("Student indicator 'Y' maps correctly")
        void toEntity_studentIndY_mappedCorrectly() {
            AccountRequestDto dto = buildMinimalRequest();
            dto.setStudentInd("Y");

            Account entity = mapper.toEntity(dto);

            assertThat(entity.getStudentInd()).isEqualTo("Y");
        }
    }

    // =========================================================================
    // toResponseDto() - Account -> AccountResponseDto
    // COBOL: MOVE WS-ACCOUNT-MASTER-RECORD TO output commarea
    // =========================================================================

    @Nested
    @DisplayName("toResponseDto() - COBOL MOVE WS-ACCOUNT-MASTER-RECORD -> output commarea")
    class ToResponseDtoTests {

        @Test
        @DisplayName("All entity fields are mapped to response DTO correctly")
        void toResponseDto_allFields_mappedCorrectly() {
            Account entity = buildFullAccount();
            AccountResponseDto dto = mapper.toResponseDto(entity);

            // Identity
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getAccountId()).isEqualTo("00001001001");
            assertThat(dto.getAccountName()).isEqualTo("JOHN DOE");
            assertThat(dto.getAccountType()).isEqualTo("1");
            assertThat(dto.getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);

            // Monetary
            assertThat(dto.getCurrBal()).isEqualByComparingTo("1500.00");
            assertThat(dto.getCreditLimit()).isEqualByComparingTo("5000.00");
            assertThat(dto.getCashCreditLimit()).isEqualByComparingTo("2000.00");

            // Dates
            assertThat(dto.getOpenDate()).isEqualTo(LocalDate.now().minusDays(30));
            assertThat(dto.getExpiryDate()).isEqualTo(LocalDate.now().plusYears(2));
            assertThat(dto.getReissueDate()).isEqualTo(LocalDate.now().plusYears(1));

            // Address
            assertThat(dto.getAddrLine1()).isEqualTo("123 Main St");
            assertThat(dto.getAddrLine2()).isEqualTo("Apt 4B");
            assertThat(dto.getAddrState()).isEqualTo("NY");
            assertThat(dto.getAddrCountry()).isEqualTo("USA");
            assertThat(dto.getAddrZip()).isEqualTo("10001");

            // Phone
            assertThat(dto.getPhoneNumber1()).isEqualTo("+12125551234");
            assertThat(dto.getPhoneNumber2()).isEqualTo("+12125555678");

            // Indicators
            assertThat(dto.getGroupId()).isEqualTo("GRP001");
            assertThat(dto.getStudentInd()).isEqualTo("N");
            assertThat(dto.getOverLimitInd()).isEqualTo("N");

            // Audit fields
            assertThat(dto.getCreatedBy()).isEqualTo("SYSTEM");
            assertThat(dto.getCreatedAt()).isNotNull();
            assertThat(dto.getUpdatedBy()).isEqualTo("admin");
            assertThat(dto.getUpdatedAt()).isNotNull();
            assertThat(dto.getVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("accountTypeDescription resolved for type '1' (COBOL EVALUATE WHEN '1')")
        void toResponseDto_accountType1_descriptionIsCredit() {
            Account entity = buildFullAccount();
            entity.setAccountType("1");

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAccountTypeDescription())
                    .as("COBOL EVALUATE ACCT-TYPE-CD WHEN '1' -> 'Credit'")
                    .isEqualTo("Credit");
        }

        @Test
        @DisplayName("accountTypeDescription resolved for type '2' (COBOL EVALUATE WHEN '2')")
        void toResponseDto_accountType2_descriptionIsDebit() {
            Account entity = buildFullAccount();
            entity.setAccountType("2");

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAccountTypeDescription())
                    .as("COBOL EVALUATE ACCT-TYPE-CD WHEN '2' -> 'Debit'")
                    .isEqualTo("Debit");
        }

        @ParameterizedTest(name = "accountType [{0}] resolves to description [{1}]")
        @CsvSource({
                "1, Credit",
                "2, Debit",
                "3, Unknown",
                "9, Unknown",
                "X, Unknown"
        })
        @DisplayName("resolveAccountTypeDesc covers all COBOL EVALUATE branches")
        void toResponseDto_accountTypeDescription_allBranches(
                String typeCode, String expectedDesc) {

            Account entity = buildFullAccount();
            entity.setAccountType(typeCode);

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAccountTypeDescription()).isEqualTo(expectedDesc);
        }

        @Test
        @DisplayName("accountTypeDescription is 'Unknown' when type is null")
        void toResponseDto_accountTypeNull_descriptionIsUnknown() {
            Account entity = buildFullAccount();
            entity.setAccountType(null);

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAccountTypeDescription()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("availableCredit computed: creditLimit - currBal (COBOL COMPUTE)")
        void toResponseDto_availableCredit_computedCorrectly() {
            Account entity = buildFullAccount();
            entity.setCurrBal(new BigDecimal("1500.00"));
            entity.setCreditLimit(new BigDecimal("5000.00"));

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit())
                    .as("COBOL COMPUTE AVAIL-CREDIT = CREDIT-LIMIT - CURR-BAL")
                    .isEqualByComparingTo("3500.00");
        }

        @Test
        @DisplayName("availableCredit is negative when balance exceeds limit (over-limit)")
        void toResponseDto_availableCredit_negativeWhenOverLimit() {
            Account entity = buildFullAccount();
            entity.setCurrBal(new BigDecimal("7500.00"));
            entity.setCreditLimit(new BigDecimal("5000.00"));

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit())
                    .as("COBOL COMPUTE AVAIL-CREDIT yields negative when CURR-BAL > CREDIT-LIMIT")
                    .isEqualByComparingTo("-2500.00");
        }

        @Test
        @DisplayName("availableCredit is zero when balance equals credit limit")
        void toResponseDto_availableCredit_zeroWhenBalanceEqualsLimit() {
            Account entity = buildFullAccount();
            entity.setCurrBal(new BigDecimal("5000.00"));
            entity.setCreditLimit(new BigDecimal("5000.00"));

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("availableCredit is zero when currBal is null")
        void toResponseDto_availableCredit_zeroWhenCurrBalNull() {
            Account entity = buildFullAccount();
            entity.setCurrBal(null);
            entity.setCreditLimit(new BigDecimal("5000.00"));

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit())
                    .as("COBOL: IF CURR-BAL = SPACES MOVE ZERO TO AVAIL-CREDIT")
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("availableCredit is zero when creditLimit is null")
        void toResponseDto_availableCredit_zeroWhenCreditLimitNull() {
            Account entity = buildFullAccount();
            entity.setCurrBal(new BigDecimal("1500.00"));
            entity.setCreditLimit(null);

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit())
                    .as("COBOL: IF CREDIT-LIMIT = SPACES MOVE ZERO TO AVAIL-CREDIT")
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("availableCredit is zero when both currBal and creditLimit are null")
        void toResponseDto_availableCredit_zeroWhenBothNull() {
            Account entity = buildFullAccount();
            entity.setCurrBal(null);
            entity.setCreditLimit(null);

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("availableCredit is full creditLimit when currBal is zero")
        void toResponseDto_availableCredit_fullCreditWhenBalZero() {
            Account entity = buildFullAccount();
            entity.setCurrBal(BigDecimal.ZERO);
            entity.setCreditLimit(new BigDecimal("5000.00"));

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAvailableCredit()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("Null entity fields produce null DTO fields (COBOL SPACES equivalent)")
        void toResponseDto_nullEntityFields_produceNullDtoFields() {
            Account entity = Account.builder()
                    .id(1L)
                    .accountId("00001001001")
                    .accountType("1")
                    .activeStatus(AccountStatus.ACTIVE)
                    .version(0L)
                    .build();

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getAccountName()).isNull();
            assertThat(dto.getCurrBal()).isNull();
            assertThat(dto.getCreditLimit()).isNull();
            assertThat(dto.getOpenDate()).isNull();
            assertThat(dto.getExpiryDate()).isNull();
            assertThat(dto.getAddrLine1()).isNull();
            assertThat(dto.getAddrState()).isNull();
            assertThat(dto.getPhoneNumber1()).isNull();
            assertThat(dto.getCreatedBy()).isNull();
            assertThat(dto.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("overLimitInd 'Y' is preserved in response")
        void toResponseDto_overLimitIndY_preservedInResponse() {
            Account entity = buildFullAccount();
            entity.setOverLimitInd("Y");

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getOverLimitInd()).isEqualTo("Y");
        }

        @Test
        @DisplayName("INACTIVE status maps to response DTO correctly")
        void toResponseDto_inactiveStatus_mappedCorrectly() {
            Account entity = buildFullAccount();
            entity.setActiveStatus(AccountStatus.INACTIVE);

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getActiveStatus()).isEqualTo(AccountStatus.INACTIVE);
        }

        @Test
        @DisplayName("currCycleCredit and currCycleDebit are mapped to response")
        void toResponseDto_cycleFields_mappedCorrectly() {
            Account entity = buildFullAccount();
            entity.setCurrCycleCredit(new BigDecimal("300.00"));
            entity.setCurrCycleDebit(new BigDecimal("150.00"));

            AccountResponseDto dto = mapper.toResponseDto(entity);

            assertThat(dto.getCurrCycleCredit()).isEqualByComparingTo("300.00");
            assertThat(dto.getCurrCycleDebit()).isEqualByComparingTo("150.00");
        }
    }

    // =========================================================================
    // updateEntityFromDto() - AccountUpdateDto -> Account (partial update)
    // COBOL: MOVE modified screen fields TO WS-ACCOUNT-MASTER-RECORD
    //        Only fields that changed on screen are moved (COBOL modified flag logic)
    // =========================================================================

    @Nested
    @DisplayName("updateEntityFromDto() - COBOL modified screen field MOVE logic")
    class UpdateEntityFromDtoTests {

        @Test
        @DisplayName("Only non-null DTO fields are applied to entity (null = unchanged)")
        void updateEntityFromDto_onlyNonNullFields_applied() {
            Account entity = buildFullAccount();

            // Save original values to verify they are NOT overwritten
            String originalName      = entity.getAccountName();
            BigDecimal originalLimit = entity.getCreditLimit();
            LocalDate originalOpen   = entity.getOpenDate();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .addrLine1("456 New Street")    // changed
                    .phoneNumber1("+19999999999")   // changed
                    // All other fields null -> must NOT be overwritten
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            // Changed fields reflected
            assertThat(entity.getAddrLine1()).isEqualTo("456 New Street");
            assertThat(entity.getPhoneNumber1()).isEqualTo("+19999999999");

            // Unchanged fields preserved exactly as before
            assertThat(entity.getAccountName()).isEqualTo(originalName);
            assertThat(entity.getCreditLimit()).isEqualByComparingTo(originalLimit);
            assertThat(entity.getOpenDate()).isEqualTo(originalOpen);
        }

        @Test
        @DisplayName("Immutable identity fields are never overwritten by update")
        void updateEntityFromDto_immutableFields_neverOverwritten() {
            Account entity = buildFullAccount();

            Long          originalId        = entity.getId();
            String        originalAccountId = entity.getAccountId();
            LocalDate     originalOpenDate  = entity.getOpenDate();
            LocalDateTime originalCreatedAt = entity.getCreatedAt();
            String        originalCreatedBy = entity.getCreatedBy();
            Long          originalVersion   = entity.getVersion();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .accountName("NEW NAME")
                    .creditLimit(new BigDecimal("9000.00"))
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            // These must NEVER be changed by an update DTO
            assertThat(entity.getId())
                    .as("Surrogate PK must never change")
                    .isEqualTo(originalId);
            assertThat(entity.getAccountId())
                    .as("COBOL: ACCT-ID is key field - never updated")
                    .isEqualTo(originalAccountId);
            assertThat(entity.getOpenDate())
                    .as("COBOL: ACCT-OPEN-DATE is set once on CREATE")
                    .isEqualTo(originalOpenDate);
            assertThat(entity.getCreatedAt())
                    .as("COBOL: created timestamp never updated")
                    .isEqualTo(originalCreatedAt);
            assertThat(entity.getCreatedBy())
                    .as("COBOL: created-by never updated")
                    .isEqualTo(originalCreatedBy);
            assertThat(entity.getVersion())
                    .as("Optimistic lock version managed by JPA, not mapper")
                    .isEqualTo(originalVersion);
        }

        @Test
        @DisplayName("Balance fields are never overwritten by update DTO")
        void updateEntityFromDto_balanceFields_neverOverwritten() {
            Account entity = buildFullAccount();
            BigDecimal originalBal         = entity.getCurrBal();
            BigDecimal originalCycleCredit = entity.getCurrCycleCredit();
            BigDecimal originalCycleDebit  = entity.getCurrCycleDebit();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .creditLimit(new BigDecimal("7000.00"))
                    .accountName("CHANGED NAME")
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getCurrBal())
                    .as("COBOL: ACCT-CURR-BAL updated only by transaction processing")
                    .isEqualByComparingTo(originalBal);
            assertThat(entity.getCurrCycleCredit())
                    .as("COBOL: cycle credit updated only by batch processing")
                    .isEqualByComparingTo(originalCycleCredit);
            assertThat(entity.getCurrCycleDebit())
                    .as("COBOL: cycle debit updated only by batch processing")
                    .isEqualByComparingTo(originalCycleDebit);
        }

        @Test
        @DisplayName("Full update DTO applies all updatable fields")
        void updateEntityFromDto_allUpdatableFields_applied() {
            Account entity = buildFullAccount();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .accountName("FULL UPDATE NAME")
                    .accountType("2")
                    .activeStatus(AccountStatus.INACTIVE)
                    .creditLimit(new BigDecimal("8000.00"))
                    .cashCreditLimit(new BigDecimal("3500.00"))
                    .expiryDate(LocalDate.now().plusYears(4))
                    .reissueDate(LocalDate.now().plusYears(3))
                    .addrLine1("789 Full Update Blvd")
                    .addrLine2("Suite 500")
                    .addrZip("20001")
                    .addrState("DC")
                    .addrCountry("USA")
                    .phoneNumber1("+12025551234")
                    .phoneNumber2("+12025555678")
                    .groupId("GRP999")
                    .studentInd("Y")
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getAccountName()).isEqualTo("FULL UPDATE NAME");
            assertThat(entity.getAccountType()).isEqualTo("2");
            assertThat(entity.getActiveStatus()).isEqualTo(AccountStatus.INACTIVE);
            assertThat(entity.getCreditLimit()).isEqualByComparingTo("8000.00");
            assertThat(entity.getCashCreditLimit()).isEqualByComparingTo("3500.00");
            assertThat(entity.getExpiryDate()).isEqualTo(LocalDate.now().plusYears(4));
            assertThat(entity.getReissueDate()).isEqualTo(LocalDate.now().plusYears(3));
            assertThat(entity.getAddrLine1()).isEqualTo("789 Full Update Blvd");
            assertThat(entity.getAddrLine2()).isEqualTo("Suite 500");
            assertThat(entity.getAddrZip()).isEqualTo("20001");
            assertThat(entity.getAddrState()).isEqualTo("DC");
            assertThat(entity.getAddrCountry()).isEqualTo("USA");
            assertThat(entity.getPhoneNumber1()).isEqualTo("+12025551234");
            assertThat(entity.getPhoneNumber2()).isEqualTo("+12025555678");
            assertThat(entity.getGroupId()).isEqualTo("GRP999");
            assertThat(entity.getStudentInd()).isEqualTo("Y");
        }

        @Test
        @DisplayName("Status change ACTIVE -> INACTIVE applied correctly")
        void updateEntityFromDto_statusChange_activeToInactive() {
            Account entity = buildFullAccount();
            assertThat(entity.getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .activeStatus(AccountStatus.INACTIVE)
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getActiveStatus())
                    .as("COBOL: MOVE 'N' TO ACCT-ACTIVE-STATUS")
                    .isEqualTo(AccountStatus.INACTIVE);
        }

        @Test
        @DisplayName("Status change INACTIVE -> ACTIVE applied correctly")
        void updateEntityFromDto_statusChange_inactiveToActive() {
            Account entity = buildFullAccount();
            entity.setActiveStatus(AccountStatus.INACTIVE);

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .activeStatus(AccountStatus.ACTIVE)
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getActiveStatus())
                    .as("COBOL: MOVE 'Y' TO ACCT-ACTIVE-STATUS")
                    .isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Empty update DTO leaves entity completely unchanged")
        void updateEntityFromDto_emptyDto_entityUnchanged() {
            Account entity        = buildFullAccount();
            Account entitySnapshot = buildFullAccount(); // reference snapshot

            AccountUpdateDto emptyDto = AccountUpdateDto.builder().build();

            mapper.updateEntityFromDto(emptyDto, entity);

            // All fields must still match the original snapshot
            assertThat(entity.getAccountName()).isEqualTo(entitySnapshot.getAccountName());
            assertThat(entity.getAccountType()).isEqualTo(entitySnapshot.getAccountType());
            assertThat(entity.getActiveStatus()).isEqualTo(entitySnapshot.getActiveStatus());
            assertThat(entity.getCreditLimit()).isEqualByComparingTo(entitySnapshot.getCreditLimit());
            assertThat(entity.getCashCreditLimit()).isEqualByComparingTo(entitySnapshot.getCashCreditLimit());
            assertThat(entity.getExpiryDate()).isEqualTo(entitySnapshot.getExpiryDate());
            assertThat(entity.getAddrLine1()).isEqualTo(entitySnapshot.getAddrLine1());
            assertThat(entity.getAddrLine2()).isEqualTo(entitySnapshot.getAddrLine2());
            assertThat(entity.getAddrZip()).isEqualTo(entitySnapshot.getAddrZip());
            assertThat(entity.getAddrState()).isEqualTo(entitySnapshot.getAddrState());
            assertThat(entity.getPhoneNumber1()).isEqualTo(entitySnapshot.getPhoneNumber1());
            assertThat(entity.getGroupId()).isEqualTo(entitySnapshot.getGroupId());
            assertThat(entity.getStudentInd()).isEqualTo(entitySnapshot.getStudentInd());
        }

        @Test
        @DisplayName("Address-only update leaves non-address fields unchanged")
        void updateEntityFromDto_addressOnlyUpdate_nonAddressFieldsPreserved() {
            Account entity = buildFullAccount();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .addrLine1("NEW ADDRESS LINE 1")
                    .addrLine2("NEW LINE 2")
                    .addrZip("99999")
                    .addrState("WA")
                    .addrCountry("USA")
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            // Address changed
            assertThat(entity.getAddrLine1()).isEqualTo("NEW ADDRESS LINE 1");
            assertThat(entity.getAddrLine2()).isEqualTo("NEW LINE 2");
            assertThat(entity.getAddrZip()).isEqualTo("99999");
            assertThat(entity.getAddrState()).isEqualTo("WA");

            // Financial fields untouched
            assertThat(entity.getAccountName()).isEqualTo("JOHN DOE");
            assertThat(entity.getCreditLimit()).isEqualByComparingTo("5000.00");
            assertThat(entity.getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Phone-only update leaves all other fields unchanged")
        void updateEntityFromDto_phoneOnlyUpdate_otherFieldsPreserved() {
            Account entity = buildFullAccount();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .phoneNumber1("+18005551234")
                    .phoneNumber2("+18005555678")
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getPhoneNumber1()).isEqualTo("+18005551234");
            assertThat(entity.getPhoneNumber2()).isEqualTo("+18005555678");
            assertThat(entity.getAddrLine1()).isEqualTo("123 Main St");
            assertThat(entity.getAccountName()).isEqualTo("JOHN DOE");
            assertThat(entity.getCreditLimit()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("Credit limit update preserves cash credit limit if not in DTO")
        void updateEntityFromDto_creditLimitOnly_cashCreditLimitPreserved() {
            Account entity = buildFullAccount();
            BigDecimal originalCashLimit = entity.getCashCreditLimit();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .creditLimit(new BigDecimal("10000.00"))
                    // cashCreditLimit intentionally omitted
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getCreditLimit()).isEqualByComparingTo("10000.00");
            assertThat(entity.getCashCreditLimit())
                    .as("Cash credit limit must not change if not in update DTO")
                    .isEqualByComparingTo(originalCashLimit);
        }

        @Test
        @DisplayName("StudentInd update from 'N' to 'Y' applied correctly")
        void updateEntityFromDto_studentIndChange_NtoY() {
            Account entity = buildFullAccount();
            assertThat(entity.getStudentInd()).isEqualTo("N");

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .studentInd("Y")
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getStudentInd())
                    .as("COBOL: MOVE 'Y' TO ACCT-STUDENT-IND")
                    .isEqualTo("Y");
        }

        @Test
        @DisplayName("GroupId update applied and other fields unchanged")
        void updateEntityFromDto_groupIdUpdate_otherFieldsUnchanged() {
            Account entity = buildFullAccount();

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .groupId("GRP-NEW")
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getGroupId()).isEqualTo("GRP-NEW");
            assertThat(entity.getAccountName()).isEqualTo("JOHN DOE");
            assertThat(entity.getActiveStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Expiry and reissue date update applied correctly")
        void updateEntityFromDto_dateUpdate_appliedCorrectly() {
            Account entity = buildFullAccount();

            LocalDate newExpiry  = LocalDate.now().plusYears(5);
            LocalDate newReissue = LocalDate.now().plusYears(4);

            AccountUpdateDto updateDto = AccountUpdateDto.builder()
                    .expiryDate(newExpiry)
                    .reissueDate(newReissue)
                    .build();

            mapper.updateEntityFromDto(updateDto, entity);

            assertThat(entity.getExpiryDate())
                    .as("COBOL: MOVE WS-NEW-EXPIRY-DATE TO ACCT-EXPIRY-DATE")
                    .isEqualTo(newExpiry);
            assertThat(entity.getReissueDate())
                    .as("COBOL: MOVE WS-NEW-REISSUE-DATE TO ACCT-REISSUE-DATE")
                    .isEqualTo(newReissue);
        }
    }

    // =========================================================================
    // resolveAccountTypeDesc() - COBOL EVALUATE ACCT-TYPE-CD
    // =========================================================================

    @Nested
    @DisplayName("resolveAccountTypeDesc() - COBOL EVALUATE ACCT-TYPE-CD branches")
    class ResolveAccountTypeDescTests {

        @Test
        @DisplayName("Type '1' resolves to 'Credit' (COBOL WHEN '1')")
        void resolveAccountTypeDesc_type1_returnsCredit() {
            Account entity = buildFullAccount();
            entity.setAccountType("1");
            assertThat(mapper.toResponseDto(entity).getAccountTypeDescription())
                    .isEqualTo("Credit");
        }

        @Test
        @DisplayName("Type '2' resolves to 'Debit' (COBOL WHEN '2')")
        void resolveAccountTypeDesc_type2_returnsDebit() {
            Account entity = buildFullAccount();
            entity.setAccountType("2");
            assertThat(mapper.toResponseDto(entity).getAccountTypeDescription())
                    .isEqualTo("Debit");
        }

        @ParameterizedTest(name = "Unknown type code [{0}] -> 'Unknown' (COBOL WHEN OTHER)")
        @ValueSource(strings = {"0", "3", "4", "9", "X", " ", "?"})
        @DisplayName("Any other type code resolves to 'Unknown' (COBOL WHEN OTHER)")
        void resolveAccountTypeDesc_unknownCode_returnsUnknown(String unknownCode) {
            Account entity = buildFullAccount();
            entity.setAccountType(unknownCode);
            assertThat(mapper.toResponseDto(entity).getAccountTypeDescription())
                    .isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Null type code resolves to 'UNKNOWN' (COBOL null guard)")
        void resolveAccountTypeDesc_nullCode_returnsUnknown() {
            Account entity = buildFullAccount();
            entity.setAccountType(null);
            assertThat(mapper.toResponseDto(entity).getAccountTypeDescription())
                    .isEqualTo("UNKNOWN");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a fully-populated AccountRequestDto.
     * Mirrors COBOL WS-ACCOUNT-MASTER-RECORD with all fields populated.
     */
    private AccountRequestDto buildFullRequest() {
        return AccountRequestDto.builder()
                .accountId("00001001001")
                .accountName("JOHN DOE")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .reissueDate(LocalDate.now().plusYears(1))
                .addrLine1("123 Main St")
                .addrLine2("Apt 4B")
                .addrState("NY")
                .addrCountry("USA")
                .addrZip("10001")
                .phoneNumber1("+12125551234")
                .phoneNumber2("+12125555678")
                .groupId("GRP001")
                .studentInd("N")
                .build();
    }

    /**
     * Builds a minimal AccountRequestDto with only mandatory fields.
     * Mirrors COBOL commarea with optional fields left as SPACES/ZEROES.
     */
    private AccountRequestDto buildMinimalRequest() {
        return AccountRequestDto.builder()
                .accountId("00001001001")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .build();
    }

    /**
     * Builds a fully-populated Account entity.
     * Mirrors COBOL WS-ACCOUNT-MASTER-RECORD after READ ACCTDAT.
     */
    private Account buildFullAccount() {
        return Account.builder()
                .id(1L)
                .accountId("00001001001")
                .accountName("JOHN DOE")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .currCycleCredit(new BigDecimal("200.00"))
                .currCycleDebit(new BigDecimal("100.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .reissueDate(LocalDate.now().plusYears(1))
                .addrLine1("123 Main St")
                .addrLine2("Apt 4B")
                .addrState("NY")
                .addrCountry("USA")
                .addrZip("10001")
                .phoneNumber1("+12125551234")
                .phoneNumber2("+12125555678")
                .groupId("GRP001")
                .studentInd("N")
                .overLimitInd("N")
                .createdBy("SYSTEM")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedBy("admin")
                .updatedAt(LocalDateTime.now())
                .version(2L)
                .build();
    }
}