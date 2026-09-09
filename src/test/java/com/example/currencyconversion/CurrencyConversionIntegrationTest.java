package com.example.currencyconversion;

import com.example.currencyconversion.client.ExchangeRateClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration de bout en bout : POST -> Controller -> Service ->
 * ExchangeRateClient (mock) -> Conversion -> Response. Aucune dépendance Internet.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CurrencyConversionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateClient exchangeRateClient;

    @Test
    void shouldConvertEndToEnd() throws Exception {
        when(exchangeRateClient.getExchangeRate("USD", "EUR")).thenReturn(new BigDecimal("0.92345678"));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType("application/json")
                        .content("{\"from\":\"usd\",\"to\":\"eur\",\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.exchangeRate").value(0.9235))
                .andExpect(jsonPath("$.convertedAmount").value(92.35))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldConvertSameCurrencyWithoutExternalCall() throws Exception {
        mockMvc.perform(post("/api/currency/convert")
                        .contentType("application/json")
                        .content("{\"from\":\"USD\",\"to\":\"USD\",\"amount\":55.555}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value(1))
                .andExpect(jsonPath("$.convertedAmount").value(55.56));

        Mockito.verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void shouldConvertUsdToCentralAfricanCfaEndToEnd() throws Exception {
        when(exchangeRateClient.getExchangeRate("USD", "XAF")).thenReturn(new BigDecimal("564.291557"));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType("application/json")
                        .content("{\"from\":\"USD\",\"to\":\"XAF\",\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("XAF"))
                .andExpect(jsonPath("$.exchangeRate").value(564.2916))
                .andExpect(jsonPath("$.convertedAmount").value(56429.16));
    }

    @Test
    void shouldConvertWestAfricanCfaToChineseYuanEndToEnd() throws Exception {
        when(exchangeRateClient.getExchangeRate("XOF", "CNY")).thenReturn(new BigDecimal("0.01192"));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType("application/json")
                        .content("{\"from\":\"xof\",\"to\":\"cny\",\"amount\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("XOF"))
                .andExpect(jsonPath("$.to").value("CNY"))
                .andExpect(jsonPath("$.convertedAmount").value(119.20));
    }

    @Test
    void shouldExposeOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/currency/convert']").exists())
                .andExpect(jsonPath("$.paths['/api/currency/rate']").exists());
    }
}
