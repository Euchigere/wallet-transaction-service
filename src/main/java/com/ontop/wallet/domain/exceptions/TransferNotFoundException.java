package com.ontop.wallet.domain.exceptions;

import lombok.NonNull;

public class TransferNotFoundException extends Exception {
    private final String message;

    public TransferNotFoundException(@NonNull String message) {
        this.message = message;
    }
}
