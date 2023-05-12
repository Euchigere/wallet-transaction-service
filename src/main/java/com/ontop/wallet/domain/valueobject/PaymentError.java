package com.ontop.wallet.domain.valueobject;

public record PaymentError(String value) {
    public static final String TIMEOUT = "timeout";

    public Boolean isTimeout() {
        return value.contains(TIMEOUT);
    }
}
