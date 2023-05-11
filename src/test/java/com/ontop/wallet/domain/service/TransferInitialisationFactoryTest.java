package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static com.ontop.wallet.domain.service.ModelFactory.TRANSFER_FEE_PERCENTAGE;
import static com.ontop.wallet.domain.service.ModelFactory.ontopAccount;
import static com.ontop.wallet.domain.service.ModelFactory.userAccount;
import static com.ontop.wallet.domain.service.ModelFactory.walletTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferInitialisationFactoryTest {
    private final TransferInitialisationFactory initialisationFactory =
            new TransferInitialisationFactory(ontopAccount(), TRANSFER_FEE_PERCENTAGE);

    @Test
    void transferInitShouldThrowIfAmountIsNotSpecified() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> initialisationFactory.transfer()
                .withTargetAccount(userAccount())
                .withWalletTransaction(walletTransaction(WalletTransactionOperation.WITHDRAWAL))
                .initialize());
        assertEquals("amount cannot be null", thrown.getMessage());
    }

    @Test
    void transferInitShouldThrowIfUserAccountIsNotSpecified() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> initialisationFactory.transfer()
                .withAmount(Money.of(BigDecimal.TEN))
                .withWalletTransaction(walletTransaction(WalletTransactionOperation.WITHDRAWAL))
                .initialize());
        assertEquals("target account cannot be null", thrown.getMessage());
    }

    @Test
    void transferInitShouldThrowIfTransactionIsNotSpecified() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> initialisationFactory.transfer()
                .withAmount(Money.of(BigDecimal.TEN))
                .withTargetAccount(userAccount())
                .initialize(), "transaction cannot be null");
        assertEquals("transaction cannot be null", thrown.getMessage());
    }

    @Test
    void transferInitShouldThrowIfCorrectTransactionIsNotSpecified() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> initialisationFactory.transfer()
                .withAmount(Money.of(BigDecimal.TEN))
                .withTargetAccount(userAccount())
                .withWalletTransaction(walletTransaction(WalletTransactionOperation.REFUND))
                .initialize(), "expected transaction of withdrawal type");
        assertEquals("expected transaction of withdrawal type", thrown.getMessage());
    }

    @Test
    void transferInitShouldReturnTransferModelWithExpectedValues() {
        final Transfer transfer = initialisationFactory.transfer()
                .withAmount(Money.of(BigDecimal.TEN))
                .withTargetAccount(userAccount())
                .withWalletTransaction(walletTransaction(WalletTransactionOperation.WITHDRAWAL))
                .initialize();
        assertEquals(TransferStatus.INITIALIZED, transfer.status());
        assertEquals(0, BigDecimal.valueOf(9).compareTo(transfer.transferAmount().value()));
        assertEquals(0, BigDecimal.ONE.compareTo(transfer.transferCharge().value()));
        assertEquals(Currency.getInstance("USD"), transfer.currency());
    }
}
