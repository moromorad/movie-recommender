package com.moro.movie_recommender.service;

import com.moro.movie_recommender.dto.Movie;
import com.moro.movie_recommender.dto.User;
import com.moro.movie_recommender.dto.trakt.TraktMovieDTO;
import com.moro.movie_recommender.dto.trakt.TraktWatchedItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for syncing Trakt movies with user's watched movies list.
 * Handles merging Trakt movies with manually added movies.
 */
@Service
public class MovieSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(MovieSyncService.class);
    
    private final TraktService traktService;
    
    public MovieSyncService(TraktService traktService) {
        this.traktService = traktService;
    }
    
    /**
     * Syncs Trakt movies for a user, preserving manually added movies.
     * 
     * @param user the user to sync movies for
     * @return the updated user with synced movies
     */
    public Mono<User> syncTraktMovies(User user) {
        if (!user.hasTraktAccount()) {
            logger.debug("User {} has no Trakt account, skipping sync", user.getName());
            return Mono.just(user);
        }
        
        String accessToken = user.getTraktAccessToken();
        logger.info("Syncing Trakt movies for user: {}", user.getName());
        
        return traktService.getWatchedMovies(accessToken)
                .collectList()
                .map(traktWatchedItems -> {
                    // Extract TraktMovieDTO objects from watched items
                    List<TraktMovieDTO> newTraktMovies = traktWatchedItems.stream()
                            .map(TraktWatchedItemDTO::getMovie)
                            .collect(Collectors.toList());
                    
                    // Update only the Trakt movies list (preserve manual movies)
                    user.setTraktMovies(new ArrayList<>(newTraktMovies));
                    
                    logger.info("Synced {} Trakt movies and preserved {} manual movies for user: {}", 
                            newTraktMovies.size(), user.getManualMovies().size(), user.getName());
                    
                    return user;
                })
                .onErrorResume(error -> {
                    logger.error("Failed to sync Trakt movies for user {}: {}", user.getName(), error.getMessage());
                    return Mono.just(user); // Return user unchanged on error
                });
    }
    
    /**
     * Syncs Trakt movies for a user and returns the updated user.
     * This is a convenience method that doesn't modify the original user object.
     * 
     * @param user the user to sync movies for
     * @return a new user object with synced movies
     */
    public Mono<User> syncTraktMoviesCopy(User user) {
        // Create a copy of the user to avoid modifying the original
        User userCopy = new User(user.getName(), 
                new java.util.ArrayList<>(user.getManualMovies()),
                new java.util.ArrayList<>(user.getTraktMovies()),
                user.getTraktAccount());
        
        return syncTraktMovies(userCopy);
    }
    
    /**
     * Adds a manual movie to a user's watched list.
     * 
     * @param user the user to add the movie to
     * @param movie the movie to add
     * @return the updated user
     */
    public User addManualMovie(User user, Movie movie) {
        if (movie != null) {
            user.addManualMovie(movie);
            logger.info("Added manual movie '{}' to user: {}", movie.getTitle(), user.getName());
        }
        return user;
    }
    
    /**
     * Removes a manual movie from a user's watched list.
     * 
     * @param user the user to remove the movie from
     * @param movie the movie to remove
     * @return true if the movie was removed, false if not found
     */
    public boolean removeManualMovie(User user, Movie movie) {
        boolean removed = user.removeManualMovie(movie);
        if (removed) {
            logger.info("Removed manual movie '{}' from user: {}", movie.getTitle(), user.getName());
        }
        return removed;
    }
    
    /**
     * Gets all movies for a user (both Trakt and manual).
     * 
     * @param user the user to get movies for
     * @return map containing both Trakt and manual movies
     */
    public Map<String, List<Movie>> getAllMovies(User user) {
        return Map.of(
            "traktMovies", user.getTraktMovies(),
            "manualMovies", user.getManualMovies(),
            "allMovies", user.getAllWatchedMovies()
        );
    }
}
