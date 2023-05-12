package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletBalance;
import com.ontop.wallet.domain.valueobject.WalletTransactionId;
import lombok.NonNull;

import java.math.BigDecimal;

public class WalletClientResponses {
    static record WalletBalanceResponse(
            @NonNull @JsonProperty("user_id") Long userId,
            @NonNull BigDecimal balance
    ) {
        WalletBalance toWalletBalance() {
            return new WalletBalance(new UserId(userId()), Money.of(balance()));
        }
    }

    static record WalletTransactionResponse(
            @NonNull @JsonProperty("wallet_transaction_id") Long walletTransactionId,
            @NonNull BigDecimal amount,
            @NonNull @JsonProperty("user_id") Long userId
    ) {
        WalletTransaction toTransaction(WalletTransactionOperation operation) {
            return WalletTransaction.walletTransaction()
                    .walletTransactionId(new WalletTransactionId(walletTransactionId))
                    .userId(new UserId(userId))
                    .amount(Money.of(amount))
                    .operation(operation)
                    .build();
        }
    }
}
