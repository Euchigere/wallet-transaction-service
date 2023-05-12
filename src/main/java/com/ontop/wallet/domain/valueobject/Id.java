package com.ontop.wallet.domain.valueobject;

import lombok.NonNull;

public record Id<T>(@NonNull Long value) {}
