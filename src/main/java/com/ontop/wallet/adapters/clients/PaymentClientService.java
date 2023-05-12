package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.service.PaymentProvider;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class PaymentClientService extends ApiClient implements PaymentProvider {
    static final String PAYMENT_URL_PATH = "/api/v1/payments";

    public PaymentClientService(final RestTemplate clientRestTemplate, final ObjectMapper mapper) {
        super(clientRestTemplate, mapper);
    }

    @Override
    public Payment makePayment(
            final Id<Transfer> transferId,
            final Money transferAmount,
            final UserAccount targetAccount,
            final OntopAccount ontopAccount
    ) {
        log.info("Processing payment: transferId={}", transferId.value());
        final PaymentRequestFactory.PaymentRequest request = PaymentRequestFactory.request()
                .targetAccount(targetAccount)
                .ontopAccount(ontopAccount)
                .amount(transferAmount)
                .build();

        final String requestBody = writeValueAsString(request).orElseThrow(() -> {
            log.error("Failed to write payment request body");
            return new PaymentProviderException("Failed to write payment request body");
        });

        log.info("Payment request: {}", requestBody);
        final ResponseEntity<String> response;
        final PaymentApiResponse responseBody;
        try {
            response = post(PAYMENT_URL_PATH, requestBody);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Payment processing failed: transferId={}", transferId.value(), ex);
            if (ex.getStatusCode().is4xxClientError()) {
                throw new PaymentProviderException("Invalid request to provider api");
            }
            responseBody = parse(ex.getResponseBodyAsString(), PaymentApiResponse.class).orElseThrow(() -> {
                log.error("Failed to parse payment exception response body: response={}, statusCode={}",
                        ex.getResponseBodyAsString(), ex.getStatusCode());
                return new PaymentProviderException("Failed to parse payment exception response body");
            });
            return responseBody.toPayment();
        }
        log.info("Payment processing successful: transferId={}; response={}", transferId.value(), response.getBody());
        responseBody = parse(response.getBody(), PaymentApiResponse.class).orElseGet(() -> {
            log.error("Payment response parsing failed: response={}, statusCode={}", response.getBody(), response.getStatusCode().value());
            return fallBackResponse();
        });
        return responseBody.toPayment();
    }

    record PaymentApiResponse(@NonNull RequestInfo requestInfo, @NonNull PaymentInfo paymentInfo) {
        record RequestInfo(String status, String error) {}
        record PaymentInfo(UUID id, BigDecimal amount) {}

        private Payment toPayment() {
            return new Payment(
                    paymentInfo.id,
                    PaymentStatus.valueOf(requestInfo.status.toUpperCase())
            );
        }
    }

    private PaymentApiResponse fallBackResponse() {
        return new PaymentApiResponse(
                new PaymentApiResponse.RequestInfo(PaymentStatus.UNKNOWN.name(), "server error"),
                new PaymentApiResponse.PaymentInfo(null, null)
        );
    }

}
