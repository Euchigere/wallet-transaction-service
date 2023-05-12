package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferReversalService {
    private final TransferRepository transferRepository;
    private final UserWalletService userWalletService;

    public void reverseTransfer(Id<Transfer> transferId) {
        log.info("Transfer reversal process started: transferId={}", transferId.value());
        try {
            Transfer transfer = transferRepository.findById(transferId);
            if (transfer.isValidStateForReversal()) {
                log.info("Transfer state validated, proceeding with reversal: transferId={}", transferId.value());

                WalletTransaction transaction =  transfer.getWithdrawal();
                WalletTransaction refund = userWalletService.createTransaction(
                        transaction.userId(),
                        transaction.amount().negate(),
                        WalletTransactionOperation.REFUND
                );
                transfer.reverseWith(refund);
                transferRepository.save(transfer);
                log.info("Transfer successfully reversed: transferId={}", transferId.value());
                // send notification to user
            } else {
                log.error("Transfer state is invalid, cannot reverse: transferId={}", transferId.value());
                // alert code owner
            }
        } catch (Exception ex) {
            log.error("Transfer reversal failed: transferId={}", transferId.value(), ex);
            // alert code owner
        }
    }
}
