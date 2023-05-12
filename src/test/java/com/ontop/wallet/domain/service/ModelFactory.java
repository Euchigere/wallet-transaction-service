package com.ontop.wallet.domain.service;

import com.ontop.wallet.config.OntopAccountProperties;
import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.AccountName;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.NationalIdNumber;
import com.ontop.wallet.domain.valueobject.PaymentError;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import com.ontop.wallet.domain.valueobject.PersonName;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletTransactionId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class ModelFactory {
    public static int TRANSFER_FEE_PERCENTAGE = 10;

    public static final TransferInitialisationFactory INITIALISATION_FACTORY =
            new TransferInitialisationFactory(ontopAccount(), TRANSFER_FEE_PERCENTAGE);

    private static OntopAccountProperties ontopAccountProperties() {
        return new OntopAccountProperties("Ontop Inc", "10101010", "20202020");
    }

    public static OntopAccount ontopAccount() {
        final OntopAccountProperties ontopAccountProperties = ontopAccountProperties();
        return OntopAccount.ontopAccount()
                .accountName(new AccountName(ontopAccountProperties.accountName()))
                .accountNumber(new AccountNumber(ontopAccountProperties.accountNumber()))
                .routingNumber(new RoutingNumber(ontopAccountProperties.routingNumber()))
                .currency(Money.DEFAULT_CURRENCY)
                .type(ontopAccountProperties.type())
                .build();
    }

    public static UserAccount userAccount() {
        return UserAccount.userAccount()
                .id(new Id<>(new Random().nextLong()))
                .created(Instant.now())
                .updated(Instant.now())
                .userName(new PersonName("Arya", "Powell"))
                .accountNumber(new AccountNumber(UUID.randomUUID().toString()))
                .routingNumber(new RoutingNumber(UUID.randomUUID().toString()))
                .nationalIdNumber(new NationalIdNumber(UUID.randomUUID().toString()))
                .currency(Money.DEFAULT_CURRENCY)
                .userId(new UserId(101L))
                .build();
    }

    public static WalletTransaction walletTransaction(final WalletTransactionOperation operation) {
        return WalletTransaction.walletTransaction()
                .id(new Id<>(new Random().nextLong()))
                .created(Instant.now())
                .operation(operation)
                .amount(Money.of(getTransactionAmount(operation)))
                .walletTransactionId(new WalletTransactionId(1010L))
                .userId(new UserId(101L))
                .build();
    }

    public static Payment payment(final PaymentStatus status) {
        return payment(status, null);
    }

    public static Payment payment(final PaymentStatus status, final PaymentError error) {
        return Payment.payment()
                .transactionId(new PaymentTransactionId(UUID.randomUUID()))
                .amount(Money.of(100L))
                .status(status)
                .error(error)
                .isCurrent(true)
                .build();
    }

    public static Transfer transferInit() {
        return INITIALISATION_FACTORY.transfer()
                .withAmount(Money.of(BigDecimal.TEN))
                .withTargetAccount(userAccount())
                .withWalletTransaction(walletTransaction(WalletTransactionOperation.WITHDRAWAL))
                .initialize();
    }

    private static BigDecimal getTransactionAmount(final WalletTransactionOperation operation) {
        BigDecimal amount = BigDecimal.valueOf(100);
        if (WalletTransactionOperation.WITHDRAWAL == operation) {
            amount = amount.negate();
        }
        return amount;
    }

    public static Transfer transferFrom(Transfer transfer, Long transferId) {
        return Transfer.transfer()
                .id(new Id<>(transferId))
                .created(transfer.created())
                .updated(transfer.updated())
                .currency(transfer.currency())
                .status(transfer.status())
                .transferCharge(transfer.transferCharge())
                .transferAmount(transfer.transferAmount())
                .targetAccount(transfer.targetAccount())
                .ontopAccountNumber(transfer.ontopAccountNumber())
                .walletTransactions(transfer.walletTransactions())
                .payments(new ArrayList<>())
                .build();
    }

    public static Transfer.TransferBuilder transferBuilder() {
        final Transfer transfer = transferInit();
        return Transfer.transfer()
                .id(new Id<>(new Random().nextLong()))
                .created(transfer.created())
                .updated(transfer.updated())
                .currency(transfer.currency())
                .status(transfer.status())
                .transferCharge(transfer.transferCharge())
                .transferAmount(transfer.transferAmount())
                .targetAccount(transfer.targetAccount())
                .ontopAccountNumber(transfer.ontopAccountNumber())
                .walletTransactions(new ArrayList<>(transfer.walletTransactions()))
                .payments(new ArrayList<>());
    }
}
