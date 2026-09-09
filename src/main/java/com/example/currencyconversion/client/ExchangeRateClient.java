package com.example.currencyconversion.client;

import com.example.currencyconversion.exception.ExternalApiException;
import com.example.currencyconversion.exception.InvalidCurrencyException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
public class ExchangeRateClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateClient.class);
    /**
     * Devise de base utilisée uniquement pour récupérer la liste des devises supportées :
     * la réponse du fournisseur contient un taux vers chaque devise disponible.
     */
    private static final String LIST_BASE_CURRENCY = "USD";

    private final WebClient webClient;
    private final String apiKey;

    public ExchangeRateClient(@Qualifier("exchangeRateWebClient") WebClient webClient,
                              @Value("${exchange-rate.api-key:}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    /**
     * Liste les codes de devises supportés par le fournisseur, triés alphabétiquement.
     * La liste provient intégralement du fournisseur : aucune devise n'est codée en dur.
     */
    public SortedSet<String> getSupportedCurrencyCodes() {
        ProviderResponse response;
        try {
            response = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/latest/{base}");
                        if (StringUtils.hasText(apiKey)) {
                            uriBuilder.queryParam("access_key", apiKey);
                        }
                        return uriBuilder.build(LIST_BASE_CURRENCY);
                    })
                    .exchangeToMono(this::handleResponse)
                    .block();
        } catch (InvalidCurrencyException | ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            // NB : jamais de message d'exception brut dans les logs (peut contenir la clé API).
            log.error("Échec de la récupération de la liste des devises : {}", e.getClass().getSimpleName());
            throw new ExternalApiException("Exchange rate provider is currently unavailable.", e);
        }

        if (response == null || response.getRates() == null || response.getRates().isEmpty()) {
            throw new ExternalApiException("Invalid provider response (no rates received).");
        }
        return new TreeSet<>(response.getRates().keySet());
    }

    /**
     * Récupère le taux de change entre {@code from} et {@code to} auprès du fournisseur externe.
     * Les codes doivent être déjà normalisés (majuscules, 3 lettres).
     */
    public BigDecimal getExchangeRate(String from, String to) {
        ProviderResponse response;
        try {
            response = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/latest/{base}");
                        if (StringUtils.hasText(apiKey)) {
                            uriBuilder.queryParam("access_key", apiKey);
                        }
                        return uriBuilder.build(from);
                    })
                    .exchangeToMono(this::handleResponse)
                    .block();
        } catch (InvalidCurrencyException | ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            // NB : on ne log jamais le message de l'exception : il peut contenir l'URL
            // complète de la requête, donc la clé API (access_key). Cf. exigence logging.
            log.error("Échec de l'appel au fournisseur de taux (from={}, to={}) : {}", from, to, e.getClass().getSimpleName());
            throw new ExternalApiException("Exchange rate provider is currently unavailable.", e);
        }

        return extractRate(response, from, to);
    }

    private Mono<ProviderResponse> handleResponse(ClientResponse clientResponse) {
        HttpStatusCode status = clientResponse.statusCode();
        if (status.is2xxSuccessful()) {
            return clientResponse.bodyToMono(ProviderResponse.class);
        }
        // Les erreurs du fournisseur peuvent contenir un corps JSON exploitable
        // (ex. {"result":"error","error-type":"unsupported-code"}). On tente de le lire.
        return clientResponse.bodyToMono(ProviderResponse.class)
                .onErrorResume(parseFailure -> Mono.empty())
                .defaultIfEmpty(new ProviderResponse())
                .map(body -> {
                    RuntimeException exception = toException(status, body);
                    log.warn("Réponse d'erreur du fournisseur : HTTP {}", status.value());
                    throw exception;
                });
    }

    private RuntimeException toException(HttpStatusCode status, ProviderResponse body) {
        if (body != null && "error".equals(body.getResult())) {
            if ("unsupported-code".equals(body.getErrorType())) {
                return new InvalidCurrencyException("Unsupported currency code (provider reported unsupported-code).");
            }
            return new ExternalApiException("Provider error: " + body.getErrorType());
        }
        return switch (status.value()) {
            case 401, 403 -> new ExternalApiException("Provider authentication failed. Check the configured API key.");
            case 404 -> new ExternalApiException("Provider endpoint not found.");
            case 429 -> new ExternalApiException("Provider rate limit exceeded.");
            default -> new ExternalApiException("Provider server error (HTTP " + status.value() + ").");
        };
    }

    private BigDecimal extractRate(ProviderResponse response, String from, String to) {
        if (response == null) {
            throw new ExternalApiException("Empty or invalid response from the rate provider.");
        }
        if ("error".equals(response.getResult())) {
            if ("unsupported-code".equals(response.getErrorType())) {
                throw new InvalidCurrencyException("Unsupported currency code: " + from);
            }
            throw new ExternalApiException("Provider error: " + response.getErrorType());
        }
        if (response.getRates() == null) {
            throw new ExternalApiException("Invalid provider response (no rates received).");
        }
        BigDecimal rate = response.getRates().get(to);
        if (rate == null) {
            throw new InvalidCurrencyException("Unsupported currency code: " + to);
        }
        return rate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProviderResponse {
        private String result;
        @JsonProperty("error-type")
        private String errorType;
        @JsonProperty("base_code")
        private String baseCode;
        private Map<String, BigDecimal> rates;

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }

        public String getBaseCode() { return baseCode; }
        public void setBaseCode(String baseCode) { this.baseCode = baseCode; }

        public Map<String, BigDecimal> getRates() { return rates; }
        public void setRates(Map<String, BigDecimal> rates) { this.rates = rates; }
    }
}