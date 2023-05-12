package com.ontop.wallet.adapters.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferRequest(
        @NotNull(message = "user id should not be null")
        @Min(value = 1, message = "user id should not be less than 1")
        Long userId,

        @NotNull(message = "amount should not be null")
        @Min(value = 1, message = "amount should not be less than 1")
        BigDecimal amount
) { }
