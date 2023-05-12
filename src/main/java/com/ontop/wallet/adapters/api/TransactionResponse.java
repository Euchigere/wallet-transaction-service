package com.ontop.wallet.adapters.api;

import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long transactionId,
        Long userId,
        BigDecimal amount,
        Instant created,
        WalletTransactionOperation operation,
        TransactionStatus status
) {
    enum TransactionStatus {
        PROCESSING, FAILED, COMPLETED
    }

    static TransactionResponse of(TransactionStatus status, WalletTransaction transaction) {
        return new TransactionResponse(
                transaction.id().value(),
                transaction.userId().value(),
                transaction.amount().value(),
                transaction.created(),
                transaction.operation(),
                status
        );
    }
}
