package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;

public interface PaymentProvider {
    Payment makePayment(final Id<Transfer> transferId,
                        final Money transferAmount,
                        final UserAccount targetAccount,
                        final OntopAccount ontopAccount);
}
