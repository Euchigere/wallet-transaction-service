package com.ontop.wallet.domain.service;

import com.ontop.wallet.adapters.clients.PaymentProviderException;
import com.ontop.wallet.domain.exceptions.TransferNotFoundException;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParseException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TransferPaymentProcessingService {
    private static final int FIRST_RETRY_COUNT = 0;

    @Value("${ontop.payment.max-retries:2}")
    private final int maxRetries;

    @Value("${ontop.payment.retry-delay-factor-seconds:5}")
    private final int retryDelayFactorSeconds;

    private final TransferRepository transferRepository;
    private final PaymentProvider paymentProvider;
    private final OntopAccountRepository ontopAccountRepository;
    private final TransferReversalService transferReversalService;

    @Async
    public void processPayment(final Id<Transfer> transferId) {
        log.info("Transfer payment processing started: transferId={}", transferId.value());
        processRetryablePayment(transferId, FIRST_RETRY_COUNT);
    }

    private void processRetryablePayment(final Id<Transfer> transferId, final int retryCount) {
        log.info("Trying payment for transfer: transferId={}, retryCount={}", transferId.value(), retryCount);
        try {
            final Transfer transfer = transferRepository.findById(transferId);
            if (transfer.isValidStateForPayment()) {
                processRetryablePayment(transfer, retryCount);
            } else {
                log.error("Transfer state is invalid, cannot process payment: transferId={}", transfer.id().value());
            }
        } catch (TransferNotFoundException ex) {
            log.error("Unable to process payment, transferId={} not found", transferId);
            // alert code owners
        } catch (Exception ex) {
            log.error("Payment processing interrupted", ex);
            // alert code owners
        }
    }

    private void processRetryablePayment(final Transfer transfer, final int retryCount) {
        try {
            final Payment payment = makePayment(transfer);
            if (payment.isRetryable()) {
                retryPayment(transfer, retryCount);
            } else if (payment.isFailed()) {
                log.info("Payment failed: transferId={}", transfer.id().value());
                initiateTransferReverse(transfer);
            } else {
                transfer.toProcessingState();
                transferRepository.save(transfer);
            }
        } catch (final PaymentProviderException ex) {
            log.error("Payment processing failed: transferId={}", transfer.id().value(), ex);
            initiateTransferReverse(transfer);
        } catch (final ResourceAccessException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                retryPayment(transfer, retryCount);
            } else {
                log.error("Exception while processing payment", ex);
                handlePaymentProcessingErrored(transfer);
            }
        } catch (final JsonParseException ex) {
            handlePaymentProcessingErrored(transfer);
        } catch (final Exception ex) {
            log.error("Exception while processing payment", ex);
            handlePaymentProcessingErrored(transfer);
        }
    }

    private Payment makePayment(final Transfer transfer) {
        final Payment payment = paymentProvider.makePayment(
                transfer.id(),
                transfer.transferAmount(),
                transfer.targetAccount(),
                ontopAccountRepository.getAccount()
        );
        transfer.recordPayment(payment);
        return payment;
    }

    private void retryPayment(final Transfer transfer, final int retryCount) {
        if (retryCount + 1 <= maxRetries) {
            transferRepository.save(transfer);
            final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.schedule(
                    () -> processRetryablePayment(transfer.id(), retryCount + 1),
                    (long) retryDelayFactorSeconds * retryCount,
                    TimeUnit.SECONDS
            );
            executorService.shutdown();
        } else {
            initiateTransferReverse(transfer);
        }
    }

    private void initiateTransferReverse(final Transfer transfer) {
        transfer.toFailedState();
        transferRepository.save(transfer);
        transferReversalService.reverseTransfer(transfer.id());
    }

    private void handlePaymentProcessingErrored(final Transfer transfer) {
        // alert code owners
        transfer.toUnknownState();
        transferRepository.save(transfer);
    }
}
