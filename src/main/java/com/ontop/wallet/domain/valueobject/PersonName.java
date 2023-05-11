package com.ontop.wallet.domain.valueobject;

import lombok.NonNull;

public record PersonName(@NonNull String firstName, @NonNull String lastName) {
    public String fullName() {
        return String.format("%s %s", firstName, lastName);
    }
 }
