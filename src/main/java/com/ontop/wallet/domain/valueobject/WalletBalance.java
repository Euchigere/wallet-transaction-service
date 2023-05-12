package com.ontop.wallet.domain.valueobject;

import lombok.NonNull;

public record WalletBalance(@NonNull UserId userId, @NonNull Money balance) {

    public boolean mayWithdraw(Money amount) {
        if (amount == null) {
            return false;
        }
        return balance.isSameCurrencyAndIsGreaterOrEqualTo(amount);
    }
}
