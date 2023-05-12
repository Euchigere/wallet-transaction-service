package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.exceptions.AccountNotFoundException;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.exceptions.TransactionException;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletBalance;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.locks.Lock;

import static com.ontop.wallet.domain.enums.WalletTransactionOperation.WITHDRAWAL;
import static com.ontop.wallet.domain.service.ModelFactory.INITIALISATION_FACTORY;
import static com.ontop.wallet.domain.service.ModelFactory.transferFrom;
import static com.ontop.wallet.domain.service.ModelFactory.userAccount;
import static com.ontop.wallet.domain.service.ModelFactory.walletTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferInitialisationServiceTest {
    private final Lock lock = mock(Lock.class);
    private final UserWalletService userWalletService = mock(UserWalletService.class);
    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final TransferRepository transferRepository = mock(TransferRepository.class);
    private final TransferInitialisationFactory transferInitialisationFactory = INITIALISATION_FACTORY;
    private final LockService lockService = mock(LockService.class);
    private final TransferPaymentProcessingService paymentProcessingService = mock(TransferPaymentProcessingService.class);

    private final TransferInitialisationService transferInitialisationService = new TransferInitialisationService(
            userWalletService,
            userAccountRepository,
            transferRepository,
            transferInitialisationFactory,
            lockService,
            paymentProcessingService
    );

    @Nested
    class InitialiseTransfer {
        @Test
        void shouldThrowIfSpecifiedAmountCurrencyIsNotCompatibleWithTargetAccountCurrency() throws AccountNotFoundException {
            final Money amountWithDifferentCurrency = new Money(BigDecimal.TEN, Currency.getInstance("EUR"));
            final UserId userId = new UserId(101L);

            when(userAccountRepository.getUserAccount(userId)).thenReturn(userAccount());

            final TransactionException thrown = assertThrows(TransactionException.class, () ->
                    transferInitialisationService.initialiseTransfer(userId, amountWithDifferentCurrency));
            assertEquals("INVALID_ACCOUNT", thrown.code());
            assertEquals("cannot process transfer to account with currency: USD", thrown.message());
            verifyNoInteractions(lockService);
        }

        @Test
        void shouldThrowIfUnableToObtainLockOnUserResource() throws AccountNotFoundException {
            final Money amount = Money.of(10L);
            final UserId userId = new UserId(101L);

            when(userAccountRepository.getUserAccount(userId)).thenReturn(userAccount());

            final ResourceLockedException thrown = assertThrows(ResourceLockedException.class, () ->
                    transferInitialisationService.initialiseTransfer(userId, amount));
            assertEquals("unable to obtain lock for user=101", thrown.message());
            verifyNoInteractions(userWalletService);
        }

        @Test
        void shouldThrowIfWalletBalanceIsLessThanRequestedAmount() throws AccountNotFoundException {
            final Money amount = Money.of(1000L);
            final UserId userId = new UserId(101L);

            when(userAccountRepository.getUserAccount(userId)).thenReturn(userAccount());
            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(userWalletService.getUserWalletBalance(userId)).thenReturn(new WalletBalance(userId, Money.of(909L)));

            final TransactionException thrown = assertThrows(TransactionException.class, () ->
                    transferInitialisationService.initialiseTransfer(userId, amount));
            assertEquals("INSUFFICIENT_FUNDS", thrown.code());
            assertEquals("user balance not sufficient to process transfer", thrown.message());
            verify(lock).unlock();
            verifyNoInteractions(transferRepository);
            verifyNoInteractions(paymentProcessingService);
        }

        @Test
        void shouldCallCreateTransactionMethodWithCorrectParameters() throws AccountNotFoundException, ResourceLockedException {
            final Money amount = Money.of(1000L);
            final UserId userId = new UserId(101L);
            final long transferId = 10L;

            when(userAccountRepository.getUserAccount(userId)).thenReturn(userAccount());
            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(userWalletService.getUserWalletBalance(userId)).thenReturn(new WalletBalance(userId, Money.of(2500L)));
            when(userWalletService.createTransaction(userId, amount, WITHDRAWAL)).thenReturn(walletTransaction(WITHDRAWAL));
            when(transferRepository.save(any(Transfer.class))).then(i -> transferFrom(i.getArgument(0), transferId));

            transferInitialisationService.initialiseTransfer(userId, amount);

            verify(lock).unlock();
            verify(userWalletService).createTransaction(userId, amount, WITHDRAWAL);
        }

        @Test
        void shouldProcessPaymentWhenTransferIsInitialised() throws AccountNotFoundException, ResourceLockedException {
            final Money amount = Money.of(1000L);
            final UserId userId = new UserId(101L);
            final long transferId = 20L;

            when(userAccountRepository.getUserAccount(userId)).thenReturn(userAccount());
            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(userWalletService.getUserWalletBalance(userId)).thenReturn(new WalletBalance(userId, Money.of(2500L)));
            when(userWalletService.createTransaction(userId, amount, WITHDRAWAL)).thenReturn(walletTransaction(WITHDRAWAL));
            when(transferRepository.save(any(Transfer.class))).then(i -> transferFrom(i.getArgument(0), transferId));

            transferInitialisationService.initialiseTransfer(userId, amount);

            verify(lock).unlock();
            verify(paymentProcessingService).processPayment(eq(new Id<>(transferId)));
        }

        @Test
        void shouldReturnInitialisedTransferModel() throws AccountNotFoundException, ResourceLockedException {
            final Money amount = Money.of(2000L);
            final UserId userId = new UserId(101L);
            final long transferId = 30L;
            final WalletTransaction walletTransaction = walletTransaction(WITHDRAWAL);

            when(userAccountRepository.getUserAccount(userId)).thenReturn(userAccount());
            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(userWalletService.getUserWalletBalance(userId)).thenReturn(new WalletBalance(userId, Money.of(2500L)));
            when(userWalletService.createTransaction(userId, amount, WITHDRAWAL)).thenReturn(walletTransaction);
            when(transferRepository.save(any(Transfer.class))).then(i -> transferFrom(i.getArgument(0), transferId));

            final Transfer transfer = transferInitialisationService.initialiseTransfer(userId, amount);

            assertEquals(TransferStatus.INITIALIZED, transfer.status());
            assertTrue(transfer.isValidStateForPayment());
            assertEquals(transferId, transfer.id().value());
            assertEquals(walletTransaction, transfer.walletTransactions().get(0));
            assertEquals(walletTransaction, transfer.getWithdrawal());
            assertEquals(Money.of(200.0), transfer.transferCharge());
            assertEquals(Money.of(1800.0), transfer.transferAmount());
            verify(lock).unlock();
        }
    }
}
