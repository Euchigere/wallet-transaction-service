package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletTransactionId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
public class WalletTransaction extends BaseModel<WalletTransaction> {
    private final UserId userId;
    private final WalletTransactionId walletTransactionId;
    private final WalletTransactionOperation operation;
    private final Money amount;

    @Builder(builderMethodName = "walletTransaction")
    private WalletTransaction(
            Id<WalletTransaction> id,
            Instant created,
            Instant updated,
            @NonNull UserId userId,
            @NonNull WalletTransactionId walletTransactionId,
            @NonNull WalletTransactionOperation operation,
            @NonNull Money amount
    ) {
        super(id, created, updated);
        this.userId = userId;
        this.walletTransactionId = walletTransactionId;
        this.operation = operation;
        this.amount = amount;
    }

    public boolean isRefund() {
        return WalletTransactionOperation.REFUND.equals(this.operation);
    }

    public boolean isWithdrawal() {
        return WalletTransactionOperation.WITHDRAWAL.equals(this.operation);
    }
}
