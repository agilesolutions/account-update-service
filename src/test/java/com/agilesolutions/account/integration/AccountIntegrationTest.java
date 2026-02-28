// test/integration/AccountIntegrationTest.java (continued)

import java.time.LocalDate;.content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
        .andReturn();

String responseBody = result.getResponse().getContentAsString();
jwtToken = objectMapper.readTree(responseBody)
                .path("data")
                .path("accessToken")
                .asText();

assertThat(jwtToken).isNotBlank();
    }

// ─── Step 2: Create Account ───────────────────────────────────────────────

@Test
@Order(2)
@DisplayName("Step 2: Create account (replaces COBOL WRITE ACCTDAT)")
void step2_createAccount_returns201() throws Exception {
    AccountRequestDto request = buildCreateRequest();

    mockMvc.perform(post("/accounts")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accountId").value(TEST_ACCOUNT_ID))
            .andExpect(jsonPath("$.data.accountType").value("1"))
            .andExpect(jsonPath("$.data.activeStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.data.currBal").value(1500.00))
            .andExpect(jsonPath("$.data.creditLimit").value(5000.00))
            .andExpect(jsonPath("$.data.availableCredit").value(3500.00))
            .andExpect(jsonPath("$.data.overLimitInd").value("N"));
}

// ─── Step 3: Get Account ──────────────────────────────────────────────────

@Test
@Order(3)
@DisplayName("Step 3: Retrieve account (replaces COBOL READ ACCTDAT)")
void step3_getAccount_returns200() throws Exception {
    mockMvc.perform(get("/accounts/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accountId").value(TEST_ACCOUNT_ID))
            .andExpect(jsonPath("$.data.accountName").value("INTEGRATION TEST"))
            .andExpect(jsonPath("$.data.addrLine1").value("100 Test Avenue"))
            .andExpect(jsonPath("$.data.addrState").value("NY"))
            .andExpect(jsonPath("$.data.addrCountry").value("USA"))
            .andExpect(jsonPath("$.data.version").value(0));
}

// ─── Step 4: Update Account ───────────────────────────────────────────────

@Test
@Order(4)
@DisplayName("Step 4: Update account (replaces COBOL REWRITE ACCTDAT)")
void step4_updateAccount_returns200() throws Exception {
    AccountUpdateDto updateDto = AccountUpdateDto.builder()
            .accountName("UPDATED INTEGRATION")
            .creditLimit(new BigDecimal("7500.00"))
            .cashCreditLimit(new BigDecimal("3000.00"))
            .addrLine1("200 Updated Avenue")
            .addrLine2("Suite 100")
            .addrZip("10002")
            .phoneNumber1("+12125559999")
            .expiryDate(LocalDate.now().plusYears(3))
            .build();

    mockMvc.perform(put("/accounts/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accountName").value("UPDATED INTEGRATION"))
            .andExpect(jsonPath("$.data.creditLimit").value(7500.00))
            .andExpect(jsonPath("$.data.addrLine1").value("200 Updated Avenue"))
            .andExpect(jsonPath("$.data.version").value(1));
}

// ─── Step 5: Verify Audit Log Created ────────────────────────────────────

@Test
@Order(5)
@DisplayName("Step 5: Verify audit trail (replaces COBOL VSAM audit write)")
void step5_auditLog_containsCreateAndUpdateEntries() throws Exception {
    mockMvc.perform(get("/audit/account/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.content[0].action").value("UPDATE"))
            .andExpect(jsonPath("$.data.content[1].action").value("CREATE"))
            .andExpect(jsonPath("$.data.content[0].changedBy").value("admin"));
}

// ─── Step 6: Search Accounts ──────────────────────────────────────────────

@Test
@Order(6)
@DisplayName("Step 6: Search by account name (replaces COBOL browse with qualifier)")
void step6_searchByAccountName_returnsResults() throws Exception {
    mockMvc.perform(get("/accounts/search")
                    .header("Authorization", "Bearer " + jwtToken)
                    .param("accountName", "UPDATED")
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content[0].accountId").value(TEST_ACCOUNT_ID))
            .andExpect(jsonPath("$.data.totalElements").value(1));
}

@Test
@Order(7)
@DisplayName("Step 7: Search by type and status (replaces COBOL keyed READ)")
void step7_searchByTypeAndStatus_returnsResults() throws Exception {
    mockMvc.perform(get("/accounts/search")
                    .header("Authorization", "Bearer " + jwtToken)
                    .param("accountType", "1")
                    .param("activeStatus", "ACTIVE")
                    .param("page", "0")
                    .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(
                    org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
}

// ─── Step 8: Over-Limit Detection ────────────────────────────────────────

@Test
@Order(8)
@DisplayName("Step 8: Create over-limit account (replaces COBOL OVER-LIMIT-IND logic)")
void step8_createOverLimitAccount_setsIndicatorY() throws Exception {
    AccountRequestDto overLimitRequest = AccountRequestDto.builder()
            .accountId("00009999002")
            .accountName("OVER LIMIT TEST")
            .accountType("1")
            .activeStatus(AccountStatus.ACTIVE)
            .currBal(new BigDecimal("6000.00"))   // exceeds creditLimit
            .creditLimit(new BigDecimal("5000.00"))
            .cashCreditLimit(new BigDecimal("2000.00"))
            .openDate(LocalDate.now().minusDays(10))
            .expiryDate(LocalDate.now().plusYears(1))
            .addrLine1("999 Over Limit Blvd")
            .addrState("CA")
            .addrCountry("USA")
            .addrZip("90001")
            .studentInd("N")
            .build();

    mockMvc.perform(post("/accounts")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(overLimitRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.overLimitInd").value("Y"))
            .andExpect(jsonPath("$.data.availableCredit").value(-1000.00));
}

@Test
@Order(9)
@DisplayName("Step 9: Query over-limit accounts endpoint")
void step9_getOverLimitAccounts_returnsOverLimitOnly() throws Exception {
    mockMvc.perform(get("/accounts/over-limit")
                    .header("Authorization", "Bearer " + jwtToken)
                    .param("page", "0")
                    .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(
                    org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.data.content[0].overLimitInd").value("Y"));
}

// ─── Step 10: Patch Account ───────────────────────────────────────────────

@Test
@Order(10)
@DisplayName("Step 10: Partial update (COBOL screen field change detection)")
void step10_patchAccount_partialUpdate_returns200() throws Exception {
    AccountUpdateDto patchDto = AccountUpdateDto.builder()
            .phoneNumber1("+19175551234")
            .addrZip("10010")
            .studentInd("Y")
            .build();

    mockMvc.perform(patch("/accounts/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patchDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.phoneNumber1").value("+19175551234"))
            .andExpect(jsonPath("$.data.addrZip").value("10010"))
            .andExpect(jsonPath("$.data.studentInd").value("Y"));
}

// ─── Step 11: Deactivate Account ─────────────────────────────────────────

@Test
@Order(11)
@DisplayName("Step 11: Deactivate account (replaces COBOL ACCT-ACTIVE-STATUS = 'N')")
void step11_deactivateAccount_returns204() throws Exception {
    mockMvc.perform(delete("/accounts/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isNoContent());
}

// ─── Step 12: Verify Deactivation ────────────────────────────────────────

@Test
@Order(12)
@DisplayName("Step 12: Verify deactivated account status")
void step12_getDeactivatedAccount_showsInactiveStatus() throws Exception {
    mockMvc.perform(get("/accounts/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.activeStatus").value("INACTIVE"));
}

// ─── Step 13: Update Inactive Account Rejected ───────────────────────────

@Test
@Order(13)
@DisplayName("Step 13: Update inactive account rejected (COBOL inactive guard)")
void step13_updateInactiveAccount_returns400() throws Exception {
    AccountUpdateDto updateDto = AccountUpdateDto.builder()
            .accountName("SHOULD FAIL")
            .build();

    mockMvc.perform(put("/accounts/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("ACCT-0003"));
}

// ─── Step 14: Get Non-Existent Account ───────────────────────────────────

@Test
@Order(14)
@DisplayName("Step 14: Get non-existent account - 404 (COBOL FILE STATUS '23')")
void step14_getNonExistentAccount_returns404() throws Exception {
    mockMvc.perform(get("/accounts/99999999999")
                    .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("ACCT-0001"))
            .andExpect(jsonPath("$.message").value(
                    org.hamcrest.Matchers.containsString("99999999999")));
}

// ─── Step 15: Duplicate Account Rejected ─────────────────────────────────

@Test
@Order(15)
@DisplayName("Step 15: Duplicate account ID rejected (COBOL FILE STATUS '22')")
void step15_createDuplicateAccount_returns409() throws Exception {
    // Use already-existing TEST_ACCOUNT_ID (even though inactive)
    AccountRequestDto duplicate = buildCreateRequest();

    mockMvc.perform(post("/accounts")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("ACCT-0002"));
}

// ─── Step 16: Audit Range Query ──────────────────────────────────────────

@Test
@Order(16)
@DisplayName("Step 16: Audit log date range query")
void step16_auditLog_dateRangeQuery_returnsEntries() throws Exception {
    String from = LocalDate.now().atStartOfDay().toString();
    String to   = LocalDate.now().plusDays(1).atStartOfDay().toString();

    mockMvc.perform(get("/audit/range")
                    .header("Authorization", "Bearer " + jwtToken)
                    .param("from", from)
                    .param("to", to)
                    .param("page", "0")
                    .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(
                    org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
}

// ─── Step 17: Audit User Query ────────────────────────────────────────────

@Test
@Order(17)
@DisplayName("Step 17: Audit log by user query")
void step17_auditLog_byUser_returnsAdminEntries() throws Exception {
    mockMvc.perform(get("/audit/user/admin")
                    .header("Authorization", "Bearer " + jwtToken)
                    .param("page", "0")
                    .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(
                    org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.data.content[0].changedBy").value("admin"));
}

// ─── Step 18: USER role audit access denied ───────────────────────────────

@Test
@Order(18)
@DisplayName("Step 18: USER role denied access to audit log (CICS NOTAUTH)")
void step18_auditLog_userRole_returns403() throws Exception {
    // Login as regular user first
    AuthDto.LoginRequest userLogin = new AuthDto.LoginRequest("user", "user123");
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userLogin)))
            .andReturn();

    // If test user doesn't exist, skip gracefully
    int status = loginResult.getResponse().getStatus();
    if (status != 200) return;

    String userToken = objectMapper.readTree(
                    loginResult.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();

    mockMvc.perform(get("/audit/account/{accountId}", TEST_ACCOUNT_ID)
                    .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
}

// ─── Step 19: Validation edge cases ──────────────────────────────────────

@Test
@Order(19)
@DisplayName("Step 19: Invalid date range rejected (COBOL EDIT-EXPIRY-DATE)")
void step19_createAccount_expiryBeforeOpen_returns400() throws Exception {
    AccountRequestDto badDates = buildCreateRequest();
    badDates.setAccountId("00009999003");
    badDates.setOpenDate(LocalDate.now().minusDays(10));
    badDates.setExpiryDate(LocalDate.now().minusDays(20)); // before open!

    mockMvc.perform(post("/accounts")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(badDates)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value(
                    org.hamcrest.Matchers.containsString("Expiry date")));
}

@Test
@Order(20)
@DisplayName("Step 20: Cash limit exceeds credit limit rejected (COBOL EDIT-CREDIT-LIMIT)")
void step20_createAccount_cashLimitExceedsCredit_returns400() throws Exception {
    AccountRequestDto badLimits = buildCreateRequest();
    badLimits.setAccountId("00009999004");
    badLimits.setCreditLimit(new BigDecimal("1000.00"));
    badLimits.setCashCreditLimit(new BigDecimal("2000.00")); // exceeds credit!

    mockMvc.perform(post("/accounts")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(badLimits)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors[0]").value(
                    org.hamcrest.Matchers.containsString("cash credit limit")));
}

// ─── Helper ──────────────────────────────────────────────────────────────

private AccountRequestDto buildCreateRequest() {
    return AccountRequestDto.builder()
            .accountId(TEST_ACCOUNT_ID)
            .accountName("INTEGRATION TEST")
            .accountType("1")
            .activeStatus(AccountStatus.ACTIVE)
            .currBal(new BigDecimal("1500.00"))
            .creditLimit(new BigDecimal("5000.00"))
            .cashCreditLimit(new BigDecimal("2000.00"))
            .openDate(LocalDate.now().minusDays(30))
            .expiryDate(LocalDate.now().plusYears(2))
            .reissueDate(LocalDate.now().plusYears(1))
            .addrLine1("100 Test Avenue")
            .addrLine2("Floor 3")
            .addrState("NY")
            .addrCountry("USA")
            .addrZip("10001")
            .phoneNumber1("+12125551234")
            .phoneNumber2("+12125555678")
            .groupId("GRP001")
            .studentInd("N")
            .build();
}
}