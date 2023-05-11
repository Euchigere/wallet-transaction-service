package com.ontop.wallet.adapters.clients;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.http.HttpStatusCode;

@Getter
public class WalletClientException extends RuntimeException {
    private final String code;

    private final String message;
    private final HttpStatusCode status;

    public WalletClientException(@NonNull String message, @NonNull String code, @NonNull HttpStatusCode status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
