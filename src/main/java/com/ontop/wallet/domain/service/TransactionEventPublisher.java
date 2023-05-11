package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.events.TransferInitialisedEvent;
import com.ontop.wallet.domain.events.TransferProcessingFailedEvent;

public interface TransactionEventPublisher {
    void publishTransferInitialisedEvent(final TransferInitialisedEvent event);

    void publishTransferProcessingFailedEvent(final TransferProcessingFailedEvent event);
}
