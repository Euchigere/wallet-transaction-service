package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import lombok.Getter;

import java.util.Currency;

@Getter
public abstract class Account {
    protected final AccountNumber accountNumber;
    protected final Currency currency;
    protected final RoutingNumber routingNumber;

    protected Account(AccountNumber accountNumber, Currency currency, RoutingNumber routingNumber) {
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.routingNumber = routingNumber;
    }
}
