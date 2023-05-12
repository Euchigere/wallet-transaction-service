package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

@Getter
public class Transfer extends BaseModel<Transfer> {
    private final AccountNumber ontopAccountNumber;
    private final UserAccount targetAccount;
    private final Money transferCharge;
    private final Money transferAmount;
    private final Currency currency;
    private TransferStatus status;

    private PaymentTransactionId paymentTransactionId;
    private final List<WalletTransaction> walletTransactions;

    @Builder(builderMethodName = "transfer")
    private Transfer(
            Id<Transfer> id,
            PaymentTransactionId paymentTransactionId,
            Instant created,
            Instant updated,
            @NonNull AccountNumber ontopAccountNumber,
            @NonNull UserAccount targetAccount,
            @NonNull Money transferCharge,
            @NonNull Money transferAmount,
            @NonNull Currency currency,
            @NonNull TransferStatus status,
            @NonNull List<WalletTransaction> walletTransactions
    ) {
        super(id, created, updated);
        this.paymentTransactionId = paymentTransactionId;
        this.ontopAccountNumber = ontopAccountNumber;
        this.targetAccount = targetAccount;
        this.transferCharge = transferCharge;
        this.transferAmount = transferAmount;
        this.currency = currency;
        this.status = status;
        this.walletTransactions = walletTransactions;
    }

    public void recordPayment(@NonNull Payment payment) {
        Assert.isTrue(paymentNotMade(), "expected payment not made yet");
        if (payment.transactionId() != null) {
            this.paymentTransactionId = new PaymentTransactionId(payment.transactionId());
        }
        this.status = payment.status().toTransferStatus();
    }

    public void reverseWith(@NonNull WalletTransaction transaction) {
        Assert.isTrue(transaction.isRefund(), "expected a refund");
        this.status = TransferStatus.REVERTED;
        this.walletTransactions.add(transaction);
    }

    private boolean isInitialisedState() {
        return TransferStatus.INITIALIZED.equals(this.status);
    }

    private boolean hasOnlyOneWalletTransactionAndItIsWithdrawal() {
        return this.walletTransactions().size() == 1 &&
                this.walletTransactions().get(0).isWithdrawal();
    }

    private boolean paymentNotMade() {
        return this.paymentTransactionId() == null;
    }

    public boolean isValidStateForProcessing() {
        return isInitialisedState() &&
                hasOnlyOneWalletTransactionAndItIsWithdrawal() &&
                paymentNotMade();
    }

    public boolean isValidStateForRevert() {
        return isFailed() &&
                hasOnlyOneWalletTransactionAndItIsWithdrawal();
    }

    public WalletTransaction getWithdrawal() {
        return this.walletTransactions.stream()
                .filter(WalletTransaction::isWithdrawal)
                .findFirst().get();
    }

    public boolean isFailed() {
        return TransferStatus.FAILED.equals(this.status);
    }

    public boolean stateIsUnknown() {
        return TransferStatus.UNKNOWN.equals(this.status);
    }

    public void toFailedState() {
        this.status = TransferStatus.FAILED;
    }

    public void toUnknownState() {
        this.status = TransferStatus.UNKNOWN;
    }

    public static Transfer initialize(
            @NonNull Instant created,
            @NonNull AccountNumber ontopAccountNumber,
            @NonNull UserAccount targetAccount,
            @NonNull Money transferCharge,
            @NonNull Money transferAmount,
            @NonNull Currency currency,
            @NonNull TransferStatus status,
            @NonNull WalletTransaction walletTransactions
    ) {
        return new Transfer(
                null, null, created, created,
                ontopAccountNumber, targetAccount, transferCharge,
                transferAmount, currency, status, List.of(walletTransactions));
    }
}
