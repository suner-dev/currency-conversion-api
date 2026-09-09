package com.example.currencyconversion.controller;

import com.example.currencyconversion.dto.ConversionRequest;
import com.example.currencyconversion.dto.ConversionResponse;
import com.example.currencyconversion.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currency")
@Tag(name = "Currency Conversion", description = "Conversion de devises via des taux de change récupérés dynamiquement")
public class CurrencyController {

    private final CurrencyConversionService conversionService;

    public CurrencyController(CurrencyConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Operation(
            summary = "Convertir un montant",
            description = "Convertit un montant d'une devise source vers une devise cible. " +
                          "Les taux sont récupérés dynamiquement auprès d'un fournisseur externe — " +
                          "les valeurs ci-dessous sont des exemples de structure, pas des taux garantis."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversion effectuée",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConversionResponse.class),
                            examples = @ExampleObject(name = "USD → EUR",
                                    value = "{ \"from\": \"USD\", \"to\": \"EUR\", \"amount\": 100, " +
                                            "\"exchangeRate\": 0.8603, \"convertedAmount\": 86.03, \"timestamp\": \"2026-09-09T12:00:00Z\" }"))),
            @ApiResponse(responseCode = "400", description = "Requête invalide (devise ou montant invalide)"),
            @ApiResponse(responseCode = "502", description = "Fournisseur de taux externe indisponible"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur imprévue")
    })
    @PostMapping("/convert")
    public ResponseEntity<ConversionResponse> convert(@Valid @RequestBody ConversionRequest request) {
        return ResponseEntity.ok(conversionService.convert(request));
    }

    @Operation(
            summary = "Obtenir un taux de change",
            description = "Récupère uniquement le taux de change entre deux devises. " +
                          "Même devise : taux = 1 sans appel externe."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Taux récupéré"),
            @ApiResponse(responseCode = "400", description = "Devise invalide"),
            @ApiResponse(responseCode = "502", description = "Fournisseur de taux externe indisponible")
    })
    @GetMapping("/rate")
    public ResponseEntity<com.example.currencyconversion.dto.RateResponse> getRate(
            @RequestParam @Schema(description = "Devise source (3 lettres)", example = "USD") String from,
            @RequestParam @Schema(description = "Devise cible (3 lettres)", example = "EUR") String to) {
        return ResponseEntity.ok(conversionService.getRate(from, to));
    }
}