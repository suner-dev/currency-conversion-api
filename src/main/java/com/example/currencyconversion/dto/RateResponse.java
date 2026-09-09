package com.example.currencyconversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Taux de change entre deux devises")
public class RateResponse {

    @Schema(description = "Devise source", example = "USD")
    private String from;

    @Schema(description = "Devise cible", example = "EUR")
    private String to;

    @Schema(description = "Taux de change (4 décimales)", example = "0.8603")
    private BigDecimal exchangeRate;

    @Schema(description = "Horodatage (ISO-8601)", example = "2026-09-09T12:00:00Z")
    private Instant timestamp;

    public static RateResponse of(String from, String to, BigDecimal exchangeRate) {
        RateResponse response = new RateResponse();
        response.from = from;
        response.to = to;
        response.exchangeRate = exchangeRate;
        response.timestamp = Instant.now();
        return response;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}