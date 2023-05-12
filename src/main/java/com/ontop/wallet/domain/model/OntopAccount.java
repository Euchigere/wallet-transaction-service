package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import com.ontop.wallet.domain.enums.AccountType;
import com.ontop.wallet.domain.valueobject.AccountName;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Currency;

@Getter
public class OntopAccount extends Account {
    private final AccountName accountName;
    private final AccountType type;

    @Builder(builderMethodName = "ontopAccount")
    private OntopAccount(
            @NonNull final AccountName accountName,
            @NonNull final AccountNumber accountNumber,
            @NonNull final Currency currency,
            @NonNull final RoutingNumber routingNumber,
            @NonNull final AccountType type
    ) {
        super(accountNumber, currency, routingNumber);
        this.accountName = accountName;
        this.type = type;
    }
}
