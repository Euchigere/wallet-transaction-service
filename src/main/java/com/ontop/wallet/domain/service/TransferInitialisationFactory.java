package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Money;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
public class TransferInitialisationFactory {
    private final BigDecimal transferChargePercentageFraction;
    private final OntopAccount ontopAccount;

    public TransferInitialisationFactory(@NonNull final OntopAccount ontopAccount, final int transferChargePercentageFraction) {
        Assert.isTrue(0 <= transferChargePercentageFraction && transferChargePercentageFraction < 100, "charge percentage should be < 100 && >= 0");
        this.transferChargePercentageFraction = BigDecimal.valueOf(transferChargePercentageFraction).divide(BigDecimal.valueOf(100.00));
        this.ontopAccount = ontopAccount;
    }

    final public @NonNull Builder transfer() {
        return new Builder();
    }

    final class Builder {
        private Money amount;
        private UserAccount targetAccount;
        private WalletTransaction walletTransaction;

        Builder withAmount(@NonNull Money amount) {
            this.amount = amount;
            return this;
        }

        Builder withTargetAccount(@NonNull UserAccount targetAccount) {
            this.targetAccount = targetAccount;
            return this;
        }

        Builder withWalletTransaction(@NonNull WalletTransaction walletTransaction) {
            this.walletTransaction = walletTransaction;
            return this;
        }

        private void validateSelf() {
            Assert.notNull(amount, "amount cannot be null");
            Assert.notNull(targetAccount, "target account cannot be null");
            Assert.notNull(walletTransaction, "transaction cannot be null");
            Assert.isTrue(walletTransaction.isWithdrawal(), "expected transaction of withdrawal type");
        }

        Transfer initialize() {
            validateSelf();
            final Money processingFee = amount.fractionOf(transferChargePercentageFraction);
            final Money transferAmount = amount.deductFrom(processingFee.value());
            final Instant created = Instant.now();
            log.info(
                    "Initialising transfer: created={}, onTopAccountNumber={}, targetAccountNumber={}, " +
                            "processingFee={}, amount={}, transactionId={}, userId={}",
                    created, ontopAccount.accountNumber().value(), targetAccount.accountNumber().value(), processingFee,
                    transferAmount, walletTransaction.walletTransactionId().value(), walletTransaction.userId().value()
            );
            return Transfer.initialize(
                    Instant.now(),
                    ontopAccount.accountNumber(),
                    targetAccount,
                    processingFee,
                    transferAmount,
                    amount.currency(),
                    TransferStatus.INITIALIZED,
                    walletTransaction
            );
        }
    }


}
