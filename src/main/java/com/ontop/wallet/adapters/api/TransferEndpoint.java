package com.ontop.wallet.adapters.api;

import com.ontop.wallet.domain.exceptions.AccountNotFoundException;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransferEndpoint {
    private final TransferEndpointService transferEndpointService;

    @PostMapping(value = "/transfers", produces = "application/json", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransfer(@RequestBody @Valid TransferRequest transferRequest)
            throws ResourceLockedException, AccountNotFoundException {
        return transferEndpointService.transfer(transferRequest.userId(), transferRequest.amount());
    }
}
