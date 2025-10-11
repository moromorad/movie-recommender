package com.moro.movie_recommender.dto.trakt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * A single entry from Trakt's "watched" list for movies.
 * Contains play count, last-watched timestamp, and the referenced movie.
 */
public class TraktWatchedItemDTO {
    @JsonProperty("plays")
    private Integer plays;

    @JsonProperty("last_watched_at")
    private OffsetDateTime lastWatchedAt;

    @JsonProperty("movie")
    private TraktMovieDTO movie;

    public Integer getPlays() { return plays; }
    public void setPlays(Integer plays) { this.plays = plays; }

    public OffsetDateTime getLastWatchedAt() { return lastWatchedAt; }
    public void setLastWatchedAt(OffsetDateTime lastWatchedAt) { this.lastWatchedAt = lastWatchedAt; }

    public TraktMovieDTO getMovie() { return movie; }
    public void setMovie(TraktMovieDTO movie) { this.movie = movie; }
}
