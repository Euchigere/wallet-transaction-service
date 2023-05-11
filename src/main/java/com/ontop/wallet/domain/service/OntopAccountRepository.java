package com.ontop.wallet.domain.service;

import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.exceptions.AccountNotFoundException;

public interface OntopAccountRepository {
    OntopAccount getAccount();
}
