package com.example.currencyconversion.controller;

import com.example.currencyconversion.dto.ConversionResponse;
import com.example.currencyconversion.dto.RateResponse;
import com.example.currencyconversion.exception.ExternalApiException;
import com.example.currencyconversion.exception.InvalidCurrencyException;
import com.example.currencyconversion.service.CurrencyConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests du controller avec le service mocke : validation des codes HTTP
 * et du format de reponse (exigence 26 du cahier des charges).
 */
@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyConversionService conversionService;

    // ---- Requete valide -> 200 + corps complet ----
    @Test
    void shouldReturn200OnValidConversion() throws Exception {
        when(conversionService.convert(any())).thenReturn(ConversionResponse.of(
                "USD", "EUR", new BigDecimal("100"), new BigDecimal("0.8603"), new BigDecimal("86.03")));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"EUR\",\"amount\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.exchangeRate").value(0.8603))
                .andExpect(jsonPath("$.convertedAmount").value(86.03))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ---- amount = 0 -> 400 (validation Bean, service jamais appele) ----
    @Test
    void shouldReturn400OnZeroAmount() throws Exception {
        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"EUR\",\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        verify(conversionService, never()).convert(any());
    }

    // ---- amount negatif -> 400 ----
    @Test
    void shouldReturn400OnNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"EUR\",\"amount\":-100}"))
                .andExpect(status().isBadRequest());

        verify(conversionService, never()).convert(any());
    }

    // ---- devise absente -> 400 ----
    @Test
    void shouldReturn400OnMissingFrom() throws Exception {
        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"EUR\",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.from").exists());
    }

    // ---- devise malformee -> 400 ----
    @Test
    void shouldReturn400OnMalformedCurrency() throws Exception {
        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"US\",\"to\":\"EUR\",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.from").exists());
    }

    // ---- JSON malforme -> 400 propre ----
    @Test
    void shouldReturn400OnMalformedJson() throws Exception {
        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ---- devise non supportee par le fournisseur -> 400 ----
    @Test
    void shouldReturn400OnUnsupportedCurrency() throws Exception {
        when(conversionService.convert(any()))
                .thenThrow(new InvalidCurrencyException("Unsupported currency code: XXX"));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"XXX\",\"to\":\"EUR\",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    // ---- API externe indisponible -> 502, pas d'exposition de detail technique ----
    @Test
    void shouldReturn502OnExternalApiFailure() throws Exception {
        when(conversionService.convert(any()))
                .thenThrow(new ExternalApiException("Exchange rate provider is currently unavailable."));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"EUR\",\"amount\":100}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.message").value("Exchange rate provider is currently unavailable."))
                .andExpect(jsonPath("$.path").value("/api/currency/convert"));
    }

    // ---- erreur imprevue -> 500 sans stack trace ----
    @Test
    void shouldReturn500OnUnexpectedError() throws Exception {
        when(conversionService.convert(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"USD\",\"to\":\"EUR\",\"amount\":100}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }

    // ---- GET /rate valide -> 200 ----
    @Test
    void shouldReturn200OnRateEndpoint() throws Exception {
        when(conversionService.getRate("USD", "EUR"))
                .thenReturn(RateResponse.of("USD", "EUR", new BigDecimal("0.8603")));

        mockMvc.perform(get("/api/currency/rate").param("from", "USD").param("to", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value(0.8603));
    }
}
