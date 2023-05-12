package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

import java.math.BigDecimal;

public class WalletClientRequests {
    static record WalletTransactionRequest(
            @NonNull @JsonProperty("user_id") Long userId,
            @NonNull BigDecimal amount
    ) { }
}
