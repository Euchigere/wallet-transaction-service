package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.service.PaymentProvider;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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
            log.error("Serialisation of payment request body failed: transferId={}", transferId.value());
            return new PaymentProviderException("Serialisation of payment request body failed");
        });

        log.info("Payment request: {}", requestBody);
        final ResponseEntity<String> response;
        try {
            response = post(PAYMENT_URL_PATH, requestBody);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Payment processing failed: transferId={}", transferId.value(), ex);
            final PaymentApiResponse responseBody = transformException(ex);
            return responseBody.toPayment();
        }
        final PaymentApiResponse responseBody = parseResponse(response);
        log.info("Payment processing successful: transferId={}; response={}", transferId.value(), response.getBody());
        return responseBody.toPayment();
    }

    private PaymentApiResponse transformException(final HttpStatusCodeException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            throw new PaymentProviderException("Invalid request to provider api");
        }

        return parse(ex.getResponseBodyAsString(), PaymentApiResponse.class).orElseThrow(() -> {
            log.error("Deserialization of payment api exception failed: response={}, statusCode={}",
                    ex.getResponseBodyAsString(), ex.getStatusCode());
            return new PaymentProviderException("Deserialization of payment api exception failed");
        });
    }

    private PaymentApiResponse parseResponse(final ResponseEntity<String> response) {
        return parse(response.getBody(), PaymentApiResponse.class).orElseThrow(() -> {
            log.error("Deserialization of payment response body failed: response={}, statusCode={}",
                    response.getBody(), response.getStatusCode().value());
            return new JsonParseException();
        });
    }

}
