package com.moro.movie_recommender.dto.trakt;

/**
 * Represents standard identifier fields included in Trakt payloads
 * for movies, such as Trakt, IMDb, and TMDB IDs.
 */
public class TraktIdsDTO {
    private Long trakt;
    private String imdb;
    private Long tmdb;
    private String slug;

    public Long getTrakt() { return trakt; }
    public void setTrakt(Long trakt) { this.trakt = trakt; }

    public String getImdb() { return imdb; }
    public void setImdb(String imdb) { this.imdb = imdb; }

    public Long getTmdb() { return tmdb; }
    public void setTmdb(Long tmdb) { this.tmdb = tmdb; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
