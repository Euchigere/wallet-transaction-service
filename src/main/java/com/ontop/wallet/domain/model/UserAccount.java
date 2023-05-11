package com.ontop.wallet.domain.model;

import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.NationalIdNumber;
import com.ontop.wallet.domain.valueobject.PersonName;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import com.ontop.wallet.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.Currency;

@Getter
public class UserAccount extends Account {
    private final Id<UserAccount> id;
    private final Instant created;
    private final Instant updated;
    private final UserId userId;
    private final PersonName name;
    private final NationalIdNumber nationalIdNumber;

    public boolean isCompatibleWithCurrency(Currency currency) {
        return this.currency.equals(currency);
    }

    @Builder(builderMethodName = "userAccount")
    private UserAccount(
            @NonNull final Id<UserAccount> id,
            @NonNull final Instant created,
            @NonNull final Instant updated,
            @NonNull final UserId userId,
            @NonNull final PersonName name,
            @NonNull final AccountNumber accountNumber,
            @NonNull final Currency currency,
            @NonNull final RoutingNumber routingNumber,
            @NonNull final NationalIdNumber nationalIdNumber
    ) {
        super(accountNumber, currency, routingNumber);
        this.id = id;
        this.created = created;
        this.updated = updated;
        this.userId = userId;
        this.name = name;
        this.nationalIdNumber = nationalIdNumber;
    }
}
