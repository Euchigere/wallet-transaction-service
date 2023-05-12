package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontop.wallet.domain.model.WalletTransaction;
import com.ontop.wallet.domain.valueobject.Money;
import com.ontop.wallet.domain.valueobject.UserId;
import com.ontop.wallet.domain.valueobject.WalletBalance;
import com.ontop.wallet.domain.valueobject.WalletTransactionId;
import org.json.JSONException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ontop.wallet.domain.enums.WalletTransactionOperation.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class WalletClientServiceTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final WalletClientService walletClientService = new WalletClientService(restTemplate, new ObjectMapper());

    @Nested
    class GetUserWalletBalance {
        @Test
        void shouldDeserializeResponseBodyCorrectly() {
            final String responseBody = """
                    {
                        "balance": 2000,
                        "user_id": 1010
                    }
                    """.stripIndent();
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            final WalletBalance balance = walletClientService.getUserWalletBalance(new UserId(101L));

            assertEquals(BigDecimal.valueOf(2000) ,balance.balance().value());
            assertEquals(1010L, balance.userId().value());
        }

        @Test
        void shouldFormatRequestUrlCorrectly() {
            final String responseBody = """
                    {
                        "balance": 2500,
                        "user_id": 1000
                    }
                    """.stripIndent();
            final String expectedUrl = "/wallets/balance?user_id=101";
            final ArgumentCaptor<HttpEntity<String>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            walletClientService.getUserWalletBalance(new UserId(101L));
            verify(restTemplate).exchange(eq(expectedUrl), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(String.class));
            final HttpEntity<String> httpEntity = httpEntityCaptor.getValue();
            assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getAccept().get(0));
        }

        @TestFactory
        List<DynamicTest> shouldTransform4xxClientErrorCorrectly() {
            return Map.of(
                    HttpStatus.NOT_FOUND, List.of("INVALID_USER", "user not found"),
                    HttpStatus.BAD_REQUEST, List.of("INVALID_REQUEST", "bad client request"),
                    HttpStatus.FORBIDDEN, List.of("CLIENT_ERROR", "unable to complete request")
            ).entrySet()
                    .stream()
                    .map(entry -> dynamicTest(entry.getKey().name(), () -> {
                when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new HttpClientErrorException(entry.getKey()));

                final WalletClientException thrown = assertThrows(
                        WalletClientException.class,
                        () -> walletClientService.getUserWalletBalance(new UserId(102L))
                );
                assertEquals(thrown.code(), entry.getValue().get(0));
                assertEquals(thrown.message(), entry.getValue().get(1));
            })).collect(Collectors.toList());
        }

        @Test
        void shouldTransform5xxServerErrorCorrectly() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            final WalletClientException thrown = assertThrows(
                    WalletClientException.class,
                    () -> walletClientService.getUserWalletBalance(new UserId(103L))
            );
            assertEquals(thrown.code(), "BAD_GATEWAY");
            assertEquals(thrown.message(), "bad gateway");
        }
    }

    @Nested
    class CreateTransaction {
        @Test
        void shouldDeserializeResponseBodyCorrectly() {
            final UserId userId = new UserId(101L);
            final Money amount = Money.of(2000L);
            final String responseBody = """
                    {
                        "wallet_transaction_id": 66319,
                        "amount": -2000,
                        "user_id": 101
                    }
                    """.stripIndent();

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            final WalletTransaction response = walletClientService.createTransaction(userId, amount, WITHDRAWAL);

            assertEquals(WITHDRAWAL, response.operation());
            assertEquals(userId, response.userId());
            assertEquals(amount.negate(), response.amount());
            assertEquals(new WalletTransactionId(66319L), response.walletTransactionId());
        }

        @Test
        void shouldSerialiseResponseBodyCorrectly() throws JSONException {
            final UserId userId = new UserId(121L);
            final Money amount = Money.of(1500L);
            final String requestPath = "/wallets/transactions";
            final String expectedRequestBody = """
                    {
                        "amount": -1500,
                        "user_id": 121
                    }
                    """.stripIndent();
            final String responseBody = """
                    {
                        "wallet_transaction_id": 66310,
                        "amount": -1500,
                        "user_id": 121
                    }
                    """.stripIndent();
            final ArgumentCaptor<HttpEntity<String>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            walletClientService.createTransaction(userId, amount, WITHDRAWAL);
            verify(restTemplate).exchange(eq(requestPath), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class));

            final HttpEntity<String> httpEntity = httpEntityCaptor.getValue();

            JSONAssert.assertEquals(expectedRequestBody, httpEntity.getBody(), true);
            assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getAccept().get(0));
            assertEquals(MediaType.APPLICATION_JSON, httpEntity.getHeaders().getContentType());
        }
    }
}
