// service/AccountServiceImpl.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.dto.*;
import com.agilesolutions.account.domain.entity.Account;
import com.agilesolutions.account.domain.enums.AccountStatus;
import com.agilesolutions.account.exception.AccountNotFoundException;
import com.agilesolutions.account.exception.BusinessValidationException;
import com.agilesolutions.account.mapper.AccountMapper;
import com.agilesolutions.account.repository.AccountRepository;
import com.agilesolutions.account.util.AccountConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Core business logic service
 *
 * Refactored from COBOL COACTUPC.cbl paragraphs:
 *
 *   PROCESS-ENTER-KEY     -> updateAccount()
 *   EDIT-ACCOUNT-DATA     -> validateAccountData() internal
 *   EDIT-ACCTID           -> validateAccountId() via ValidationService
 *   GET-ACCT-DATA         -> getAccountById()
 *   UPDATE-ACCOUNT-INFO   -> updateAccount() persistence phase
 *   SEND-ERRMSG           -> BusinessValidationException throws
 *   SEND-PLAIN-TEXT       -> API response
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final ValidationService validationService;
    private final AuditService auditService;

    /**
     * Replaces COBOL paragraph: PROCESS-ENTER-KEY (CREATE variant)
     * and WRITE ACCTDAT FROM WS-ACCOUNT-MASTER-RECORD
     */
    @Override
    public AccountResponseDto createAccount(AccountRequestDto requestDto) {
        log.info("Creating account with ID: {}", requestDto.getAccountId());

        // Mirrors COBOL: EDIT-ACCTID check for duplicate
        if (accountRepository.existsByAccountId(requestDto.getAccountId())) {
            throw new BusinessValidationException(
                    AccountConstants.ERR_ACCOUNT_EXISTS,
                    "Account already exists with ID: " + requestDto.getAccountId()
            );
        }

        // Mirrors COBOL: EDIT-ACCOUNT-DATA validations
        validationService.validateAccountData(requestDto);

        Account account = accountMapper.toEntity(requestDto);
        account.setOpenDate(requestDto.getOpenDate() != null
                ? requestDto.getOpenDate()
                : LocalDate.now());

        // Mirrors COBOL: COMPUTE OVER-LIMIT-IND
        account.setOverLimitInd(computeOverLimitInd(account));

        Account saved = accountRepository.save(account);
        log.info("Account created successfully: {}", saved.getAccountId());

        auditService.logCreate("ACCOUNT", saved.getAccountId(), saved);

        return accountMapper.toResponseDto(saved);
    }

    /**
     * Replaces COBOL paragraph: GET-ACCT-DATA
     * READ ACCTDAT INTO WS-ACCOUNT-MASTER-RECORD
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountById(String accountId) {
        log.debug("Fetching account: {}", accountId);

        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId,
                        AccountConstants.ERR_ACCOUNT_NOT_FOUND
                ));

        return accountMapper.toResponseDto(account);
    }

    /**
     * Replaces COBOL paragraphs:
     *   PROCESS-ENTER-KEY
     *   EDIT-ACCOUNT-DATA
     *   UPDATE-ACCOUNT-INFO
     *   REWRITE ACCTDAT FROM WS-ACCOUNT-MASTER-RECORD
     */
    @Override
    public AccountResponseDto updateAccount(String accountId, AccountUpdateDto updateDto) {
        log.info("Updating account: {}", accountId);

        // Mirrors COBOL: READ ACCTDAT WITH LOCK / check NOT FOUND
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId,
                        AccountConstants.ERR_ACCOUNT_NOT_FOUND
                ));

        // Mirrors COBOL: EDIT-ACCOUNT-DATA guard conditions
        if (account.getActiveStatus() == AccountStatus.N) {
            throw new BusinessValidationException(
                    AccountConstants.ERR_ACCOUNT_INACTIVE,
                    "Cannot update inactive account: " + accountId
            );
        }

        // Mirrors COBOL: EDIT-EXPIRY-DATE
        validationService.validateUpdateData(updateDto, account);

        // Capture old state for audit
        String oldValue = auditService.serializeAccount(account);

        // Mirrors COBOL: MOVE fields from commarea to ACCT-RECORD
        accountMapper.updateEntityFromDto(updateDto, account);

        // Mirrors COBOL: COMPUTE OVER-LIMIT-IND AFTER UPDATE
        account.setOverLimitInd(computeOverLimitInd(account));

        Account updated = accountRepository.save(account);
        log.info("Account updated successfully: {}", updated.getAccountId());

        auditService.logUpdate("ACCOUNT", updated.getAccountId(), oldValue, updated);

        return accountMapper.toResponseDto(updated);
    }

    /**
     * Replaces COBOL: DELETE ACCTDAT (logical delete via status flag)
     * COBOL uses ACCT-ACTIVE-STATUS = 'N' rather than physical delete
     */
    @Override
    public void deleteAccount(String accountId) {
        log.info("Deactivating account: {}", accountId);

        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId,
                        AccountConstants.ERR_ACCOUNT_NOT_FOUND
                ));

        String oldValue = auditService.serializeAccount(account);
        account.setActiveStatus(AccountStatus.N);
        accountRepository.save(account);

        auditService.logDelete("ACCOUNT", accountId, oldValue);
        log.info("Account deactivated: {}", accountId);
    }

    /**
     * Replaces COBOL: browse/scroll through ACCTDAT
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AccountResponseDto> getAllAccounts(Pageable pageable) {
        Page<Account> page = accountRepository.findAll(pageable);
        return toPagedResponse(page);
    }

    /**
     * Multi-field search replacing COBOL START/READ NEXT key logic
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AccountResponseDto> searchAccounts(
            String accountType,
            String activeStatus,
            String accountName,
            Pageable pageable) {

        Page<Account> page;

        if (accountName != null && !accountName.isBlank()) {
            page = accountRepository.findByAccountNameContaining(accountName, pageable);
        } else if (accountType != null && activeStatus != null) {
            AccountStatus status = AccountStatus.fromCode(activeStatus);
            page = accountRepository.findByAccountTypeAndActiveStatus(accountType, status, pageable);
        } else if (activeStatus != null) {
            AccountStatus status = AccountStatus.fromCode(activeStatus);
            page = accountRepository.findByActiveStatus(status, pageable);
        } else {
            page = accountRepository.findAll(pageable);
        }

        return toPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AccountResponseDto> getOverLimitAccounts(Pageable pageable) {
        Page<Account> page = accountRepository.findAccountsOverLimit(pageable);
        return toPagedResponse(page);
    }

    /**
     * Mirrors COBOL: EVALUATE TRUE
     *   WHEN ACCT-CURR-BAL > ACCT-CREDIT-LIMIT
     *     MOVE 'Y' TO ACCT-OVER-LIMIT-IND
     */
    private String computeOverLimitInd(Account account) {
        if (account.getCreditLimit() == null || account.getCurrBal() == null) {
            return AccountConstants.IND_NO;
        }
        return account.getCurrBal().compareTo(account.getCreditLimit()) > 0
                ? AccountConstants.IND_YES
                : AccountConstants.IND_NO;
    }

    private PagedResponseDto<AccountResponseDto> toPagedResponse(Page<Account> page) {
        List<AccountResponseDto> content = page.getContent()
                .stream()
                .map(accountMapper::toResponseDto)
                .toList();

        return PagedResponseDto.<AccountResponseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}