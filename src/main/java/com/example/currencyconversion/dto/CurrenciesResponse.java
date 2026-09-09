package com.example.currencyconversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Liste des devises supportées par le fournisseur de taux")
public class CurrenciesResponse {

    @Schema(description = "Nombre de devises disponibles", example = "166")
    private int count;

    @Schema(description = "Codes de devises triés alphabétiquement (ISO 4217)",
            example = "[\"AED\",\"ARS\",\"AUD\",\"CNY\",\"EUR\",\"GBP\",\"JPY\",\"USD\",\"XAF\",\"XOF\"]")
    private List<String> currencies;

    @Schema(description = "Horodatage de la récupération (ISO-8601)", example = "2026-09-09T12:00:00Z")
    private Instant timestamp;

    public static CurrenciesResponse of(List<String> currencies) {
        CurrenciesResponse response = new CurrenciesResponse();
        response.currencies = currencies;
        response.count = currencies.size();
        response.timestamp = Instant.now();
        return response;
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public List<String> getCurrencies() { return currencies; }
    public void setCurrencies(List<String> currencies) { this.currencies = currencies; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
