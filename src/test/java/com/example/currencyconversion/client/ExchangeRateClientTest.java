package com.example.currencyconversion.client;

import com.example.currencyconversion.exception.ExternalApiException;
import com.example.currencyconversion.exception.InvalidCurrencyException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du client externe avec MockWebServer : aucun appel reseau reel.
 * Couvre le HTTP 200, les erreurs HTTP du fournisseur, les reponses malformees,
 * le timeout et la connexion refusee (exigence 28 du cahier des charges).
 */
class ExchangeRateClientTest {

    private MockWebServer server;
    private ExchangeRateClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(500));
        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/v6").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        client = new ExchangeRateClient(webClient, "test-api-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ---- HTTP 200 : reponse valide -> taux extrait ----
    @Test
    void shouldParseSuccessfulResponse() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"success\",\"base_code\":\"USD\",\"rates\":{\"EUR\":0.860312,\"USD\":1.0}}"));

        BigDecimal rate = client.getExchangeRate("USD", "EUR");

        assertEquals(0, new BigDecimal("0.860312").compareTo(rate));

        // Verifie la requete construite : chemin + cle API en parametre
        var recorded = server.takeRequest();
        assertEquals("/v6/latest/USD?access_key=test-api-key", recorded.getPath());
    }

    // ---- HTTP 200 sans cle API configuree : pas de access_key dans l'URL ----
    @Test
    void shouldOmitAccessKeyWhenNotConfigured() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"success\",\"base_code\":\"USD\",\"rates\":{\"EUR\":0.9}}"));

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/v6").toString())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .build();
        new ExchangeRateClient(webClient, "").getExchangeRate("USD", "EUR");

        assertEquals("/v6/latest/USD", server.takeRequest().getPath());
    }

    // ---- HTTP 200 mais corps JSON malforme -> ExternalApiException ----
    @Test
    void shouldFailOnMalformedSuccessfulBody() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{ not-valid-json "));

        ExternalApiException ex = assertThrows(ExternalApiException.class,
                () -> client.getExchangeRate("USD", "EUR"));
        assertEquals("Exchange rate provider is currently unavailable.", ex.getMessage());
    }

    // ---- HTTP 200 avec result=success mais sans rates -> ExternalApiException ----
    @Test
    void shouldFailWhenRatesMissing() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"success\",\"base_code\":\"USD\"}"));

        ExternalApiException ex = assertThrows(ExternalApiException.class,
                () -> client.getExchangeRate("USD", "EUR"));
        assertEquals("Invalid provider response (no rates received).", ex.getMessage());
    }

    // ---- Devise cible absente des taux -> InvalidCurrencyException ----
    @Test
    void shouldFailWhenTargetCurrencyMissingFromRates() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"success\",\"base_code\":\"USD\",\"rates\":{\"EUR\":0.9}}"));

        assertThrows(InvalidCurrencyException.class, () -> client.getExchangeRate("USD", "ZZZ"));
    }

    // ---- HTTP 401 : probleme de cle API ----
    @Test
    void shouldTranslateUnauthorized() {
        server.enqueue(new MockResponse().setResponseCode(401));

        ExternalApiException ex = assertThrows(ExternalApiException.class,
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(ex.getMessage().contains("authentication"));
    }

    // ---- HTTP 404 avec unsupported-code -> InvalidCurrencyException ----
    @Test
    void shouldTranslateUnsupportedCodeError() {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"error\",\"error-type\":\"unsupported-code\"}"));

        assertThrows(InvalidCurrencyException.class, () -> client.getExchangeRate("XXX", "EUR"));
    }

    // ---- HTTP 429 : limite de requetes ----
    @Test
    void shouldTranslateRateLimit() {
        server.enqueue(new MockResponse().setResponseCode(429));

        ExternalApiException ex = assertThrows(ExternalApiException.class,
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(ex.getMessage().contains("rate limit"));
    }

    // ---- HTTP 500 : probleme fournisseur ----
    @Test
    void shouldTranslateProviderServerError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        ExternalApiException ex = assertThrows(ExternalApiException.class,
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(ex.getMessage().contains("500"));
    }

    // ---- Liste des devises supportées : extraction des codes des taux ----
    @Test
    void shouldParseSupportedCurrencyCodes() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"success\",\"base_code\":\"USD\","
                        + "\"rates\":{\"EUR\":0.86,\"USD\":1.0,\"XAF\":564.29,\"XOF\":564.29,\"CNY\":6.72}}"));

        SortedSet<String> codes = client.getSupportedCurrencyCodes();

        assertEquals(new TreeSet<>(java.util.List.of("CNY", "EUR", "USD", "XAF", "XOF")), codes);

        // La liste est construite avec USD comme devise de base
        assertEquals("/v6/latest/USD?access_key=test-api-key", server.takeRequest().getPath());
    }

    // ---- Liste des devises : erreur du fournisseur -> ExternalApiException ----
    @Test
    void shouldFailListingOnProviderError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThrows(ExternalApiException.class, () -> client.getSupportedCurrencyCodes());
    }

    // ---- Liste des devises : réponse sans taux -> ExternalApiException ----
    @Test
    void shouldFailListingWhenRatesMissing() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\":\"success\",\"base_code\":\"USD\"}"));

        assertThrows(ExternalApiException.class, () -> client.getSupportedCurrencyCodes());
    }

    // ---- Timeout : le serveur n'envoie rien -> ExternalApiException ----
    @Test
    void shouldTranslateTimeout() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        assertThrows(ExternalApiException.class, () -> client.getExchangeRate("USD", "EUR"));
    }

    // ---- Connexion refusee : serveur eteint -> ExternalApiException ----
    @Test
    void shouldTranslateConnectionFailure() throws IOException {
        server.shutdown();

        assertThrows(ExternalApiException.class, () -> client.getExchangeRate("USD", "EUR"));
    }
}
