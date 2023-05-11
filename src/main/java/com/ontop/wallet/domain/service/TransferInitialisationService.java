package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.exceptions.AccountNotFoundException;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.events.TransferInitialisedEvent;
import com.ontop.wallet.domain.exceptions.TransactionException;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletBalance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;

import static com.ontop.wallet.domain.enums.WalletTransactionOperation.WITHDRAWAL;

@Service
@Slf4j
@AllArgsConstructor
public class TransferInitialisationService {
    private final static String INSUFFICIENT_FUNDS = "user balance not sufficient to process transfer";
    private final static String UNABLE_TO_PROCESS_TRANSFER_WITH_ACCOUNT_CURRENCY = "cannot process transfer to account with currency: ";
    private final static String UNABLE_TO_OBTAIN_LOCK_FOR_USER = "unable to obtain lock for user=";

    private final UserWalletService userWalletService;
    private final UserAccountRepository userAccountRepository;
    private final TransferRepository transferRepository;
    private final TransferInitialisationFactory transferInitialisationFactory;
    private final LockService lockService;
    private final TransactionEventPublisher eventPublisher;

    public Transfer initialiseTransfer(UserId userId, Money amount) throws AccountNotFoundException, ResourceLockedException {
        log.info("Initialising transfer: user={}, amount={}", userId.value(), amount);
        final UserAccount userAccount = userAccountRepository.getUserAccount(userId);
        log.info("Fetched user account: user={}, account={}", userId.value(), userAccount.id().value());

        if (!userAccount.isCompatibleWithCurrency(amount.currency())) {
            final String message = UNABLE_TO_PROCESS_TRANSFER_WITH_ACCOUNT_CURRENCY + userAccount.currency();
            log.error(message);
            throw new TransactionException("INVALID_ACCOUNT", message);
        }

        Lock lock = null;
        try {
            lock = lockUserResource(userId);
            final WalletBalance walletBalance = userWalletService.getUserWalletBalance(userId);
            log.info("Fetched user wallet balance: user={}, balance={}", walletBalance.userId().value(), walletBalance.balance().value());
            if (!walletBalance.mayWithdraw(amount)) {
                log.error(INSUFFICIENT_FUNDS);
                throw new TransactionException("INSUFFICIENT_FUNDS", INSUFFICIENT_FUNDS);
            }

            final WalletTransaction walletTransaction = userWalletService.createTransaction(userId, amount, WITHDRAWAL);
            log.info("Wallet transaction: transactionId={}, amount={}, operation={}",
                    walletTransaction.walletTransactionId().value(), walletTransaction.amount().value(), walletTransaction.operation());

            final Transfer transfer = transferRepository.save(
                    transferInitialisationFactory.transfer()
                            .withAmount(amount)
                            .withTargetAccount(userAccount)
                            .withWalletTransaction(walletTransaction)
                            .initialize()
            );
            eventPublisher.publishTransferInitialisedEvent(new TransferInitialisedEvent(transfer.id()));
            log.info("Transfer initialised: transactionId={}", transfer.id().value());
            // send notification to user
            return transfer;
        } finally {
            if (lock != null) {
                lock.unlock();
                log.info("Lock on resource released: userId={}", userId.value());
            }
        }
    }

    private Lock lockUserResource(UserId userId) throws ResourceLockedException {
        final Lock lock = lockService.getLock(getLockKey(userId));
        if (lock == null || !lock.tryLock()) {
            final String message = UNABLE_TO_OBTAIN_LOCK_FOR_USER + userId.value();
            log.debug(message);
            throw new ResourceLockedException(message);
        }
        log.info("Successfully acquired lock on resource: {}", userId.value());
        return lock;
    }

    private String getLockKey(UserId userId) {
        return "userId=" + userId.value();
    }
}
