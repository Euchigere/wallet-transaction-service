package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.exceptions.AccountNotFoundException;

public interface UserAccountRepository {
    UserAccount getUserAccount(UserId userId) throws AccountNotFoundException;
}
