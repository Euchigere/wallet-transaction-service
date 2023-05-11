package com.ontop.wallet.domain.events;

import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.valueobject.Id;
import lombok.NonNull;

public record TransferInitialisedEvent(@NonNull Id<Transfer> transferId) { }
