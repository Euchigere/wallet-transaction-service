package com.ontop.wallet.adapters;

import com.ontop.wallet.adapters.jpa.entities.AccountRecord;
import com.ontop.wallet.adapters.jpa.repository.AccountRecordRepository;
import com.ontop.wallet.domain.exceptions.AccountNotFoundException;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.service.UserAccountRepository;
import com.ontop.wallet.domain.valueobject.UserId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
class UserAccountRepositoryImpl implements UserAccountRepository {
    private final static String USER_ACCOUNT_NOT_FOUND = "account for user=%d not found";

    private final AccountRecordRepository accountRecordRepository;

    @Override
    public UserAccount getUserAccount(UserId userId) throws AccountNotFoundException {
        AccountRecord accountRecord = accountRecordRepository.findByUserId(userId.value()).orElseThrow(() -> {
            final String message = String.format(USER_ACCOUNT_NOT_FOUND, userId.value());
            log.error(message);
            return new AccountNotFoundException(message);
        });
        return accountRecord.toDomain();
    }
}
