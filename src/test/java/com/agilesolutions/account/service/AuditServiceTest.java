// test/service/AuditServiceTest.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.entity.AuditLog;
import com.agilesolutions.account.domain.enums.AccountStatus;
import com.agilesolutions.account.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService - Audit trail tests (replaces COBOL VSAM audit writes)")
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    // ObjectMapper must be injected manually since @InjectMocks won't autowire it
    @BeforeEach
    void setUp() {
        // Inject real ObjectMapper via reflection
        try {
            var field = AuditService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(auditService, new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject ObjectMapper", e);
        }
    }

    @Test
    @DisplayName("logCreate: saves CREATE audit entry with correct fields")
    void testLogCreate_savesCorrectAuditEntry() {
        // Set up security context (replaces COBOL EXEC CICS ASSIGN USERID)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null));

        Account account = buildSampleAccount();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.logCreate("ACCOUNT", "00001001001", account);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("ACCOUNT");
        assertThat(saved.getEntityId()).isEqualTo("00001001001");
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getChangedBy()).isEqualTo("testuser");
        assertThat(saved.getChangedAt()).isNotNull();
        assertThat(saved.getOldValue()).isNull();
        assertThat(saved.getNewValue()).isNotBlank();

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("logUpdate: saves UPDATE entry with old and new values")
    void testLogUpdate_savesOldAndNewValues() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("adminuser", null));

        Account account = buildSampleAccount();
        String oldValue = "{\"accountId\":\"00001001001\",\"accountName\":\"OLD NAME\"}";

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.logUpdate("ACCOUNT", "00001001001", oldValue, account);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("UPDATE");
        assertThat(saved.getOldValue()).isEqualTo(oldValue);
        assertThat(saved.getNewValue()).isNotBlank();
        assertThat(saved.getChangedBy()).isEqualTo("adminuser");

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("logDelete: saves DELETE entry with old value only")
    void testLogDelete_savesOldValueOnly() {
        String oldValue = "{\"accountId\":\"00001001001\"}";
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.logDelete("ACCOUNT", "00001001001", oldValue);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("DELETE");
        assertThat(saved.getOldValue()).isEqualTo(oldValue);
        assertThat(saved.getNewValue()).isNull();
    }

    @Test
    @DisplayName("getCurrentUser: anonymous user when no security context")
    void testGetCurrentUser_anonymous_whenNoContext() {
        SecurityContextHolder.clearContext();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.logDelete("ACCOUNT", "00001001001", "{}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isEqualTo("ANONYMOUS");
    }

    @Test
    @DisplayName("serializeAccount: returns valid JSON string")
    void testSerializeAccount_returnsValidJson() {
        Account account = buildSampleAccount();
        String json = auditService.serializeAccount(account);

        assertThat(json).isNotBlank();
        assertThat(json).contains("00001001001");
        assertThat(json).contains("TEST ACCOUNT");
    }

    @Test
    @DisplayName("auditLog failure: does not propagate exception to caller")
    void testAuditLog_repositoryFailure_doesNotPropagate() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw - audit failure must not break business flow
        assertThatCode(() -> auditService.logCreate("ACCOUNT", "00001001001",
                buildSampleAccount()))
                .doesNotThrowAnyException();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Account buildSampleAccount() {
        return Account.builder()
                .id(1L)
                .accountId("00001001001")
                .accountName("TEST ACCOUNT")
                .accountType("1")
                .activeStatus(AccountStatus.ACTIVE)
                .currBal(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .openDate(LocalDate.now().minusDays(30))
                .overLimitInd("N")
                .version(0L)
                .build();
    }
}