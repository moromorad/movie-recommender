package com.moro.movie_recommender.dto;

import java.time.OffsetDateTime;

/**
 * Represents a linked Trakt account with its access token and metadata.
 * This is stored within a User object to associate Trakt credentials with a specific user.
 */
public class TraktAccount {
    private String accessToken;
    private String refreshToken;
    private OffsetDateTime linkedAt;
    private String traktUsername;
    private String traktUserId;

    public TraktAccount() {}

    public TraktAccount(String accessToken) {
        this.accessToken = accessToken;
        this.linkedAt = OffsetDateTime.now();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public OffsetDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(OffsetDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }

    public String getTraktUsername() {
        return traktUsername;
    }

    public void setTraktUsername(String traktUsername) {
        this.traktUsername = traktUsername;
    }

    public String getTraktUserId() {
        return traktUserId;
    }

    public void setTraktUserId(String traktUserId) {
        this.traktUserId = traktUserId;
    }

    /**
     * Checks if this Trakt account has a valid access token.
     * 
     * @return true if access token is present and not empty
     */
    public boolean hasValidToken() {
        return accessToken != null && !accessToken.trim().isEmpty();
    }
}
