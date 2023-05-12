package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontop.wallet.domain.enums.AccountType;
import com.ontop.wallet.domain.enums.PaymentStatus;
import com.ontop.wallet.domain.model.OntopAccount;
import com.ontop.wallet.domain.model.Payment;
import com.ontop.wallet.domain.model.Transfer;
import com.ontop.wallet.domain.model.UserAccount;
import com.ontop.wallet.domain.valueobject.AccountName;
import com.ontop.wallet.domain.valueobject.AccountNumber;
import com.ontop.wallet.domain.valueobject.Id;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.NationalIdNumber;
import com.ontop.wallet.domain.valueobject.PaymentError;
import com.ontop.wallet.domain.valueobject.PaymentTransactionId;
import com.ontop.wallet.domain.valueobject.PersonName;
import com.ontop.wallet.domain.valueobject.RoutingNumber;
import com.ontop.wallet.domain.valueobject.UserId;
import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class PaymentClientServiceTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);

    private final PaymentClientService paymentClientService = new PaymentClientService(restTemplate, new ObjectMapper());

    private final OntopAccount ontopAccount = OntopAccount.ontopAccount()
            .accountName(new AccountName("ONTOP INC"))
            .accountNumber(new AccountNumber("0245253419"))
            .currency(Money.DEFAULT_CURRENCY)
            .routingNumber(new RoutingNumber("028444018"))
            .type(AccountType.COMPANY)
            .build();

    private final UserAccount targetAccount = UserAccount.userAccount()
            .id((new Id<>(1L)))
            .created(Instant.now())
            .updated(Instant.now())
            .userName(new PersonName("TONY", "STARK"))
            .userId(new UserId(101L))
            .accountNumber(new AccountNumber("1885226711"))
            .currency(Money.DEFAULT_CURRENCY)
            .routingNumber(new RoutingNumber("211927207"))
            .nationalIdNumber(new NationalIdNumber("1212121212"))
            .build();

    private final String requestBody = """
            {
                "source": {
                    "type": "COMPANY",
                    "sourceInformation": {
                        "name": "ONTOP INC"
                    },
                    "account": {
                        "accountNumber": "0245253419",
                        "currency": "USD",
                        "routingNumber": "028444018"
                    }
                },
                "destination": {
                    "name": "TONY STARK",
                    "account": {
                        "accountNumber": "1885226711",
                        "currency": "USD",
                        "routingNumber": "211927207"
                    }
                },
                "amount": 1000
            }
            """.stripIndent();

    private final String successFulResponse = """
            {
                "requestInfo": {
                    "status": "Processing"
                },
                "paymentInfo": {
                    "amount": 1000,
                    "id": "70cfe468-91b9-4e04-8910-5e8257dfadfa"
                }
            }
            """.stripIndent();

    private final String failureResponse = """
            {
                "requestInfo": {
                    "status": "Failed",
                    "error": "bank rejected payment"
                },
                "paymentInfo": {
                    "amount": 1000,
                    "id": "7633f4c9-51e4-4b62-97b0-51156966f1d7"
                }
            }
            """.stripIndent();

    @Nested
    class MakePayment {
        @Test
        void shouldSerialiseExpectedRequestCorrectly() throws JSONException {
            final Id<Transfer> transferId = new Id<>(10L);
            final Money transferAmount = Money.of(1000L);
            final String requestPath = "/api/v1/payments";
            final ArgumentCaptor<HttpEntity<String>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(successFulResponse, HttpStatus.OK));

            paymentClientService.makePayment(transferId, transferAmount, targetAccount, ontopAccount);

            verify(restTemplate).exchange(eq(requestPath), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class));

            final HttpEntity<String> httpEntity = httpEntityCaptor.getValue();

            JSONAssert.assertEquals(requestBody, httpEntity.getBody(), true);
            assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getAccept().get(0));
            assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getContentType());
        }

        @Test
        void shouldDeserializeExpectedResponseBodyCorrectly() {
            final Id<Transfer> transferId = new Id<>(10L);
            final Money transferAmount = Money.of(1000L);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(successFulResponse, HttpStatus.OK));

            final Payment payment = paymentClientService.makePayment(transferId, transferAmount, targetAccount, ontopAccount);

            assertEquals(PaymentStatus.PROCESSING, payment.status());
            assertEquals(new PaymentTransactionId(UUID.fromString("70cfe468-91b9-4e04-8910-5e8257dfadfa")), payment.transactionId());
        }

        @Test
        void shouldTransform5xxServerErrorCorrectly() {
            final Id<Transfer> transferId = new Id<>(10L);
            final Money transferAmount = Money.of(1000L);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new HttpServerErrorException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "internal server error",
                            failureResponse.getBytes(StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8
                    ));

            final Payment payment = paymentClientService.makePayment(transferId, transferAmount, targetAccount, ontopAccount);

            assertEquals(PaymentStatus.FAILED, payment.status());
            assertEquals(new PaymentTransactionId(UUID.fromString("7633f4c9-51e4-4b62-97b0-51156966f1d7")), payment.transactionId());
            assertEquals(Money.of(1000L), payment.amount());
            assertEquals(new PaymentError("bank rejected payment"), payment.error());
        }

        @Test
        void shouldTransform4xxClientErrorCorrectly() {
            final Id<Transfer> transferId = new Id<>(10L);
            final Money transferAmount = Money.of(1000L);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

            final PaymentProviderException thrown = assertThrows(PaymentProviderException.class,
                    () -> paymentClientService.makePayment(transferId, transferAmount, targetAccount, ontopAccount));

            assertEquals("Invalid request to provider api", thrown.message());
        }
    }
}
