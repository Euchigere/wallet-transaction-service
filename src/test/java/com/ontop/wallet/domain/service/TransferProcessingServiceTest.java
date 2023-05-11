package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static com.ontop.wallet.domain.enums.TransferStatus.PROCESSING;
import static com.ontop.wallet.domain.enums.WalletTransactionOperation.REFUND;
import static com.ontop.wallet.domain.service.ModelFactory.ontopAccount;
import static com.ontop.wallet.domain.service.ModelFactory.transferBuilder;
import static com.ontop.wallet.domain.service.ModelFactory.transferFrom;
import static com.ontop.wallet.domain.service.ModelFactory.transferInit;
import static com.ontop.wallet.domain.service.ModelFactory.walletTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class TransferProcessingServiceTest {
    private final Lock lock = mock(Lock.class);
    private final TransferRepository transferRepository = mock(TransferRepository.class);
    private final PaymentProvider paymentProvider = mock(PaymentProvider.class);
    private final OntopAccountRepository ontopAccountRepository = mock(OntopAccountRepository.class);
    private final LockService lockService = mock(LockService.class);
    private final TransactionEventPublisher eventPublisher = mock(TransactionEventPublisher.class);

    private final TransferProcessingService processingService = new TransferProcessingService(
            transferRepository,
            paymentProvider,
            ontopAccountRepository,
            lockService,
            eventPublisher
    );

    @Nested
    class ProcessTransfer {
        @Test
        void shouldThrowIfUnableToObtainLockOnTransferResource() {
            final ResourceLockedException thrown = assertThrows(ResourceLockedException.class,
                    () -> processingService.processTransfer(new Id<>(10L)));
            assertEquals("unable to obtain lock for resource: transferId=10", thrown.message());
        }

        @Test
        void shouldStopProcessingIfTransferNotFound() throws TransferNotFoundException, ResourceLockedException {
            final Id<Transfer> transferId = new Id<Transfer>(11L);

            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(transferRepository.findById(transferId)).thenThrow(new TransferNotFoundException("transfer not found"));

            processingService.processTransfer(transferId);

            verify(transferRepository).findById(transferId);
            verifyNoInteractions(ontopAccountRepository);
            verifyNoInteractions(paymentProvider);
            verify(lock).unlock();
        }

        @TestFactory
        Collection<DynamicTest> shouldStopProcessingIfTransferStateNotValid() {
            final Id<Transfer> transferId = new Id<Transfer>(11L);

            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            final AtomicInteger index = new AtomicInteger();
            return transfersWithInvalidState()
                    .entrySet()
                    .stream()
                    .map(entry -> dynamicTest(entry.getKey(), () -> {
                        when(transferRepository.findById(transferId)).thenReturn(entry.getValue());

                        processingService.processTransfer(transferId);

                        verifyNoInteractions(ontopAccountRepository);
                        verifyNoInteractions(paymentProvider);
                        verify(lock, times(index.incrementAndGet())).unlock();
                    })).collect(Collectors.toList());
        }

        @Test
        void shouldRecordPaymentDetailsAndPersistNewTransferStateAfterPayment() throws TransferNotFoundException, ResourceLockedException {
            final Id<Transfer> transferId = new Id<Transfer>(11L);
            final Transfer transfer =  transferFrom(transferInit(), transferId.value());
            final OntopAccount ontopAccount = ontopAccount();
            final Payment payment = new Payment(UUID.randomUUID(), PaymentStatus.FAILED);

            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(lockService.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(transferRepository.findById(transferId)).thenReturn(transfer);
            when(ontopAccountRepository.getAccount()).thenReturn(ontopAccount);
            when(paymentProvider.makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount))
                    .thenReturn(payment);

            processingService.processTransfer(transferId);

            verify(transferRepository).save(transferArgumentCaptor.capture());
            verify(lock).unlock();
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(payment.transactionId(), savedTransfer.paymentTransactionId().value());
            assertEquals(TransferStatus.FAILED, savedTransfer.status());
        }

        private Map<String, Transfer> transfersWithInvalidState() {
            return Map.of(
                    "invalid transfer status", transferBuilder().status(PROCESSING).build(),
                    "empty transaction", transferBuilder().walletTransactions(Collections.emptyList()).build(),
                    "invalid transaction", transferBuilder().walletTransactions(List.of(walletTransaction(REFUND))).build(),
                    "non null transactionId", transferBuilder().paymentTransactionId(new PaymentTransactionId(UUID.randomUUID())).build()
            );
        }
    }
}
