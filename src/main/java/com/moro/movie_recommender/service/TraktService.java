package com.moro.movie_recommender.service;

import com.moro.movie_recommender.config.TraktProperties;
import com.moro.movie_recommender.dto.trakt.TraktWatchedItemDTO;
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

        Flux<Map<String, Object>> ratings = getUserRatings(accessToken);

        // Combine watched movies with ratings
        return watchedMovies.zipWith(ratings.collectList().flux().repeat())
                .map(tuple -> {
                    TraktWatchedItemDTO watchedItem = tuple.getT1();
                    List<Map<String, Object>> ratingsList = tuple.getT2();
                    
                    // Find matching rating for this movie using trakt ID
                    Long movieTraktId = watchedItem.getMovie().getIds().getTrakt();
                    Integer userRating = ratingsList.stream()
                            .filter(rating -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> movie = (Map<String, Object>) rating.get("movie");
                                @SuppressWarnings("unchecked")
                                Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                                Object traktId = ids.get("trakt");
                                return movieTraktId != null && movieTraktId.equals(traktId);
                            })
                            .map(rating -> (Integer) rating.get("rating"))
                            .findFirst()
                            .orElse(null);
                    
                    // Set the user rating on the movie
                    watchedItem.getMovie().setUserRating(userRating);
                    return watchedItem;
                });
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
