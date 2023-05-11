package com.ontop.wallet.adapters;

import com.ontop.wallet.config.OntopAccountProperties;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.service.OntopAccountRepository;
import com.ontop.wallet.domain.valueobject.AccountName;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
@AllArgsConstructor
class OntopAccountRepositoryImpl implements OntopAccountRepository {
    private final OntopAccountProperties ontopAccountProperties;

    @Override
    public OntopAccount getAccount() {
        return OntopAccount.ontopAccount()
                .accountName(new AccountName(ontopAccountProperties.accountName()))
                .accountNumber(new AccountNumber(ontopAccountProperties.accountNumber()))
                .routingNumber(new RoutingNumber(ontopAccountProperties.routingNumber()))
                .currency(Currency.getInstance("USD"))
                .type(ontopAccountProperties.type())
                .build();
    }
}
