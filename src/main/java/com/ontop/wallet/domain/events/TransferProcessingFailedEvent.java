package com.ontop.wallet.domain.events;

import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;

public record TransferProcessingFailedEvent(Id<Transfer> transferId) {
}
