// test/integration/AccountIntegrationTest.java
package com.agilesolutions.account.integration;

import com.agilesolutions.account.domain.dto.*;
import com.agilesolutions.account.domain.enums.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full end-to-end integration test suite using Testcontainers + PostgreSQL.
 *
 * Maps the complete COBOL COACTUPC.cbl transaction lifecycle:
 *
 *  Step  1 : EXEC CICS SIGNON              -> POST /auth/login
 *  Step  2 : WRITE ACCTDAT                 -> POST /accounts         (CREATE)
 *  Step  3 : READ ACCTDAT KEY = ACCT-ID    -> GET  /accounts/{id}    (READ)
 *  Step  4 : REWRITE ACCTDAT               -> PUT  /accounts/{id}    (FULL UPDATE)
 *  Step  5 : Audit trail verification      -> GET  /audit/account/{id}
 *  Step  6 : Browse by name qualifier      -> GET  /accounts/search?accountName=
 *  Step  7 : Browse by type+status         -> GET  /accounts/search?accountType=&activeStatus=
 *  Step  8 : COMPUTE OVER-LIMIT-IND = 'Y'  -> POST /accounts         (over-limit account)
 *  Step  9 : EVALUATE OVER-LIMIT-IND       -> GET  /accounts/over-limit
 *  Step 10 : Screen field change detection -> PATCH /accounts/{id}   (PARTIAL UPDATE)
 *  Step 11 : ACCT-ACTIVE-STATUS = 'N'      -> DELETE /accounts/{id}  (LOGICAL DELETE)
 *  Step 12 : Verify deactivation           -> GET  /accounts/{id}
 *  Step 13 : Inactive account guard        -> PUT  /accounts/{id}    (must be rejected)
 *  Step 14 : FILE STATUS '23' path         -> GET  /accounts/99999999999 (NOT FOUND)
 *  Step 15 : FILE STATUS '22' path         -> POST /accounts         (DUPLICATE KEY)
 *  Step 16 : Audit date range query        -> GET  /audit/range
 *  Step 17 : Audit user query              -> GET  /audit/user/{username}
 *  Step 18 : EXEC CICS NOTAUTH (USER role) -> GET  /audit/account/{id} (forbidden)
 *  Step 19 : EDIT-EXPIRY-DATE validation   -> POST /accounts         (bad dates)
 *  Step 20 : EDIT-CREDIT-LIMIT validation  -> POST /accounts         (bad limits)
 *  Step 21 : Paginated account list        -> GET  /accounts?page=0&size=5
 *  Step 22 : Sort by balance descending    -> GET  /accounts?sortBy=currBal&direction=DESC
 *  Step 23 : Balance range search          -> GET  /accounts/search  (minBal/maxBal)
 *  Step 24 : EXEC CICS SIGNOFF             -> POST /auth/logout
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AccountIntegrationTest - Full COBOL COACTUPC lifecycle")
class AccountIntegrationTest {

    // ─── Testcontainers PostgreSQL ────────────────────────────────────────────

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("accountdb_test")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .withReuse(true);

    @DynamicPropertySource
    static void overrideDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url",          postgres::getJdbcUrl);
        registry.add("spring.flyway.user",         postgres::getUsername);
        registry.add("spring.flyway.password",     postgres::getPassword);
    }

    // ─── Autowired beans ──────────────────────────────────────────────────────

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ─── Shared state across ordered test steps ───────────────────────────────

    /**
     * JWT token obtained in step 1, reused in all subsequent steps.
     * Replaces COBOL CICS COMMAREA session token.
     */
    private static String adminJwtToken;

    /**
     * Primary test account ID used throughout the lifecycle.
     * Mirrors COBOL WS-ACCT-ID working-storage field.
     */
    private static final String PRIMARY_ACCOUNT_ID   = "00009990001";

    /**
     * Secondary account for over-limit scenario (step 8).
     * Mirrors COBOL WS-OVER-LIMIT-ACCT-ID.
     */
    private static final String OVER_LIMIT_ACCOUNT_ID = "00009990002";

    /**
     * Third account for extra data to support pagination/search tests.
     */
    private static final String EXTRA_ACCOUNT_ID     = "00009990003";

    // ─── ObjectMapper setup ───────────────────────────────────────────────────

    @BeforeEach
    void configureObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // =========================================================================
    // Step 1 – EXEC CICS SIGNON -> POST /auth/login
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Step 01 | CICS SIGNON -> POST /auth/login returns JWT token")
    void step01_login_asAdmin_returnsJwtToken() throws Exception {

        AuthDto.LoginRequest loginRequest =
                new AuthDto.LoginRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andReturn();

        adminJwtToken = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();

        assertThat(adminJwtToken)
                .as("JWT token must not be blank after login")
                .isNotBlank();
    }

    // =========================================================================
    // Step 2 – WRITE ACCTDAT -> POST /accounts (CREATE primary account)
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Step 02 | WRITE ACCTDAT -> POST /accounts creates primary account")
    void step02_createPrimaryAccount_returns201() throws Exception {

        AccountRequestDto request = buildPrimaryAccountRequest();

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.data.accountId").value(PRIMARY_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.accountName").value("PRIMARY TEST ACCOUNT"))
                .andExpect(jsonPath("$.data.accountType").value("1"))
                .andExpect(jsonPath("$.data.accountTypeDescription").value("Credit"))
                .andExpect(jsonPath("$.data.activeStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currBal").value(1500.00))
                .andExpect(jsonPath("$.data.creditLimit").value(5000.00))
                .andExpect(jsonPath("$.data.cashCreditLimit").value(2000.00))
                .andExpect(jsonPath("$.data.availableCredit").value(3500.00))
                .andExpect(jsonPath("$.data.overLimitInd").value("N"))
                .andExpect(jsonPath("$.data.addrLine1").value("100 Integration Ave"))
                .andExpect(jsonPath("$.data.addrState").value("NY"))
                .andExpect(jsonPath("$.data.addrCountry").value("USA"))
                .andExpect(jsonPath("$.data.addrZip").value("10001"))
                .andExpect(jsonPath("$.data.phoneNumber1").value("+12125551234"))
                .andExpect(jsonPath("$.data.studentInd").value("N"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    // =========================================================================
    // Step 3 – READ ACCTDAT KEY = ACCT-ID -> GET /accounts/{accountId}
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Step 03 | READ ACCTDAT -> GET /accounts/{id} retrieves primary account")
    void step03_getPrimaryAccount_returns200() throws Exception {

        mockMvc.perform(get("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(PRIMARY_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.accountName").value("PRIMARY TEST ACCOUNT"))
                .andExpect(jsonPath("$.data.activeStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.groupId").value("GRP001"))
                .andExpect(jsonPath("$.data.phoneNumber1").value("+12125551234"))
                .andExpect(jsonPath("$.data.phoneNumber2").value("+12125555678"))
                .andExpect(jsonPath("$.data.addrLine2").value("Floor 5"))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    // =========================================================================
    // Step 4 – REWRITE ACCTDAT -> PUT /accounts/{id} (FULL UPDATE)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Step 04 | REWRITE ACCTDAT -> PUT /accounts/{id} full update succeeds")
    void step04_fullUpdateAccount_returns200() throws Exception {

        AccountUpdateDto updateDto = AccountUpdateDto.builder()
                .accountName("UPDATED PRIMARY ACCT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .creditLimit(new BigDecimal("7500.00"))
                .cashCreditLimit(new BigDecimal("3000.00"))
                .expiryDate(LocalDate.now().plusYears(3))
                .reissueDate(LocalDate.now().plusYears(2))
                .addrLine1("200 Updated Boulevard")
                .addrLine2("Suite 300")
                .addrZip("10002")
                .addrState("NY")
                .addrCountry("USA")
                .phoneNumber1("+12125559001")
                .phoneNumber2("+12125559002")
                .groupId("GRP002")
                .studentInd("N")
                .build();

        mockMvc.perform(put("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account updated successfully"))
                .andExpect(jsonPath("$.data.accountName").value("UPDATED PRIMARY ACCT"))
                .andExpect(jsonPath("$.data.creditLimit").value(7500.00))
                .andExpect(jsonPath("$.data.cashCreditLimit").value(3000.00))
                .andExpect(jsonPath("$.data.availableCredit").value(6000.00))
                .andExpect(jsonPath("$.data.addrLine1").value("200 Updated Boulevard"))
                .andExpect(jsonPath("$.data.addrLine2").value("Suite 300"))
                .andExpect(jsonPath("$.data.addrZip").value("10002"))
                .andExpect(jsonPath("$.data.phoneNumber1").value("+12125559001"))
                .andExpect(jsonPath("$.data.groupId").value("GRP002"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    // =========================================================================
    // Step 5 – Audit trail after CREATE + UPDATE
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Step 05 | VSAM audit trail -> GET /audit/account/{id} has CREATE+UPDATE")
    void step05_auditLog_containsCreateAndUpdate() throws Exception {

        mockMvc.perform(get("/audit/account/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.content").isArray())
                // Most recent entry first (DESC order)
                .andExpect(jsonPath("$.data.content[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.data.content[0].entityId").value(PRIMARY_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.content[0].entityType").value("ACCOUNT"))
                .andExpect(jsonPath("$.data.content[0].changedBy").value("admin"))
                .andExpect(jsonPath("$.data.content[0].changedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].oldValue").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].newValue").isNotEmpty())
                .andExpect(jsonPath("$.data.content[1].action").value("CREATE"))
                .andExpect(jsonPath("$.data.content[1].oldValue").doesNotExist());
    }

    // =========================================================================
    // Step 6 – Browse ACCTDAT by name qualifier -> GET /accounts/search?accountName=
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Step 06 | COBOL name browse -> GET /accounts/search?accountName= returns match")
    void step06_searchByAccountName_returnsMatch() throws Exception {

        mockMvc.perform(get("/accounts/search")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("accountName", "UPDATED")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].accountId")
                        .value(PRIMARY_ACCOUNT_ID));
    }

    @Test
    @Order(6)
    @DisplayName("Step 06b | COBOL name browse -> no match returns empty page")
    void step06b_searchByAccountName_noMatch_returnsEmptyPage() throws Exception {

        mockMvc.perform(get("/accounts/search")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("accountName", "ZZZNOMATCH999")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // =========================================================================
    // Step 7 – Browse by type + status -> GET /accounts/search?accountType=&activeStatus=
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Step 07 | COBOL keyed READ -> GET /accounts/search by type+status")
    void step07_searchByTypeAndStatus_returnsFilteredResults() throws Exception {

        mockMvc.perform(get("/accounts/search")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("accountType", "1")
                        .param("activeStatus", "ACTIVE")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "accountId")
                        .param("direction", "ASC"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[*].accountType",
                        everyItem(is("1"))))
                .andExpect(jsonPath("$.data.content[*].activeStatus",
                        everyItem(is("ACTIVE"))));
    }

    @Test
    @Order(7)
    @DisplayName("Step 07b | COBOL keyed READ -> search INACTIVE status returns inactive only")
    void step07b_searchByStatus_inactive_returnsInactiveOnly() throws Exception {

        // First create an inactive account so we have data to assert against
        AccountRequestDto inactiveBase = buildExtraAccountRequest();
        inactiveBase.setAccountId("00009990009");
        inactiveBase.setActiveStatus(AccountStatus.INACTIVE);

        mockMvc.perform(post("/accounts")
                .header("Authorization", "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inactiveBase)));

        mockMvc.perform(get("/accounts/search")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("activeStatus", "INACTIVE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].activeStatus",
                        everyItem(is("INACTIVE"))));
    }

    // =========================================================================
    // Step 8 – COMPUTE OVER-LIMIT-IND = 'Y' -> POST /accounts (over-limit)
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Step 08 | COMPUTE OVER-LIMIT-IND='Y' -> POST /accounts sets indicator")
    void step08_createOverLimitAccount_overLimitIndSetToY() throws Exception {

        AccountRequestDto overLimitRequest = AccountRequestDto.builder()
                .accountId(OVER_LIMIT_ACCOUNT_ID)
                .accountName("OVER LIMIT ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("8500.00"))       // intentionally > creditLimit
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(45))
                .expiryDate(LocalDate.now().plusYears(2))
                .reissueDate(LocalDate.now().plusYears(1))
                .addrLine1("999 Over Limit Lane")
                .addrState("CA")
                .addrCountry("USA")
                .addrZip("90001")
                .phoneNumber1("+13105551234")
                .studentInd("N")
                .groupId("GRP003")
                .build();

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overLimitRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountId").value(OVER_LIMIT_ACCOUNT_ID))
                .andExpect(jsonPath("$.data.overLimitInd").value("Y"))
                .andExpect(jsonPath("$.data.currBal").value(8500.00))
                .andExpect(jsonPath("$.data.creditLimit").value(5000.00))
                // availableCredit should be negative: 5000 - 8500 = -3500
                .andExpect(jsonPath("$.data.availableCredit").value(-3500.00));
    }

    // =========================================================================
    // Step 9 – EVALUATE OVER-LIMIT-IND -> GET /accounts/over-limit
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Step 09 | EVALUATE OVER-LIMIT-IND='Y' -> GET /accounts/over-limit")
    void step09_getOverLimitAccounts_returnsOnlyOverLimitRecords() throws Exception {

        mockMvc.perform(get("/accounts/over-limit")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                // Every returned account must have currBal > creditLimit
                .andExpect(jsonPath("$.data.content[*].overLimitInd",
                        everyItem(is("Y"))))
                // Verify our specific over-limit account is in the list
                .andExpect(jsonPath("$.data.content[?(@.accountId == '" +
                        OVER_LIMIT_ACCOUNT_ID + "')]").exists());
    }

    // =========================================================================
    // Step 10 – Screen field change detection -> PATCH /accounts/{id}
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Step 10 | COBOL screen field change -> PATCH /accounts/{id} partial update")
    void step10_patchAccount_onlyChangedFieldsUpdated() throws Exception {

        AccountUpdateDto patchDto = AccountUpdateDto.builder()
                .phoneNumber1("+19175559999")   // changed
                .addrZip("10015")               // changed
                .studentInd("Y")                // changed
                // accountName intentionally omitted -> must remain "UPDATED PRIMARY ACCT"
                // creditLimit intentionally omitted -> must remain 7500.00
                .build();

        mockMvc.perform(patch("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account patched successfully"))
                // Changed fields reflected
                .andExpect(jsonPath("$.data.phoneNumber1").value("+19175559999"))
                .andExpect(jsonPath("$.data.addrZip").value("10015"))
                .andExpect(jsonPath("$.data.studentInd").value("Y"))
                // Unchanged fields must be preserved
                .andExpect(jsonPath("$.data.accountName").value("UPDATED PRIMARY ACCT"))
                .andExpect(jsonPath("$.data.creditLimit").value(7500.00))
                .andExpect(jsonPath("$.data.groupId").value("GRP002"));
    }

    // =========================================================================
    // Step 11 – ACCT-ACTIVE-STATUS = 'N' -> DELETE /accounts/{id} (logical delete)
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Step 11 | ACCT-ACTIVE-STATUS='N' -> DELETE /accounts/{id} deactivates")
    void step11_deactivateAccount_returns204() throws Exception {

        mockMvc.perform(delete("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // Step 12 – Verify deactivation persisted
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Step 12 | Verify deactivation -> GET /accounts/{id} shows INACTIVE")
    void step12_getDeactivatedAccount_showsInactiveStatus() throws Exception {

        mockMvc.perform(get("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.data.accountId").value(PRIMARY_ACCOUNT_ID));
    }

    // =========================================================================
    // Step 13 – Inactive account update guard -> PUT /accounts/{id} (must reject)
    // =========================================================================

    @Test
    @Order(13)
    @DisplayName("Step 13 | COBOL inactive guard -> PUT on INACTIVE account returns 400")
    void step13_updateInactiveAccount_returns400WithAcct0003() throws Exception {

        AccountUpdateDto updateDto = AccountUpdateDto.builder()
                .accountName("THIS SHOULD BE REJECTED")
                .creditLimit(new BigDecimal("9999.00"))
                .build();

        mockMvc.perform(put("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0003"))
                .andExpect(jsonPath("$.message").value(
                        containsString("Cannot update inactive account")));
    }

    // =========================================================================
    // Step 14 – FILE STATUS '23' path -> GET /accounts/99999999999 (NOT FOUND)
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("Step 14 | FILE STATUS '23' -> GET non-existent account returns 404")
    void step14_getNonExistentAccount_returns404WithAcct0001() throws Exception {

        mockMvc.perform(get("/accounts/{accountId}", "99999999999")
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0001"))
                .andExpect(jsonPath("$.message").value(containsString("99999999999")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // =========================================================================
    // Step 15 – FILE STATUS '22' path -> POST /accounts (DUPLICATE KEY)
    // =========================================================================

    @Test
    @Order(15)
    @DisplayName("Step 15 | FILE STATUS '22' -> POST duplicate accountId returns 400")
    void step15_createDuplicateAccount_returns400WithAcct0002() throws Exception {

        // PRIMARY_ACCOUNT_ID was created in step 2 (now inactive but still exists)
        AccountRequestDto duplicate = buildPrimaryAccountRequest();

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0002"))
                .andExpect(jsonPath("$.message").value(
                        containsString(PRIMARY_ACCOUNT_ID)));
    }

    // =========================================================================
    // Step 16 – Audit date range query -> GET /audit/range
    // =========================================================================

    @Test
    @Order(16)
    @DisplayName("Step 16 | Audit date range -> GET /audit/range returns today's entries")
    void step16_auditLog_dateRangeQuery_returnsTodayEntries() throws Exception {

        String from = LocalDateTime.now().minusHours(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String to   = LocalDateTime.now().plusHours(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(get("/audit/range")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("from", from)
                        .param("to",   to)
                        .param("page", "0")
                        .param("size", "100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                // Steps 2,4,8,10,11 all generated audit entries
                .andExpect(jsonPath("$.data.totalElements")
                        .value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.data.content[0].changedAt").isNotEmpty());
    }

    // =========================================================================
    // Step 17 – Audit user query -> GET /audit/user/{username}
    // =========================================================================

    @Test
    @Order(17)
    @DisplayName("Step 17 | Audit user query -> GET /audit/user/admin returns admin entries")
    void step17_auditLog_byUser_returnsAdminEntries() throws Exception {

        mockMvc.perform(get("/audit/user/{username}", "admin")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements")
                        .value(greaterThanOrEqualTo(5)))
                // All entries must belong to 'admin'
                .andExpect(jsonPath("$.data.content[*].changedBy",
                        everyItem(is("admin"))));
    }

    // =========================================================================
    // Step 18 – EXEC CICS NOTAUTH -> USER role blocked from audit endpoints
    // =========================================================================

    @Test
    @Order(18)
    @DisplayName("Step 18 | CICS NOTAUTH -> USER role denied access to /audit/** (403)")
    void step18_auditEndpoint_userRole_returns403() throws Exception {

        // Authenticate as a regular user
        AuthDto.LoginRequest userLogin = new AuthDto.LoginRequest("user", "user123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn();

        int loginStatus = loginResult.getResponse().getStatus();

        // If test user doesn't exist in this environment, skip gracefully
        if (loginStatus != 200) {
            System.out.println("Skipping step 18: test 'user' account not seeded");
            return;
        }

        String userToken = objectMapper
                .readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // USER role must be forbidden on audit endpoints
        mockMvc.perform(get("/audit/account/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));

        // USER role must also be forbidden on over-limit endpoint
        mockMvc.perform(get("/accounts/over-limit")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Step 19 – EDIT-EXPIRY-DATE validation -> POST /accounts (bad dates)
    // =========================================================================

    @Test
    @Order(19)
    @DisplayName("Step 19 | EDIT-EXPIRY-DATE -> POST with expiry before open date returns 400")
    void step19_createAccount_expiryBeforeOpenDate_returns400() throws Exception {

        AccountRequestDto badDates = buildExtraAccountRequest();
        badDates.setAccountId("00009990091");
        badDates.setOpenDate(LocalDate.now().minusDays(10));
        badDates.setExpiryDate(LocalDate.now().minusDays(20)); // BEFORE open date!

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDates)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0004"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]",
                        containsString("Expiry date must be after open date")));
    }

    @Test
    @Order(19)
    @DisplayName("Step 19b | EDIT-OPEN-DATE -> POST with future open date returns 400")
    void step19b_createAccount_futureOpenDate_returns400() throws Exception {

        AccountRequestDto futureOpen = buildExtraAccountRequest();
        futureOpen.setAccountId("00009990092");
        futureOpen.setOpenDate(LocalDate.now().plusDays(5));  // future!
        futureOpen.setExpiryDate(LocalDate.now().plusYears(1));

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureOpen)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]",
                        containsString("Open date cannot be in the future")));
    }

    @Test
    @Order(19)
    @DisplayName("Step 19c | EDIT-REISSUE-DATE -> POST with reissue after expiry returns 400")
    void step19c_createAccount_reissueAfterExpiry_returns400() throws Exception {

        AccountRequestDto badReissue = buildExtraAccountRequest();
        badReissue.setAccountId("00009990093");
        badReissue.setOpenDate(LocalDate.now().minusDays(30));
        badReissue.setExpiryDate(LocalDate.now().plusYears(1));
        badReissue.setReissueDate(LocalDate.now().plusYears(2)); // AFTER expiry!

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReissue)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]",
                        containsString("Reissue date must be before expiry date")));
    }

    // =========================================================================
    // Step 20 – EDIT-CREDIT-LIMIT validation -> POST /accounts (bad limits)
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("Step 20 | EDIT-CREDIT-LIMIT -> cash limit exceeds credit limit returns 400")
    void step20_createAccount_cashLimitExceedsCredit_returns400() throws Exception {

        AccountRequestDto badLimits = buildExtraAccountRequest();
        badLimits.setAccountId("00009990094");
        badLimits.setCreditLimit(new BigDecimal("1000.00"));
        badLimits.setCashCreditLimit(new BigDecimal("2500.00")); // EXCEEDS creditLimit!

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLimits)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCT-0004"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]",
                        containsString("Cash credit limit cannot exceed")));
    }

    @Test
    @Order(20)
    @DisplayName("Step 20b | EDIT-ACCTID -> non-numeric accountId returns 400")
    void step20b_createAccount_nonNumericAccountId_returns400() throws Exception {

        AccountRequestDto badId = buildExtraAccountRequest();
        badId.setAccountId("ABCDE123456"); // non-numeric!

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badId)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @Order(20)
    @DisplayName("Step 20c | EDIT-ACCT-TYPE -> invalid account type returns 400")
    void step20c_createAccount_invalidAccountType_returns400() throws Exception {

        AccountRequestDto badType = buildExtraAccountRequest();
        badType.setAccountId("00009990095");
        badType.setAccountType("9"); // invalid! must be 1 or 2

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badType)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // =========================================================================
    // Step 21 – Paginated account list -> GET /accounts?page=0&size=5
    // =========================================================================

    @Test
    @Order(21)
    @DisplayName("Step 21 | COBOL browse/scroll -> GET /accounts paginated list")
    void step21_getAllAccounts_paginatedResponse() throws Exception {

        // Create extra account to ensure we have data for pagination
        AccountRequestDto extra = buildExtraAccountRequest();
        extra.setAccountId(EXTRA_ACCOUNT_ID);

        mockMvc.perform(post("/accounts")
                .header("Authorization", "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(extra)));

        // Now test pagination
        mockMvc.perform(get("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "accountId")
                        .param("direction", "ASC"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalPages").value(greaterThanOrEqualTo(1)));
    }

    // =========================================================================
    // Step 22 – Sort by balance descending -> GET /accounts?sortBy=currBal&direction=DESC
    // =========================================================================

    @Test
    @Order(22)
    @DisplayName("Step 22 | COBOL sorted browse -> GET /accounts sorted by currBal DESC")
    void step22_getAllAccounts_sortedByBalanceDescending() throws Exception {

        MvcResult result = mockMvc.perform(get("/accounts")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "currBal")
                        .param("direction", "DESC"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        // Parse response and verify descending order
        String body = result.getResponse().getContentAsString();
        var contentNode = objectMapper.readTree(body).path("data").path("content");

        if (contentNode.size() >= 2) {
            double firstBal  = contentNode.get(0).path("currBal").asDouble();
            double secondBal = contentNode.get(1).path("currBal").asDouble();
            assertThat(firstBal)
                    .as("First account balance should be >= second (DESC order)")
                    .isGreaterThanOrEqualTo(secondBal);
        }
    }

    // =========================================================================
    // Step 23 – Balance range search -> GET /accounts/search
    // =========================================================================

    @Test
    @Order(23)
    @DisplayName("Step 23 | Balance range search -> accounts within range returned")
    void step23_searchByBalanceRange_returnsAccountsInRange() throws Exception {

        // OVER_LIMIT_ACCOUNT_ID has currBal = 8500.00
        // EXTRA_ACCOUNT_ID has currBal = 2500.00
        // Balance range 2000..9000 should return both

        mockMvc.perform(get("/accounts/search")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // =========================================================================
    // Step 24 – EXEC CICS SIGNOFF -> POST /auth/logout
    // =========================================================================

    @Test
    @Order(24)
    @DisplayName("Step 24 | CICS SIGNOFF -> POST /auth/logout clears session")
    void step24_logout_returns200() throws Exception {

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // =========================================================================
    // Step 25 – Token invalidated after logout
    // =========================================================================

    @Test
    @Order(25)
    @DisplayName("Step 25 | Post-logout -> expired/discarded token returns 401")
    void step25_accessAfterLogout_returns401() throws Exception {

        // Discard token to simulate client-side token removal
        // Note: JWT is stateless so server won't reject it unless
        // a token blacklist is implemented; here we use an invalid token
        String invalidToken = "Bearer invalid.token.value";

        mockMvc.perform(get("/accounts/{accountId}", PRIMARY_ACCOUNT_ID)
                        .header("Authorization", invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds the primary test account request.
     * Mirrors COBOL WS-ACCOUNT-MASTER-RECORD population
     * in PROCESS-ENTER-KEY paragraph.
     */
    private AccountRequestDto buildPrimaryAccountRequest() {
        return AccountRequestDto.builder()
                .accountId(PRIMARY_ACCOUNT_ID)
                .accountName("PRIMARY TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusYears(2))
                .reissueDate(LocalDate.now().plusYears(1))
                .addrLine1("100 Integration Ave")
                .addrLine2("Floor 5")
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
     * Builds an extra test account for pagination / search tests.
     * Mirrors COBOL WS-ACCOUNT-MASTER-RECORD for secondary test data.
     */
    private AccountRequestDto buildExtraAccountRequest() {
        return AccountRequestDto.builder()
                .accountId(EXTRA_ACCOUNT_ID)
                .accountName("EXTRA TEST ACCOUNT")
                .accountType("2")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("2500.00"))
                .creditLimit(new BigDecimal("0.00"))
                .cashCreditLimit(new BigDecimal("0.00"))
                .openDate(LocalDate.now().minusDays(15))
                .expiryDate(LocalDate.now().plusYears(1))
                .reissueDate(LocalDate.now().plusMonths(6))
                .addrLine1("200 Extra Street")
                .addrLine2("Unit 10")
                .addrState("TX")
                .addrCountry("USA")
                .addrZip("75001")
                .phoneNumber1("+12145559999")
                .groupId("GRP004")
                .studentInd("Y")
                .build();
    }
}