package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.valueobject.WalletBalance;
import com.ontop.wallet.domain.valueobject.UserId;

public interface UserWalletService {
    WalletBalance getUserWalletBalance(UserId userId);

    WalletTransaction createTransaction(UserId userId, Money amount, WalletTransactionOperation operation);
}
