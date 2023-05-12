package com.ontop.wallet.adapters.messagebroker;

import com.ontop.wallet.config.KafkaTopicConfig;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.service.TransferProcessingService;
import com.ontop.wallet.domain.service.TransferRevertService;
import com.ontop.wallet.domain.valueobject.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private final TransferProcessingService transferProcessingService;
    private final TransferRevertService transferRevertService;

    @KafkaListener(topics = KafkaTopicConfig.TRANSFER_INITIALISED)
    public void handleTransactionInitialisedEvent(final KafkaEvent event) throws ResourceLockedException {
        log.info("Processing Transfer: {}", event);
        transferProcessingService.processTransfer(new Id<>(event.transferId()));
    }

    @KafkaListener(topics = KafkaTopicConfig.TRANSFER_PROCESSING_FAILED)
    public void handleTransferProcessingFailedEvent(final KafkaEvent event) throws ResourceLockedException {
        log.info("Reverting transfer: {}", event);
        transferRevertService.reverseTransfer(new Id<>(event.transferId()));
    }
}
