package com.ontop.wallet.adapters.messagebroker;

import com.ontop.wallet.config.KafkaTopicConfig;
import com.ontop.wallet.domain.events.TransferInitialisedEvent;
import com.ontop.wallet.domain.events.TransferProcessingFailedEvent;
import com.ontop.wallet.domain.service.TransactionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, KafkaEvent> kafkaTemplate;

    @Override
    public void publishTransferInitialisedEvent(TransferInitialisedEvent event) {
        log.info("Publishing event: {}", event);
        kafkaTemplate.send(KafkaTopicConfig.TRANSFER_INITIALISED, new KafkaEvent(event.transferId().value()));
    }

    @Override
    public void publishTransferProcessingFailedEvent(TransferProcessingFailedEvent event) {
        log.info("Publishing event: {}", event);
        kafkaTemplate.send(KafkaTopicConfig.TRANSFER_PROCESSING_FAILED, new KafkaEvent(event.transferId().value()));
    }
}
