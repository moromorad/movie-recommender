package com.moro.movie_recommender.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user with a name and separate lists for watched movies.
 * Users can optionally have a linked Trakt account for automatic movie syncing,
 * or they can manually add movies without any Trakt integration.
 * 
 * Movies are stored in separate lists to prevent manual movies from being lost during Trakt syncs.
 */
public class User {
    private String name;
    private List<Movie> manualMovies; // Manually added movies (preserved during syncs)
    private List<Movie> traktMovies; // Trakt-synced movies (updated during syncs)
    private TraktAccount traktAccount; // Optional - null if user doesn't have Trakt linked

    public User(String name) {
        this.name = name;
        this.manualMovies = new ArrayList<>();
        this.traktMovies = new ArrayList<>();
        this.traktAccount = null; // No Trakt account by default
    }

    public User(String name, List<Movie> manualMovies, List<Movie> traktMovies) {
        this.name = name;
        this.manualMovies = manualMovies != null ? new ArrayList<>(manualMovies) : new ArrayList<>();
        this.traktMovies = traktMovies != null ? new ArrayList<>(traktMovies) : new ArrayList<>();
        this.traktAccount = null; // No Trakt account by default
    }

    public User(String name, List<Movie> manualMovies, List<Movie> traktMovies, TraktAccount traktAccount) {
        this.name = name;
        this.manualMovies = manualMovies != null ? new ArrayList<>(manualMovies) : new ArrayList<>();
        this.traktMovies = traktMovies != null ? new ArrayList<>(traktMovies) : new ArrayList<>();
        this.traktAccount = traktAccount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Movie> getManualMovies() {
        return manualMovies;
    }

    public void setManualMovies(List<Movie> manualMovies) {
        this.manualMovies = manualMovies != null ? new ArrayList<>(manualMovies) : new ArrayList<>();
    }

    public List<Movie> getTraktMovies() {
        return traktMovies;
    }

    public void setTraktMovies(List<Movie> traktMovies) {
        this.traktMovies = traktMovies != null ? new ArrayList<>(traktMovies) : new ArrayList<>();
    }

    /**
     * Gets all watched movies (both manual and Trakt).
     * 
     * @return combined list of all movies
     */
    public List<Movie> getAllWatchedMovies() {
        List<Movie> allMovies = new ArrayList<>();
        allMovies.addAll(manualMovies);
        allMovies.addAll(traktMovies);
        return allMovies;
    }

    public void addManualMovie(Movie movie) {
        if (movie != null) {
            this.manualMovies.add(movie);
        }
    }

    public boolean removeManualMovie(Movie movie) {
        return this.manualMovies.remove(movie);
    }

    public TraktAccount getTraktAccount() {
        return traktAccount;
    }

    public void setTraktAccount(TraktAccount traktAccount) {
        this.traktAccount = traktAccount;
    }

    /**
     * Checks if this user has a linked Trakt account.
     * 
     * @return true if user has a Trakt account with valid token
     */
    public boolean hasTraktAccount() {
        return traktAccount != null && traktAccount.hasValidToken();
    }

    /**
     * Links a Trakt account to this user.
     * 
     * @param accessToken the Trakt access token
     * @param refreshToken the Trakt refresh token (optional)
     */
    public void linkTraktAccount(String accessToken, String refreshToken) {
        this.traktAccount = new TraktAccount(accessToken);
        if (refreshToken != null) {
            this.traktAccount.setRefreshToken(refreshToken);
        }
    }

    /**
     * Unlinks the Trakt account from this user.
     */
    public void unlinkTraktAccount() {
        this.traktAccount = null;
    }

    /**
     * Gets the Trakt access token if available.
     * 
     * @return the access token or null if no Trakt account
     */
    public String getTraktAccessToken() {
        return hasTraktAccount() ? traktAccount.getAccessToken() : null;
    }
}
