package com.ontop.wallet.adapters.api;

import jakarta.validation.constraints.NotNull;

public record ApiError(@NotNull String code, @NotNull String message) { }
