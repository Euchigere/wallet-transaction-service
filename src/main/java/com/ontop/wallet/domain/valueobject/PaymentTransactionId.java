package com.ontop.wallet.domain.valueobject;

import lombok.NonNull;

import java.util.UUID;

public record PaymentTransactionId(@NonNull UUID value) {}
