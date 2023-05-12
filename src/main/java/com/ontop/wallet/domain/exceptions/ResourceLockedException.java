package com.ontop.wallet.domain.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ResourceLockedException extends Exception {
    private final String message;

    public ResourceLockedException(@NonNull String message) {
        this.message = message;
    }
}
