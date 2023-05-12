package com.ontop.wallet.domain.service;

import com.ontop.wallet.adapters.clients.PaymentProviderException;
import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.PaymentError;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.ontop.wallet.domain.enums.TransferStatus.PROCESSING;
import static com.ontop.wallet.domain.enums.WalletTransactionOperation.REFUND;
import static com.ontop.wallet.domain.service.ModelFactory.ontopAccount;
import static com.ontop.wallet.domain.service.ModelFactory.payment;
import static com.ontop.wallet.domain.service.ModelFactory.transferBuilder;
import static com.ontop.wallet.domain.service.ModelFactory.transferFrom;
import static com.ontop.wallet.domain.service.ModelFactory.transferInit;
import static com.ontop.wallet.domain.service.ModelFactory.walletTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class TransferPaymentProcessingServiceTest {
    private final int maxRetries = 1;
    private final int retryDelayFactorSeconds = 1;
    private final TransferRepository transferRepository = mock(TransferRepository.class);
    private final PaymentProvider paymentProvider = mock(PaymentProvider.class);
    private final OntopAccountRepository ontopAccountRepository = mock(OntopAccountRepository.class);
    private final TransferReversalService transferReversalService = mock(TransferReversalService.class);

    private final TransferPaymentProcessingService paymentProcessingService = new TransferPaymentProcessingService(
            maxRetries,
            retryDelayFactorSeconds,
            transferRepository,
            paymentProvider,
            ontopAccountRepository,
            transferReversalService
    );

    @Nested
    class ProcessTransfer {
        @Test
        void shouldStopProcessingIfTransferNotFound() throws TransferNotFoundException {
            final Id<Transfer> transferId = new Id<>(11L);

            when(transferRepository.findById(transferId)).thenThrow(new TransferNotFoundException("transfer not found"));

            paymentProcessingService.processPayment(transferId);

            verify(transferRepository).findById(transferId);
            verifyNoInteractions(ontopAccountRepository);
            verifyNoInteractions(paymentProvider);
        }

        @TestFactory
        Collection<DynamicTest> shouldStopProcessingIfTransferStateNotValid() {
            final Id<Transfer> transferId = new Id<>(12L);

            final AtomicInteger index = new AtomicInteger();
            return transfersWithInvalidState()
                    .entrySet()
                    .stream()
                    .map(entry -> dynamicTest(entry.getKey(), () -> {
                        when(transferRepository.findById(transferId)).thenReturn(entry.getValue());

                        paymentProcessingService.processPayment(transferId);

                        verifyNoInteractions(ontopAccountRepository);
                        verifyNoInteractions(paymentProvider);
                    })).collect(Collectors.toList());
        }

        @Test
        void shouldRecordPaymentDetailsAndChangeTransferStatusToProcessingIfPaymentSucceeds() throws TransferNotFoundException {
            final Id<Transfer> transferId = new Id<>(13L);
            final Transfer transfer =  transferFrom(transferInit(), transferId.value());
            final OntopAccount ontopAccount = ontopAccount();
            final Payment payment = payment(PaymentStatus.PROCESSING);
            final TransferStatus transferStatusBeforeProcessing = transfer.status();

            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(transferRepository.findById(transferId)).thenReturn(transfer);
            when(ontopAccountRepository.getAccount()).thenReturn(ontopAccount);
            when(paymentProvider.makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount))
                    .thenReturn(payment);

            paymentProcessingService.processPayment(transferId);

            verify(paymentProvider).makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount);
            verify(transferRepository).save(transferArgumentCaptor.capture());
            verifyNoInteractions(transferReversalService);
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(TransferStatus.INITIALIZED, transferStatusBeforeProcessing);
            assertEquals(payment, savedTransfer.currentPayment());
            assertEquals(PROCESSING, savedTransfer.status());
            assertNotEquals(transferStatusBeforeProcessing, savedTransfer.status());
        }

        @Test
        void shouldRecordPaymentDetailsAndInitiateReverseIfPaymentFailsAndIsNotRetryable() throws TransferNotFoundException {
            final Id<Transfer> transferId = new Id<>(14L);
            final Transfer transfer =  transferFrom(transferInit(), transferId.value());
            final OntopAccount ontopAccount = ontopAccount();
            final Payment payment = payment(PaymentStatus.FAILED);
            final TransferStatus transferStatusBeforeProcessing = transfer.status();

            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(transferRepository.findById(transferId)).thenReturn(transfer);
            when(ontopAccountRepository.getAccount()).thenReturn(ontopAccount);
            when(paymentProvider.makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount))
                    .thenReturn(payment);

            paymentProcessingService.processPayment(transferId);

            verify(paymentProvider).makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount);
            verify(transferRepository).save(transferArgumentCaptor.capture());
            verify(transferReversalService).reverseTransfer(eq(transferId));
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(TransferStatus.INITIALIZED, transferStatusBeforeProcessing);
            assertEquals(payment, savedTransfer.currentPayment());
            assertEquals(TransferStatus.FAILED, savedTransfer.status());
            assertNotEquals(transferStatusBeforeProcessing, savedTransfer.status());
        }

        @Test
        void shouldRecordPaymentDetailsAndRetryPaymentIfPaymentFailsAndIsRetryable() throws TransferNotFoundException {
            final Id<Transfer> transferId = new Id<>(15L);
            final Transfer transfer =  transferFrom(transferInit(), transferId.value());
            final OntopAccount ontopAccount = ontopAccount();
            final Payment payment = payment(PaymentStatus.FAILED, new PaymentError("timeout"));
            final TransferStatus transferStatusBeforeProcessing = transfer.status();

            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(transferRepository.findById(transferId)).thenReturn(transfer);
            when(ontopAccountRepository.getAccount()).thenReturn(ontopAccount);
            when(paymentProvider.makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount))
                    .thenReturn(payment);

            paymentProcessingService.processPayment(transferId);

            verify(transferRepository).save(transferArgumentCaptor.capture());
            verify(paymentProvider).makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount);
            verifyNoInteractions(transferReversalService);
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(TransferStatus.INITIALIZED, transferStatusBeforeProcessing);
            assertEquals(payment, savedTransfer.currentPayment());
            assertEquals(transferStatusBeforeProcessing, savedTransfer.status());
        }

        @Test
        void shouldInitiateReverseIfPaymentProcessingFails() throws TransferNotFoundException {
            final Id<Transfer> transferId = new Id<>(16L);
            final Transfer transfer =  transferFrom(transferInit(), transferId.value());
            final OntopAccount ontopAccount = ontopAccount();
            final TransferStatus transferStatusBeforeProcessing = transfer.status();

            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(transferRepository.findById(transferId)).thenReturn(transfer);
            when(ontopAccountRepository.getAccount()).thenReturn(ontopAccount);
            when(paymentProvider.makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount))
                    .thenThrow(new PaymentProviderException("payment processing failed"));

            paymentProcessingService.processPayment(transferId);

            verify(paymentProvider).makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount);
            verify(transferRepository).save(transferArgumentCaptor.capture());
            verify(transferReversalService).reverseTransfer(eq(transferId));
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(TransferStatus.INITIALIZED, transferStatusBeforeProcessing);
            assertTrue(savedTransfer.payments().isEmpty());
            assertEquals(TransferStatus.FAILED, savedTransfer.status());
            assertNotEquals(transferStatusBeforeProcessing, savedTransfer.status());
        }

        @Test
        void shouldChangeTransferStatusToUnknownIfPaymentProcessingErrors() throws TransferNotFoundException {
            final Id<Transfer> transferId = new Id<>(17L);
            final Transfer transfer =  transferFrom(transferInit(), transferId.value());
            final OntopAccount ontopAccount = ontopAccount();
            final TransferStatus transferStatusBeforeProcessing = transfer.status();

            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(transferRepository.findById(transferId)).thenReturn(transfer);
            when(ontopAccountRepository.getAccount()).thenReturn(ontopAccount);
            when(paymentProvider.makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount))
                    .thenThrow(new RuntimeException());

            paymentProcessingService.processPayment(transferId);

            verify(paymentProvider).makePayment(transfer.id(), transfer.transferAmount(), transfer.targetAccount(), ontopAccount);
            verify(transferRepository).save(transferArgumentCaptor.capture());
            verifyNoInteractions(transferReversalService);
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(TransferStatus.INITIALIZED, transferStatusBeforeProcessing);
            assertTrue(savedTransfer.payments().isEmpty());
            assertEquals(TransferStatus.UNKNOWN, savedTransfer.status());
            assertNotEquals(transferStatusBeforeProcessing, savedTransfer.status());
        }

        private Map<String, Transfer> transfersWithInvalidState() {
            return Map.of(
                    "invalid transfer status", transferBuilder().status(PROCESSING).build(),
                    "empty transaction", transferBuilder().walletTransactions(Collections.emptyList()).build(),
                    "invalid transaction", transferBuilder().walletTransactions(List.of(walletTransaction(REFUND))).build(),
                    "non null transactionId", transferBuilder().payments(List.of(payment(PaymentStatus.PROCESSING))).build()
            );
        }
    }
}
