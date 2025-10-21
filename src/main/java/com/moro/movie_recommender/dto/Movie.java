package com.moro.movie_recommender.dto;

/**
 * Interface representing a movie with basic information.
 * All movie implementations should provide title, year, and user rating.
 */
public interface Movie {
    String getTitle();
    Integer getYear();
    Integer getUserRating();
}
