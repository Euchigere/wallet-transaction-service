package com.ontop.wallet.config;

import com.ontop.wallet.domain.service.OntopAccountRepository;
import com.ontop.wallet.domain.service.TransferInitialisationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TransactionConfig {
    private static final int TRANSFER_CHARGE_PERCENT = 10;

    private final OntopAccountRepository ontopAccountRepository;

    @Bean
    public TransferInitialisationFactory transferTransactionFactory()  {
        return new TransferInitialisationFactory(ontopAccountRepository.getAccount(), TRANSFER_CHARGE_PERCENT);
    }
}
