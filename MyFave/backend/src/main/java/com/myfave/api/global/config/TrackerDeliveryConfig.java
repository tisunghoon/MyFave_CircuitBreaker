package com.myfave.api.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TrackerDeliveryConfig {

    @Value("${tracker.api-url}")
    private String apiUrl;

    @Value("${tracker.client-id}")
    private String clientId;

    @Value("${tracker.client-secret}")
    private String clientSecret;

    @Bean
    public WebClient trackerWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "TRACKQL-API-KEY " + clientId + ":" + clientSecret)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
