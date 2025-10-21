package com.moro.movie_recommender.service;

import com.moro.movie_recommender.config.TraktProperties;
import com.moro.movie_recommender.dto.trakt.TraktWatchedItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Thin client around Trakt's HTTP API using Spring WebClient.
 *
 * <p>Configures a client with the appropriate base URL and default headers
 * (Trakt API key and version), and exposes operations needed by controllers.
 */
@Service
public class TraktService {

    private static final Logger logger = LoggerFactory.getLogger(TraktService.class);
    
    private final WebClient webClient;
    private final TraktProperties props;

    /**
     * Constructs a WebClient pre-configured with Trakt base URL and headers.
     *
     * @param builder shared WebClient builder
     * @param props   Trakt configuration properties
     */
    public TraktService(WebClient.Builder builder, TraktProperties props) {
        this.props = props;
        this.webClient = builder
                .baseUrl(props.getApiBase())
                .defaultHeader("trakt-api-key", props.getClientId())
                .defaultHeader("trakt-api-version", props.getApiVersion())
                .build();
    }

    /**
     * Exchanges an OAuth authorization code for an access token at
     * {@code POST /oauth/token}.
     *
     * @param code authorization code received from Trakt
     * @return a Mono emitting the token response map (includes {@code access_token})
     */
    public Mono<Map<String, Object>> exchangeCodeForToken(String code) {
        return webClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "code", code,
                        "client_id", props.getClientId(),
                        "client_secret", props.getClientSecret(),
                        "redirect_uri", props.getRedirectUri(),
                        "grant_type", "authorization_code"
                ))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Fetches the user's watched movies from Trakt at
     * {@code GET /sync/watched/movies?extended=full}.
     * Also fetches user ratings and merges them into the movie objects.
     *
     * @param accessToken bearer token obtained via OAuth exchange
     * @return a Flux streaming watched items mapped to DTOs with ratings
     */
    public Flux<TraktWatchedItemDTO> getWatchedMovies(String accessToken) {
        // Fetch both watched movies and ratings in parallel
        Flux<TraktWatchedItemDTO> watchedMovies = webClient.get()
                .uri("/sync/watched/movies?extended=full")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToFlux(TraktWatchedItemDTO.class);

        // Fetch all ratings once and cache them
        Mono<List<Map<String, Object>>> ratingsMono = getUserRatings(accessToken).collectList();

        // Combine watched movies with ratings
        return watchedMovies.flatMap(watchedItem -> 
            ratingsMono.map(ratingsList -> {
                // Find matching rating for this movie using trakt ID
                Long movieTraktId = watchedItem.getMovie().getIds().getTrakt();
                Integer userRating = findUserRatingForMovie(ratingsList, movieTraktId);
                
                // Set the user rating on the movie (null if no rating found)
                watchedItem.getMovie().setUserRating(userRating);
                
                // Log rating status for debugging
                if (userRating != null) {
                    logger.debug("Found rating {} for movie '{}' (Trakt ID: {})", 
                        userRating, watchedItem.getMovie().getTitle(), movieTraktId);
                } else {
                    logger.debug("No rating found for movie '{}' (Trakt ID: {})", 
                        watchedItem.getMovie().getTitle(), movieTraktId);
                }
                
                return watchedItem;
            })
        );
    }

    /**
     * Fetches the currently authenticated Trakt user's profile using the provided access token.
     * Tries to be resilient by returning a generic map shape.
     */
    public Mono<Map<String, Object>> getCurrentUser(String accessToken) {
        return webClient.get()
                .uri("/users/me")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Finds the user rating for a specific movie by its Trakt ID.
     * Returns null if no rating is found.
     *
     * @param ratingsList list of all user ratings
     * @param movieTraktId the Trakt ID of the movie to find rating for
     * @return the user rating (1-10) or null if not found
     */
    private Integer findUserRatingForMovie(List<Map<String, Object>> ratingsList, Long movieTraktId) {
        if (movieTraktId == null || ratingsList == null) {
            return null;
        }

        return ratingsList.stream()
                .filter(rating -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> movie = (Map<String, Object>) rating.get("movie");
                        if (movie == null) return false;
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                        if (ids == null) return false;
                        
                        Object traktIdObj = ids.get("trakt");
                        if (traktIdObj == null) return false;
                        
                        // Handle both Integer and Long trakt IDs
                        Long traktId = null;
                        if (traktIdObj instanceof Integer) {
                            traktId = ((Integer) traktIdObj).longValue();
                        } else if (traktIdObj instanceof Long) {
                            traktId = (Long) traktIdObj;
                        }
                        
                        return movieTraktId.equals(traktId);
                    } catch (Exception e) {
                        // Log error but continue processing other ratings
                        logger.debug("Error parsing rating for movie ID {}: {}", movieTraktId, e.getMessage());
                        return false;
                    }
                })
                .map(rating -> {
                    try {
                        Object ratingObj = rating.get("rating");
                        if (ratingObj instanceof Integer) {
                            return (Integer) ratingObj;
                        } else if (ratingObj instanceof Long) {
                            return ((Long) ratingObj).intValue();
                        }
                        return null;
                    } catch (Exception e) {
                        logger.debug("Error extracting rating value: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(rating -> rating != null && rating >= 1 && rating <= 10)
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches the user's movie ratings from Trakt at
     * {@code GET /sync/ratings/movies}.
     *
     * @param accessToken bearer token obtained via OAuth exchange
     * @return a Flux streaming rating objects
     */
    public Flux<Map<String, Object>> getUserRatings(String accessToken) {
        return webClient.get()
                .uri("/sync/ratings/movies")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
