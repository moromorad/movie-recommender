package com.moro.movie_recommender.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Permissive WebFlux security configuration for development.
 *
 * <p>Allows access to the homepage, static assets, Trakt OAuth endpoints,
 * and API routes, avoiding the default Spring Security login page.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/", "/index.html",
                                "/auth/trakt/**",
                                "/login/oauth2/**",
                                "/api/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"
                        ).permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }
}

