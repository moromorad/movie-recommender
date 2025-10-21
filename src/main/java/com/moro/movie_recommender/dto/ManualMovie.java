package com.moro.movie_recommender.dto;

/**
 * Represents a manually entered movie that is not from Trakt API.
 * Implements the Movie interface with required fields.
 */
public class ManualMovie implements Movie {
    private String title;
    private Integer year;
    private Integer userRating;

    public ManualMovie(String title, Integer year, Integer userRating) {
        this.title = title;
        this.year = year;
        this.userRating = userRating;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Integer getYear() {
        return year;
    }

    @Override
    public Integer getUserRating() {
        return userRating;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }
}
