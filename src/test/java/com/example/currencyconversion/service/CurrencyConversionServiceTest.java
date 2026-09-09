package com.example.currencyconversion.service;

import com.example.currencyconversion.client.ExchangeRateClient;
import com.example.currencyconversion.dto.ConversionRequest;
import com.example.currencyconversion.dto.ConversionResponse;
import com.example.currencyconversion.dto.RateResponse;
import com.example.currencyconversion.exception.ExternalApiException;
import com.example.currencyconversion.exception.InvalidAmountException;
import com.example.currencyconversion.exception.InvalidCurrencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CurrencyConversionServiceTest {

    private ExchangeRateClient client;
    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        client = mock(ExchangeRateClient.class);
        service = new CurrencyConversionService(client);
    }

    // ---- Cas 1 : USD -> EUR, amount = 100, taux = 0.92 => 92.00 ----
    @Test
    void shouldConvertUsdToEur() {
        when(client.getExchangeRate("USD", "EUR")).thenReturn(new BigDecimal("0.92"));

        ConversionResponse response = service.convert(req("USD", "EUR", "100"));

        assertEquals("USD", response.getFrom());
        assertEquals("EUR", response.getTo());
        assertEquals(0, new BigDecimal("0.9200").compareTo(response.getExchangeRate()));
        assertEquals(0, new BigDecimal("92.00").compareTo(response.getConvertedAmount()));
        verify(client).getExchangeRate("USD", "EUR");
    }

    // ---- Cas 2 : EUR -> USD, taux contrôlé ----
    @Test
    void shouldConvertEurToUsd() {
        when(client.getExchangeRate("EUR", "USD")).thenReturn(new BigDecimal("1.0837"));

        ConversionResponse response = service.convert(req("EUR", "USD", "250"));

        assertEquals(0, new BigDecimal("1.0837").compareTo(response.getExchangeRate()));
        assertEquals(0, new BigDecimal("270.93").compareTo(response.getConvertedAmount()));
    }

    // ---- Cas 3 : même devise -> taux = 1, aucun appel externe ----
    @Test
    void shouldReturnRateOneForSameCurrencyWithoutExternalCall() {
        ConversionResponse response = service.convert(req("USD", "USD", "100"));
        assertEquals(0, BigDecimal.ONE.compareTo(response.getExchangeRate()));
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getConvertedAmount()));
        verifyNoInteractions(client);
    }

    @Test
    void shouldReturnRateOneForSameCurrencyOnRateEndpointWithoutExternalCall() {
        RateResponse response = service.getRate("EUR", "EUR");
        assertEquals(0, BigDecimal.ONE.compareTo(response.getExchangeRate()));
        verifyNoInteractions(client);
    }

    // ---- Cas 4 : montant zéro -> erreur ----
    @Test
    void shouldRejectZeroAmount() {
        assertThrows(InvalidAmountException.class, () -> service.convert(req("USD", "EUR", "0")));
        verifyNoInteractions(client);
    }

    // ---- Cas 5 : montant négatif -> erreur ----
    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(InvalidAmountException.class, () -> service.convert(req("USD", "EUR", "-100")));
        verifyNoInteractions(client);
    }

    // ---- Cas 6 : devise invalide -> erreur propre ----
    @Test
    void shouldRejectMalformedCurrency() {
        assertThrows(InvalidCurrencyException.class, () -> service.convert(req("US", "EUR", "100")));
        assertThrows(InvalidCurrencyException.class, () -> service.convert(req("USDD", "EUR", "100")));
        assertThrows(InvalidCurrencyException.class, () -> service.convert(req("", "EUR", "100")));
        verifyNoInteractions(client);
    }

    // ---- Normalisation : usd / UsD -> USD ----
    @Test
    void shouldNormalizeCurrencyCodes() {
        when(client.getExchangeRate("USD", "EUR")).thenReturn(new BigDecimal("0.92"));
        service.convert(req("usd", "EUR", "100"));
        verify(client).getExchangeRate("USD", "EUR");
    }

    // ---- Cas 7 : API externe indisponible -> ExternalApiException propagée ----
    @Test
    void shouldPropagateExternalApiFailure() {
        when(client.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalApiException("Exchange rate provider is currently unavailable."));
        assertThrows(ExternalApiException.class, () -> service.convert(req("USD", "EUR", "100")));
    }

    // ---- Cas 8 : timeout externe -> ExternalApiException ----
    @Test
    void shouldPropagateTimeoutFailure() {
        when(client.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalApiException("Exchange rate provider is unavailable or timed out."));
        assertThrows(ExternalApiException.class, () -> service.convert(req("USD", "EUR", "100")));
    }

    // ---- Cas 9 : réponse externe invalide -> ExternalApiException ----
    @Test
    void shouldPropagateInvalidExternalResponse() {
        when(client.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalApiException("Invalid provider response (no rates received)."));
        assertThrows(ExternalApiException.class, () -> service.convert(req("USD", "EUR", "100")));
    }

    // ---- Devise inconnue du fournisseur -> InvalidCurrencyException ----
    @Test
    void shouldPropagateUnsupportedCurrency() {
        when(client.getExchangeRate("XXX", "EUR"))
                .thenThrow(new InvalidCurrencyException("Unsupported currency code: XXX"));
        assertThrows(InvalidCurrencyException.class, () -> service.convert(req("XXX", "EUR", "100")));
    }

    // ---- Devises africaines et chinoise (XAF, XOF, CNY) ----
    @Test
    void shouldConvertUsdToCentralAfricanCfa() {
        when(client.getExchangeRate("USD", "XAF")).thenReturn(new BigDecimal("564.291557"));

        ConversionResponse response = service.convert(req("USD", "XAF", "100"));

        assertEquals("XAF", response.getTo());
        assertEquals(0, new BigDecimal("564.2916").compareTo(response.getExchangeRate()));
        assertEquals(0, new BigDecimal("56429.16").compareTo(response.getConvertedAmount()));
    }

    @Test
    void shouldConvertWestAfricanCfaToChineseYuan() {
        when(client.getExchangeRate("XOF", "CNY")).thenReturn(new BigDecimal("0.011920"));

        ConversionResponse response = service.convert(req("XOF", "CNY", "10000"));

        assertEquals("CNY", response.getTo());
        assertEquals(0, new BigDecimal("119.20").compareTo(response.getConvertedAmount()));
    }

    @Test
    void shouldConvertUsdToJapaneseYen() {
        when(client.getExchangeRate("USD", "JPY")).thenReturn(new BigDecimal("155.1278"));

        ConversionResponse response = service.convert(req("USD", "JPY", "1000"));

        assertEquals("JPY", response.getTo());
        assertEquals(0, new BigDecimal("155127.80").compareTo(response.getConvertedAmount()));
    }

    @Test
    void shouldConvertEuroToBritishPound() {
        when(client.getExchangeRate("EUR", "GBP")).thenReturn(new BigDecimal("0.843512"));

        ConversionResponse response = service.convert(req("EUR", "GBP", "250"));

        assertEquals("GBP", response.getTo());
        assertEquals(0, new BigDecimal("210.88").compareTo(response.getConvertedAmount()));
    }

    private ConversionRequest req(String from, String to, String amount) {
        return new ConversionRequest(from, to, new BigDecimal(amount));
    }
}