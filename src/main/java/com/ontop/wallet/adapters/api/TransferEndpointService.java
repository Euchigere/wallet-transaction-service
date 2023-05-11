package com.ontop.wallet.adapters.api;

import com.ontop.wallet.domain.exceptions.AccountNotFoundException;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.service.TransferInitialisationService;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.ontop.wallet.adapters.api.TransactionResponse.TransactionStatus.PROCESSING;

@Component
@RequiredArgsConstructor
public class TransferEndpointService {
    private final TransferInitialisationService transferInitialisationService;

    TransactionResponse transfer(Long userId, BigDecimal amount) throws ResourceLockedException, AccountNotFoundException {
        final Transfer transfer = transferInitialisationService.initialiseTransfer(new UserId(userId), Money.of(amount));
        return TransactionResponse.of(PROCESSING, transfer.walletTransactions().get(0));
    }
}
