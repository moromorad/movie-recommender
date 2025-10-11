package com.moro.movie_recommender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds Trakt-related configuration properties from {@code application.properties}
 * using the {@code trakt.*} prefix.
 *
 * <p>Expected keys include:
 * <ul>
 *   <li>{@code trakt.client-id}</li>
 *   <li>{@code trakt.client-secret}</li>
 *   <li>{@code trakt.redirect-uri}</li>
 *   <li>{@code trakt.api-base}</li>
 *   <li>{@code trakt.web-base}</li>
 *   <li>{@code trakt.api-version}</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "trakt")
public class TraktProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String apiBase;
    private String webBase;
    private String apiVersion;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getApiBase() { return apiBase; }
    public void setApiBase(String apiBase) { this.apiBase = apiBase; }

    public String getWebBase() { return webBase; }
    public void setWebBase(String webBase) { this.webBase = webBase; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
}
