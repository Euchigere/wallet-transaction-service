package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.enums.TransferStatus;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.WalletTransaction;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.ontop.wallet.domain.enums.WalletTransactionOperation.REFUND;
import static com.ontop.wallet.domain.service.ModelFactory.transferBuilder;
import static com.ontop.wallet.domain.service.ModelFactory.walletTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferReversalServiceTest {
    private final TransferRepository transferRepository = mock(TransferRepository.class);
    private final UserWalletService userWalletService = mock(UserWalletService.class);
    private final TransferReversalService transferReversalService = new TransferReversalService(transferRepository, userWalletService);

    @Nested
    class ReverseTransfer {
        @Test
        void shouldNotReverseTransferIfTransferStateIsInvalid() throws TransferNotFoundException {
            final Transfer transfer = transferBuilder().status(TransferStatus.INITIALIZED).build();

            when(transferRepository.findById(transfer.id())).thenReturn(transfer);
            transferReversalService.reverseTransfer(transfer.id());

            verifyNoInteractions(userWalletService);

            assertEquals(TransferStatus.INITIALIZED, transfer.status());
            assertEquals(1, transfer.walletTransactions().size());
        }

        @Test
        void shouldReverseTransferIfRefundIsSuccessful() throws TransferNotFoundException {
            final Transfer transfer = transferBuilder().status(TransferStatus.FAILED).build();
            final TransferStatus transferStatusBeforeReversal = transfer.status();
            final WalletTransaction withdrawal = transfer.getWithdrawal();
            final WalletTransaction refund = walletTransaction(REFUND);
            final ArgumentCaptor<Transfer> transferArgumentCaptor = ArgumentCaptor.forClass(Transfer.class);

            when(transferRepository.findById(transfer.id())).thenReturn(transfer);
            when(userWalletService.createTransaction(withdrawal.userId(), withdrawal.amount().negate(), REFUND))
                    .thenReturn(refund);
            transferReversalService.reverseTransfer(transfer.id());

            verify(transferRepository).save(transferArgumentCaptor.capture());
            final Transfer savedTransfer = transferArgumentCaptor.getValue();

            assertEquals(TransferStatus.REVERSED, savedTransfer.status());
            assertNotEquals(transferStatusBeforeReversal, savedTransfer.status());
            assertEquals(2, transfer.walletTransactions().size());
        }
    }
}
