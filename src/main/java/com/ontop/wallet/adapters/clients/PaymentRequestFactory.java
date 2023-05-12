package com.ontop.wallet.adapters.clients;

import com.ontop.wallet.domain.enums.AccountType;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.valueobject.Money;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Currency;

public class PaymentRequestFactory {
    @Builder(builderMethodName = "request")
    private static PaymentRequest buildRequest(
            @NonNull Money amount,
            @NonNull UserAccount targetAccount,
            @NonNull OntopAccount ontopAccount
    ) {
        Assert.isTrue(amount.currency().equals(targetAccount.currency())
                && ontopAccount.currency().equals(targetAccount.currency()), "cannot make payment between accounts with different currency");

        final Account destinationAccount = Account.of(targetAccount);
        final Destination destination = new Destination(targetAccount.userName().fullName(), destinationAccount);

        final Account sourceAccount = Account.of(ontopAccount);
        final Source source = new Source(ontopAccount.type(), new AccountName(ontopAccount.accountName().value()), sourceAccount);
        return new PaymentRequest(source, destination, amount.value());
    }

    record PaymentRequest(Source source, Destination destination, BigDecimal amount) { }
    record Source(AccountType type, AccountName sourceInformation, Account account) {}
    record AccountName(String name) { }
    record Destination(String name, Account account) { }
    record Account(String accountNumber, Currency currency, String routingNumber) {
        static Account of(com.ontop.wallet.domain.model.Account account) {
            return new Account(account.accountNumber().value(), account.currency(), account.routingNumber().value());
        }
    }
}
