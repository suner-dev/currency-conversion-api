package com.example.currencyconversion.service;

import com.example.currencyconversion.client.ExchangeRateClient;
import com.example.currencyconversion.dto.ConversionRequest;
import com.example.currencyconversion.dto.ConversionResponse;
import com.example.currencyconversion.dto.CurrenciesResponse;
import com.example.currencyconversion.dto.RateResponse;
import com.example.currencyconversion.exception.InvalidAmountException;
import com.example.currencyconversion.exception.InvalidCurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.SortedSet;

@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final int RATE_SCALE = 4;
    private static final int AMOUNT_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final String CURRENCY_CODE_PATTERN = "[A-Z]{3}";

    private final ExchangeRateClient exchangeRateClient;

    public CurrencyConversionService(ExchangeRateClient exchangeRateClient) {
        this.exchangeRateClient = exchangeRateClient;
    }

    /**
     * Convertit un montant d'une devise vers une autre.
     * Arrondis : taux à 4 décimales, montant converti à 2 décimales (HALF_UP).
     */
    public ConversionResponse convert(ConversionRequest request) {
        String from = normalizeCurrency(request.getFrom());
        String to = normalizeCurrency(request.getTo());
        BigDecimal amount = request.getAmount();

        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than 0");
        }

        BigDecimal exchangeRate = getExchangeRate(from, to);
        BigDecimal convertedAmount = amount.multiply(exchangeRate).setScale(AMOUNT_SCALE, ROUNDING);
        BigDecimal displayRate = exchangeRate.setScale(RATE_SCALE, ROUNDING);

        return ConversionResponse.of(from, to, amount, displayRate, convertedAmount);
    }

    /**
     * Liste les devises supportées par le fournisseur, triées alphabétiquement.
     * Source de vérité : le fournisseur externe (aucune liste codée en dur).
     */
    public CurrenciesResponse listSupportedCurrencies() {
        log.info("Récupération de la liste des devises supportées auprès du fournisseur");
        SortedSet<String> codes = exchangeRateClient.getSupportedCurrencyCodes();
        log.info("{} devises disponibles", codes.size());
        return CurrenciesResponse.of(List.copyOf(codes));
    }

    /**
     * Récupère le taux pour seule. Optimisation documentée : si from == to, retourne 1
     * sans appel externe.
     */
    public RateResponse getRate(String from, String to) {
        String source = normalizeCurrency(from);
        String target = normalizeCurrency(to);
        BigDecimal exchangeRate = getExchangeRate(source, target);
        return RateResponse.of(source, target, exchangeRate.setScale(RATE_SCALE, ROUNDING));
    }

    private BigDecimal getExchangeRate(String from, String to) {
        if (from.equals(to)) {
            // Optimisation documentée : même devise -> taux = 1, aucun appel externe.
            log.info("Conversion {} -> {} : même devise, taux = 1", from, to);
            return BigDecimal.ONE;
        }
        log.info("Appel du fournisseur de taux pour {} -> {}", from, to);
        BigDecimal rate = exchangeRateClient.getExchangeRate(from, to);
        log.info("Taux reçu {} -> {} : {}", from, to, rate);
        return rate;
    }

    /**
     * Normalise un code devise : trim + majuscules + contrôle format ISO 4217 (3 lettres).
     */
    private String normalizeCurrency(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidCurrencyException("Currency code is required");
        }
        String normalized = code.trim().toUpperCase();
        if (!normalized.matches(CURRENCY_CODE_PATTERN)) {
            throw new InvalidCurrencyException("Invalid currency code: " + code);
        }
        return normalized;
    }
}