package com.ontop.wallet.adapters.clients;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class PaymentProviderException extends RuntimeException {
    private final String message;

    public PaymentProviderException(@NonNull String message) {
        this.message = message;
    }
}
