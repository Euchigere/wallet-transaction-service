package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontop.wallet.domain.enums.WalletTransactionOperation;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.WalletBalance;
import com.ontop.wallet.domain.service.UserWalletService;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletTransactionId;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Component
public class WalletClientService extends ApiClient implements UserWalletService {
    private final static String BALANCE_URL_PATH_TEMPLATE = "/wallets/balance?user_id=%d";
    private final static String TRANSACTION_URL_PATH = "/wallets/transactions";

    public WalletClientService(RestTemplate clientRestTemplate, ObjectMapper mapper) {
        super(clientRestTemplate, mapper);
    }

    @Override
    public WalletBalance getUserWalletBalance(UserId userId) {
        final ResponseEntity<String> response;
        final String requestPath = String.format(BALANCE_URL_PATH_TEMPLATE, userId.value());
        try {
            log.info("Fetching wallet balance for user={}, path={}", userId.value(), requestPath);
            response = get(requestPath);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error(ex.getResponseBodyAsString(), ex);
            throw transformException(ex);
        }

        final WalletBalanceResponse responseBody = parse(response.getBody(), WalletBalanceResponse.class).orElseThrow(() -> {
            log.error("Failed to get wallet balance response body for user={}", userId.value());
            return new WalletClientException("unable to parse client response body", "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        });
        return responseBody.toWalletBalance();
    }

    @Override
    public WalletTransaction createTransaction(UserId userId, Money amount, WalletTransactionOperation operation) {
        log.info("Creating transaction: userId={}, amount={}, operation={}", userId.value(), amount.value(), operation);

        WalletTransactionRequest request = new WalletTransactionRequest(userId.value(), Objects.requireNonNull(getRequestAmount(amount.value(), operation)));
        String requestBody = writeValueAsString(request).orElseThrow(() -> {
            log.error("Failed to write create transaction request body: ={}", request);
            return new WalletClientException("error writing client request body", "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        });

        final ResponseEntity<String> response;
        try {
            log.info("Create transaction request={}", request);
            response = post(TRANSACTION_URL_PATH, requestBody);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Create transaction request failed: response={}", ex.getResponseBodyAsString(), ex);
            throw transformException(ex);
        }

        final WalletTransactionResponse transactionResponse = parse(response.getBody(), WalletTransactionResponse.class).orElseThrow(() -> {
            log.error("Failed to parse create transaction response body: ={}", response.getBody());
            return new WalletClientException("failed to parse client response body", "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        });
        return transactionResponse.toTransaction(operation);
    }

    private BigDecimal getRequestAmount(BigDecimal amount, WalletTransactionOperation operation) {
        switch (operation) {
            case WITHDRAWAL -> {
                return amount.compareTo(BigDecimal.ZERO) > 0 ? amount.negate() : amount;
            }
            case REFUND -> {
                return amount.compareTo(BigDecimal.ZERO) < 0 ? amount.negate() : amount;
            }
        }
        return null;
    }

    private WalletClientException transformException(HttpStatusCodeException ex) {
        if (HttpStatus.BAD_REQUEST.isSameCodeAs(ex.getStatusCode())) {
            return new WalletClientException("bad client request", "INVALID_REQUEST", ex.getStatusCode());
        } else if (HttpStatus.NOT_FOUND.isSameCodeAs(ex.getStatusCode())) {
            return new WalletClientException("user not found", "INVALID_USER", ex.getStatusCode());
        } else if (ex.getStatusCode().is5xxServerError()) {
            return new WalletClientException("bad gateway", "BAD_GATEWAY", HttpStatus.BAD_GATEWAY);
        }
        return new WalletClientException("unable to complete request", "CLIENT_ERROR", ex.getStatusCode());
    }

    private record WalletBalanceResponse(
            @NonNull @JsonProperty("user_id") Long userId,
            @NonNull BigDecimal balance
    ) {
        private WalletBalance toWalletBalance() {
            return new WalletBalance(new UserId(userId()), Money.of(balance()));
        }
    }

    private record WalletTransactionRequest(
            @NonNull @JsonProperty("user_id") Long userId,
            @NonNull BigDecimal amount
    ) { }

    private record WalletTransactionResponse(
            @NonNull @JsonProperty("wallet_transaction_id") Long walletTransactionId,
            @NonNull BigDecimal amount,
            @NonNull @JsonProperty("user_id") Long userId
    ) {
        private WalletTransaction toTransaction(WalletTransactionOperation operation) {
            return WalletTransaction.walletTransaction()
                    .walletTransactionId(new WalletTransactionId(walletTransactionId))
                    .userId(new UserId(userId))
                    .amount(Money.of(amount))
                    .operation(operation)
                    .build();
        }
    }
}
