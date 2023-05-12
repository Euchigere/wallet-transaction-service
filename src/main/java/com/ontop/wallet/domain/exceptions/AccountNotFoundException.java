package com.ontop.wallet.domain.exceptions;

import lombok.NonNull;

public class AccountNotFoundException extends Exception {
    private final String message;

    public AccountNotFoundException(@NonNull String message) {
        this.message = message;
    }
}
