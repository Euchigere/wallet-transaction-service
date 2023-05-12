package com.ontop.wallet.adapters.clients;

import lombok.NonNull;

public class PaymentProviderException extends RuntimeException {
    private final String message;

    public PaymentProviderException(@NonNull String message) {
        this.message = message;
    }
}
