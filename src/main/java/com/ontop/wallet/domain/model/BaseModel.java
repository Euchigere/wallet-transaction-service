package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.valueobject.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseModel<T> {
    private Id<T> id;
    private Instant created;
    private Instant updated;
}
