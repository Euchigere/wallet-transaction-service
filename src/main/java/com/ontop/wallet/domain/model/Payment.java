package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.enums.PaymentStatus;
import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

@Getter
public class Payment {
    private final UUID transactionId;
    private final PaymentStatus status;

    public Payment(UUID transactionId, @NonNull PaymentStatus status) {
        this.transactionId = transactionId;
        this.status = status;
    }
}
