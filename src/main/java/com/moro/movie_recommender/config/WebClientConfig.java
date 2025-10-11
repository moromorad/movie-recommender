package com.moro.movie_recommender.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides shared WebClient configuration for performing HTTP requests
 * to external services (e.g., Trakt API). Exposes a {@link WebClient.Builder}
 * bean so services can customize base URL and headers while sharing defaults.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Increase buffer size a bit for safety when dealing with larger payloads
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        return WebClient.builder().exchangeStrategies(strategies);
    }
}
