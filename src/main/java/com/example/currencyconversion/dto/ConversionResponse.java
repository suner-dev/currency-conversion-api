package com.example.currencyconversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Réponse de conversion de devise")
public class ConversionResponse {

    @Schema(description = "Devise source", example = "USD")
    private String from;

    @Schema(description = "Devise cible", example = "EUR")
    private String to;

    @Schema(description = "Montant converti (2 décimales)", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Taux de change appliqué (4 décimales)", example = "0.8603")
    private BigDecimal exchangeRate;

    @Schema(description = "Montant converti (2 décimales)", example = "86.03")
    private BigDecimal convertedAmount;

    @Schema(description = "Horodatage de la conversion (ISO-8601)", example = "2026-09-09T12:00:00Z")
    private Instant timestamp;

    public static ConversionResponse of(String from, String to, BigDecimal amount,
                                        BigDecimal exchangeRate, BigDecimal convertedAmount) {
        ConversionResponse response = new ConversionResponse();
        response.from = from;
        response.to = to;
        response.amount = amount;
        response.exchangeRate = exchangeRate;
        response.convertedAmount = convertedAmount;
        response.timestamp = Instant.now();
        return response;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public void setConvertedAmount(BigDecimal convertedAmount) { this.convertedAmount = convertedAmount; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}