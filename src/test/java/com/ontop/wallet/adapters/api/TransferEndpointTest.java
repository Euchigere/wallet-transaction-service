package com.ontop.wallet.adapters.api;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ontop.wallet.adapters.api.TransactionResponse.TransactionStatus.PROCESSING;
import static com.ontop.wallet.domain.enums.WalletTransactionOperation.WITHDRAWAL;
import static com.ontop.wallet.domain.service.ModelFactory.walletTransaction;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc
class TransferEndpointTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferEndpointService transferEndpointService;

    private final TransactionResponse transactionResponse =
            TransactionResponse.of(PROCESSING, walletTransaction(WITHDRAWAL));

    @Nested
    class CreateTransfer {
        private final String path = "/transfers";

        @Test
        void shouldSerialiseValidRequest() throws Exception {
            final String validRequest = "{\"userId\": 101, \"amount\": 200}";
            when(transferEndpointService.transfer(anyLong(), any(BigDecimal.class))).thenReturn(transactionResponse);

            mockMvc.perform(post(path)
                    .content(validRequest)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @TestFactory
        List<DynamicTest> shouldFailIfRequestIsInvalid() throws Exception {
            return Map.of(
                    "user id should not be null", "{\"amount\": 200}",
                    "user id should not be less than 1", "{\"userId\": 0, \"amount\": 200}",
                    "amount should not be null", "{\"userId\": 101}",
                    "amount should not be less than 1", "{\"userId\": 101, \"amount\": -50}"
            ).entrySet().stream()
                    .map(entry -> dynamicTest(entry.getKey(), () -> {
                        mockMvc.perform(post(path)
                                        .content(entry.getValue())
                                        .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                                .andExpect(jsonPath("$.message").value(entry.getKey()))
                                .andDo(print());
            })).collect(Collectors.toList());
        }

        @Test
        void shouldReturnExpectedResponse() throws Exception {
            final String validRequest = "{\"userId\": 101, \"amount\": 200}";
            when(transferEndpointService.transfer(anyLong(), any(BigDecimal.class))).thenReturn(transactionResponse);

            final MvcResult mvcResponse = mockMvc.perform(post(path)
                            .content(validRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andReturn();

            final String expectedResponse = """
                    {
                        "transactionId": %d,
                    	"userId": 101,
                    	"amount": -100,
                    	"operation": "WITHDRAWAL",
                    	"status": "PROCESSING"
                    }
                    """.stripIndent();
            JSONAssert.assertEquals(
                    String.format(expectedResponse, transactionResponse.transactionId()),
                    mvcResponse.getResponse().getContentAsString(),
                    false
            );
        }
    }
}
