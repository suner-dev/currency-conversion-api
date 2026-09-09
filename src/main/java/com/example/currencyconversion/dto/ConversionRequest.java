package com.example.currencyconversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(description = "Requête de conversion de devise")
public class ConversionRequest {

    @NotBlank(message = "Source currency is required")
    @Pattern(regexp = "[A-Za-z]{3}", message = "Source currency must be a 3-letter ISO code (e.g. USD, EUR)")
    @Schema(description = "Devise source (code ISO 4217 à 3 lettres)", example = "USD")
    private String from;

    @NotBlank(message = "Target currency is required")
    @Pattern(regexp = "[A-Za-z]{3}", message = "Target currency must be a 3-letter ISO code (e.g. USD, EUR)")
    @Schema(description = "Devise cible (code ISO 4217 à 3 lettres)", example = "EUR")
    private String to;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Schema(description = "Montant à convertir (strictement positif)", example = "100")
    private BigDecimal amount;

    public ConversionRequest() {}

    public ConversionRequest(String from, String to, BigDecimal amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}