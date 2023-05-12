package com.ontop.wallet.config;

import com.ontop.wallet.domain.enums.AccountType;
import com.ontop.wallet.domain.valueobject.Money;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Currency;

@ConfigurationProperties("ontop.account")
@Getter
public class OntopAccountProperties {
    private final String accountNumber;
    private final String accountName;
    private final AccountType type;
    private final String routingNumber;
    private final Currency currency;

    @ConstructorBinding
    public OntopAccountProperties(
            @NonNull String accountName,
            @NonNull String accountNumber,
            @NonNull String routingNumber
    ) {
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.routingNumber = routingNumber;
        this.type = AccountType.COMPANY;
        this.currency = Money.DEFAULT_CURRENCY;
    }
}
