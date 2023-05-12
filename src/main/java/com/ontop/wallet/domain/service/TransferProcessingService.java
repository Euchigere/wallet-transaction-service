package com.ontop.wallet.domain.service;

import com.ontop.wallet.adapters.clients.PaymentProviderException;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.events.TransferProcessingFailedEvent;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferProcessingService {
    private final TransferRepository transferRepository;
    private final PaymentProvider paymentProvider;
    private final OntopAccountRepository ontopAccountRepository;
    private final LockService lockService;
    private final TransactionEventPublisher eventPublisher;

    public void processTransfer(Id<Transfer> transferId) throws ResourceLockedException {
        log.info("Transfer processing started: transferId={}", transferId.value());
        Lock lock = null;
        try {
            lock = lockResource(transferId);
            processTransferSynchronized(transferId);
        } finally {
            if (lock != null) {
                lock.unlock();
                log.info("Lock on resource released: transferId={}", transferId.value());
            }
        }
    }

    private void processTransferSynchronized(Id<Transfer> transferId) {
        final Optional<Transfer> maybeTransfer = getTransfer(transferId);
        if (maybeTransfer.isEmpty()) {
            return;
        }

        final Transfer transfer = maybeTransfer.get();
        if (!transfer.isValidStateForProcessing()) {
            // alert code owners
            log.error("Transfer state is invalid, cannot process: transferId={}", transferId.value());
            return;
        }
        log.info("Transfer state validated, proceeding with payment: transferId={}", transferId.value());

        try {
            final OntopAccount ontopAccount = ontopAccountRepository.getAccount();
            final Payment payment = paymentProvider.makePayment(
                    transfer.id(),
                    transfer.transferAmount(),
                    transfer.targetAccount(),
                    ontopAccount
            );
            transfer.recordPayment(payment);
            transferRepository.save(transfer);
        } catch (final PaymentProviderException ex) {
            log.error("Payment processing failed: transferId={}", transferId.value(), ex);
            closeProcessing(transfer);
            return;
        } catch (final Exception ex) {
            // send alert/notification to code owners
            log.error("Unknown exception while processing payment", ex);
            transfer.toUnknownState();
            transferRepository.save(transfer);
            return;
        }
        if (transfer.isFailed()) {
            log.info("Payment processing failed: transferId={}", transferId.value());
            eventPublisher.publishTransferProcessingFailedEvent(new TransferProcessingFailedEvent(transferId));
        } else if (transfer.stateIsUnknown()) {
            log.error("Payment processing in an unknown state: transferId={}", transferId.value());
            // alert code owners
        }
        log.info("Payment processing completed successfully: transferId={}", transferId.value());
    }

    private void closeProcessing(final Transfer transfer) {
        transfer.toFailedState();
        transferRepository.save(transfer);
        eventPublisher.publishTransferProcessingFailedEvent(new TransferProcessingFailedEvent(transfer.id()));
    }

    private Optional<Transfer> getTransfer(Id<Transfer> transferId) {
        try {
            final Transfer transfer = transferRepository.findById(transferId);
            return Optional.of(transfer);
        } catch (TransferNotFoundException ex) {
            log.error("Transfer not found: transferId={}", transferId.value(), ex);
        }
        return Optional.empty();
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
        return "transferId=" + transferId.value();
    }
}
