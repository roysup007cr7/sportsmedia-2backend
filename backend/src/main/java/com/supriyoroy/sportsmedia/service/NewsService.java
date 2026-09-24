package com.supriyoroy.sportsmedia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    @Value("${news.api.enabled:true}")
    private boolean enabled;

    @Value("${news.api.token:6e7328cae4b140eab27bfb44326b9a10}")
    private String token;

    @Value("${news.api.base-url:https://newsapi.org/v2}")
    private String baseUrl;

    @Value("${news.api.query:football}")
    private String query;

    public String getLatestSportsNews() {
        if (!enabled || token == null || token.isBlank()) {
            return "{\"articles\": []}";
        }

        try {
            RestClient client = RestClient.builder().baseUrl(baseUrl).build();
            return client.get()
                    .uri("/everything?q={query}&sortBy=publishedAt&pageSize=10&apiKey={token}", query, token)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to fetch news from NewsAPI: {}", e.getMessage());
            return "{\"articles\": []}";
        }
    }
}