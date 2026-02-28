// mapper/AccountMapper.java
package com.agilesolutions.account.mapper;

import com.agilesolutions.account.domain.dto.AccountRequestDto;
import com.agilesolutions.account.domain.dto.AccountResponseDto;
import com.agilesolutions.account.domain.dto.AccountUpdateDto;
import com.agilesolutions.account.domain.entity.Account;
import org.mapstruct.*;

/**
 * Maps between COBOL commarea field groups and Java DTOs
 * Mirrors COBOL MOVE statements from/to WS-ACCOUNT-MASTER-RECORD
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "overLimitInd", ignore = true)
    Account toEntity(AccountRequestDto dto);

    @Mapping(target = "accountTypeDescription", expression = "java(resolveAccountTypeDesc(account.getAccountType()))")
    @Mapping(target = "availableCredit", expression = "java(account.getAvailableCredit())")
    AccountResponseDto toResponseDto(Account account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "currBal", ignore = true)
    @Mapping(target = "currCycleCredit", ignore = true)
    @Mapping(target = "currCycleDebit", ignore = true)
    @Mapping(target = "openDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "overLimitInd", ignore = true)
    void updateEntityFromDto(AccountUpdateDto dto, @MappingTarget Account account);

    // Mirrors COBOL EVALUATE ACCT-TYPE-CD
    default String resolveAccountTypeDesc(String typeCode) {
        if (typeCode == null) return "UNKNOWN";
        return switch (typeCode) {
            case "1" -> "Credit";
            case "2" -> "Debit";
            default -> "Unknown";
        };
    }
}