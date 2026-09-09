package com.example.currencyconversion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Currency Conversion API")
                        .version("1.0.0")
                        .description("""
                                API REST de conversion de devises.
                                Les taux de change sont récupérés **dynamiquement** auprès d'un fournisseur externe
                                (open.er-api.com) via Spring WebClient — aucune donnée de taux n'est stockée localement.
                                """)
                        .contact(new Contact().name("API Support").email("support@example.com"))
                        .license(new License().name("MIT")));
    }
}