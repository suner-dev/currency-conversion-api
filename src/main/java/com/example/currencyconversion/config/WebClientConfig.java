package com.example.currencyconversion.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient exchangeRateWebClient(
            @Value("${exchange-rate.base-url}") String baseUrl,
            @Value("${exchange-rate.timeout}") long timeoutMillis,
            @Value("${exchange-rate.connect-timeout}") long connectTimeoutMillis) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeoutMillis))
                .responseTimeout(Duration.ofMillis(timeoutMillis));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}