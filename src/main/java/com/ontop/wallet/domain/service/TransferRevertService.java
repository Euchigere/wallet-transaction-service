package com.ontop.wallet.domain.service;

import com.ontop.wallet.adapters.clients.WalletClientException;
import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferRevertService {
    private final TransferRepository transferRepository;
    private final UserWalletService userWalletService;
    private final LockService lockService;

    public void reverseTransfer(Id<Transfer> transferId) throws ResourceLockedException {
        log.info("Transfer reverse process started: transferId={}", transferId.value());
        Lock lock = null;
        try {
            lock = lockResource(transferId);

            Transfer transfer = transferRepository.findById(transferId);
            if (!transfer.isValidStateForRevert()) {
                log.error("Transfer state is invalid, cannot reverse: transferId={}", transferId.value());
                return;
            }
            log.info("Transfer state validated, proceeding with reverse: transferId={}", transferId.value());

            WalletTransaction transaction =  transfer.getWithdrawal();
            WalletTransaction refund = userWalletService.createTransaction(
                    transaction.userId(),
                    transaction.amount().negate(),
                    WalletTransactionOperation.REFUND
            );
            transfer.reverseWith(refund);
            transferRepository.save(transfer);
            log.info("Transfer successfully reverted: transferId={}", transferId.value());
            // send notification to user
        } catch (TransferNotFoundException | WalletClientException ex) {
            log.error("Transfer reversal failed: transferId={}", transferId.value(), ex);
            // alert code owner
        } finally {
            if (lock != null) {
                lock.unlock();
                log.info("Lock on resource released: transfer={}", transferId.value());
            }
        }
    }

    private Lock lockResource(Id<Transfer> transferId) throws ResourceLockedException {
        log.info("Attempting to obtain lock for resource: {}", getLockKey(transferId));
        final Lock lock = lockService.getLock(getLockKey(transferId));
        if (lock == null || !lock.tryLock()) {
            final String message = "unable to obtain lock for resource: " + getLockKey(transferId);
            log.error(message);
            throw new ResourceLockedException(message);
        }
        log.info("Successfully acquired lock on resource: {}", getLockKey(transferId));
        return lock;
    }

    private String getLockKey(Id<Transfer> transferId) {
        return "transfer=" + transferId.value();
    }
}
