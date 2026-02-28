// service/AccountService.java
package com.agilesolutions.account.service;

import com.agilesolutions.account.domain.dto.*;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    AccountResponseDto createAccount(AccountRequestDto requestDto);

    AccountResponseDto getAccountById(String accountId);

    AccountResponseDto updateAccount(String accountId, AccountUpdateDto updateDto);

    void deleteAccount(String accountId);

    PagedResponseDto<AccountResponseDto> getAllAccounts(Pageable pageable);

    PagedResponseDto<AccountResponseDto> searchAccounts(
            String accountType,
            String activeStatus,
            String accountName,
            Pageable pageable);

    PagedResponseDto<AccountResponseDto> getOverLimitAccounts(Pageable pageable);
}