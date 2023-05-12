package com.ontop.wallet.domain.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class TransactionException extends RuntimeException {
    private final String code;
    private final String message;

    public TransactionException(@NonNull String code, @NonNull String message) {
        this.code = code;
        this.message = message;
    }
}
