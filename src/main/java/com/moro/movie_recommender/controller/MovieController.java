package com.moro.movie_recommender.controller;

import com.moro.movie_recommender.dto.Movie;
import com.moro.movie_recommender.dto.ManualMovie;
import com.moro.movie_recommender.service.MovieSyncService;
import com.moro.movie_recommender.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Exposes movie-related API endpoints.
 * 
 * <p>Supports both Trakt-linked users and manual-only users:
 * <ul>
 *   <li>For Trakt users: Fetches movies from Trakt API</li>
 *   <li>For manual users: Returns manually added movies</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final UserService userService;
    private final MovieSyncService movieSyncService;

    public MovieController(UserService userService, MovieSyncService movieSyncService) {
        this.userService = userService;
        this.movieSyncService = movieSyncService;
    }

    /**
     * Gets watched movies for a specific user.
     * For Trakt-linked users: returns synced movies from Trakt
     * For manual-only users: returns manually added movies
     *
     * @param userName the user's name
     * @return 200 with watched movies; 404 if user not found
     */
    @GetMapping("/{userName}/watched")
    public Mono<ResponseEntity<List<Movie>>> getWatchedMovies(@PathVariable String userName) {
        return userService.getUser(userName)
                .map(user -> ResponseEntity.ok(user.getAllWatchedMovies()))
                .switchIfEmpty(Mono.just(ResponseEntity.<List<Movie>>notFound().build()));
    }
    
    /**
     * Gets all watched movies for a user (both Trakt and manual).
     * This combines Trakt movies with manually added movies.
     *
     * @param userName the user's name
     * @return 200 with all watched movies; 404 if user not found
     */
    @GetMapping("/{userName}/all")
    public Mono<ResponseEntity<Map<String, List<Movie>>>> getAllWatchedMovies(@PathVariable String userName) {
        return userService.getUser(userName)
                .map(user -> ResponseEntity.ok(movieSyncService.getAllMovies(user)))
                .switchIfEmpty(Mono.just(ResponseEntity.<Map<String, List<Movie>>>notFound().build()));
    }
    
    /**
     * Syncs Trakt movies for a user and updates their watched list.
     *
     * @param userName the user's name
     * @return 200 with sync result; 404 if user not found
     */
    @PostMapping("/{userName}/sync")
    public Mono<ResponseEntity<Map<String, Object>>> syncTraktMovies(@PathVariable String userName) {
        return userService.getUser(userName)
                .flatMap(user -> {
                    if (!user.hasTraktAccount()) {
                        return Mono.just(ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "User does not have a linked Trakt account")));
                    }
                    
                    return movieSyncService.syncTraktMovies(user)
                            .map(syncedUser -> ResponseEntity.ok(Map.<String, Object>of(
                                    "message", "Trakt movies synced successfully",
                                    "user", syncedUser.getName(),
                                    "totalMovies", syncedUser.getAllWatchedMovies().size(),
                                    "traktMovies", syncedUser.getTraktMovies().size(),
                                    "manualMovies", syncedUser.getManualMovies().size()
                            )));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.<Map<String, Object>>notFound().build()));
    }
    
    /**
     * Adds a manual movie to a user's watched list.
     *
     * @param userName the user's name
     * @param movieRequest the movie data
     * @return 200 with the added movie; 404 if user not found
     */
    @PostMapping("/{userName}/manual")
    public Mono<ResponseEntity<?>> addManualMovie(@PathVariable String userName, 
                                                  @RequestBody Map<String, Object> movieRequest) {
        return userService.getUser(userName)
                .map(user -> {
                    String title = getString(movieRequest, "title");
                    Integer year = getInteger(movieRequest, "year");
                    Integer userRating = getOptionalRating(movieRequest, "userRating");

                    if (title == null || title.isBlank()) {
                        return ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "title must be a non-empty string"));
                    }
                    if (year == null) {
                        return ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "year must be an integer"));
                    }
                    if (movieRequest.containsKey("userRating") && userRating == null) {
                        return ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "userRating must be an integer between 1 and 10"));
                    }

                    ManualMovie movie = new ManualMovie(title, year, userRating);
                    movieSyncService.addManualMovie(user, movie);

                    return ResponseEntity.ok(movie);
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
    
    /**
     * Removes a movie from a user's watched list.
     *
     * @param userName the user's name
     * @param movieRequest the movie data to remove
     * @return 200 with removal result; 404 if user not found
     */
    @DeleteMapping("/{userName}/manual")
    public Mono<ResponseEntity<Map<String, Object>>> removeManualMovie(@PathVariable String userName,
                                                     @RequestBody Map<String, Object> movieRequest) {
        return userService.getUser(userName)
                .map(user -> {
                    String title = getString(movieRequest, "title");
                    Integer year = getInteger(movieRequest, "year");
                    
                    if (title == null || title.isBlank()) {
                        return ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "title must be a non-empty string"));
                    }
                    if (year == null) {
                        return ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "year must be an integer"));
                    }
                    
                    // Find and remove the movie from manual movies
                    boolean removed = user.getManualMovies().stream()
                            .filter(movie -> title.equals(movie.getTitle()) && year.equals(movie.getYear()))
                            .findFirst()
                            .map(movie -> movieSyncService.removeManualMovie(user, movie))
                            .orElse(false);
                    
                    if (removed) {
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "message", "Movie removed successfully",
                                "title", title,
                                "year", year
                        ));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.<String, Object>of("error", "Movie not found"));
                    }
                })
                .switchIfEmpty(Mono.just(ResponseEntity.<Map<String, Object>>notFound().build()));
    }

    // --- Helpers: safe extraction and validation ---
    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return (v instanceof String s) ? s : null;
    }

    private static Integer getInteger(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private static Integer getOptionalRating(Map<String, Object> map, String key) {
        Integer val = getInteger(map, key);
        if (val == null) return null; // not provided or unparsable -> caller decides
        if (val >= 1 && val <= 10) return val;
        return null; // out of range -> signal invalid
    }
}
