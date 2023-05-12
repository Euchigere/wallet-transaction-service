package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Collections;
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
    private final List<Payment> payments;
    private final List<WalletTransaction> walletTransactions;

    @Builder(builderMethodName = "transfer")
    private Transfer(
            Id<Transfer> id,
            Instant created,
            Instant updated,
            @NonNull AccountNumber ontopAccountNumber,
            @NonNull UserAccount targetAccount,
            @NonNull Money transferCharge,
            @NonNull Money transferAmount,
            @NonNull Currency currency,
            @NonNull TransferStatus status,
            @NonNull List<WalletTransaction> walletTransactions,
            @NonNull List<Payment> payments
    ) {
        super(id, created, updated);
        this.ontopAccountNumber = ontopAccountNumber;
        this.targetAccount = targetAccount;
        this.transferCharge = transferCharge;
        this.transferAmount = transferAmount;
        this.currency = currency;
        this.status = status;
        this.walletTransactions = walletTransactions;
        this.payments = payments;
    }

    public void recordPayment(@NonNull Payment payment) {
        Assert.isTrue(paymentNotMade(), "expected payment not made yet");
        Assert.isTrue(payment.isCurrent(), "expected new payment to be the current");
        this.payments.forEach(Payment::notCurrent);
        this.payments.add(payment);
    }

    public void reverseWith(@NonNull WalletTransaction transaction) {
        Assert.isTrue(transaction.isRefund(), "expected transaction to be a refund");
        Assert.isTrue(isValidStateForReversal(), "expected valid state for reversal");
        this.status = TransferStatus.REVERSED;
        this.walletTransactions.add(transaction);
    }

    private boolean isInitialised() {
        return TransferStatus.INITIALIZED.equals(this.status);
    }

    private boolean hasOnlyOneWalletTransactionAndItIsWithdrawal() {
        return this.walletTransactions().size() == 1 &&
                this.walletTransactions().get(0).isWithdrawal();
    }

    private boolean paymentNotMade() {
        return this.payments.isEmpty() ||
                this.payments.stream().allMatch(Payment::isFailed);
    }

    public boolean isValidStateForPayment() {
        return isInitialised() &&
                hasOnlyOneWalletTransactionAndItIsWithdrawal() &&
                paymentNotMade();
    }

    public boolean isValidStateForReversal() {
        return isFailed() &&
                hasOnlyOneWalletTransactionAndItIsWithdrawal() &&
                paymentNotMade();
    }

    public WalletTransaction getWithdrawal() {
        return this.walletTransactions.stream()
                .filter(WalletTransaction::isWithdrawal)
                .findFirst().get();
    }

    public Payment currentPayment() {
        return this.payments.stream()
                .filter(Payment::isCurrent)
                .findFirst()
                .get();
    }

    public boolean isFailed() {
        return TransferStatus.FAILED.equals(this.status);
    }

    public void toProcessingState() {
        this.status = TransferStatus.PROCESSING;
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
        Assert.isTrue(walletTransactions.isWithdrawal(), "expected transaction to be a withdrawal");
        return new Transfer(
                null, created, created,
                ontopAccountNumber, targetAccount, transferCharge,
                transferAmount, currency, status, List.of(walletTransactions),
                Collections.emptyList()
        );
    }
}
