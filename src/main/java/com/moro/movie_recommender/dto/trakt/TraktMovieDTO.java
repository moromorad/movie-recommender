package com.moro.movie_recommender.dto.trakt;

/**
 * Minimal movie representation as returned by Trakt API in many endpoints.
 * Includes human-readable fields and a nested {@link TraktIdsDTO} block.
 */
public class TraktMovieDTO {
    private String title;
    private Integer year;
    private TraktIdsDTO ids;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public TraktIdsDTO getIds() { return ids; }
    public void setIds(TraktIdsDTO ids) { this.ids = ids; }
}
