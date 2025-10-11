package com.moro.movie_recommender.controller;

import com.moro.movie_recommender.dto.trakt.TraktWatchedItemDTO;
import com.moro.movie_recommender.service.TraktService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Exposes movie-related API endpoints backed by Trakt.
 *
 * <p>Requires that the user has a valid Trakt access token stored in the
 * session (set during the OAuth callback). If no token is present, requests
 * return 401 (Unauthorized).
 */
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TraktService traktService;

    public MovieController(TraktService traktService) {
        this.traktService = traktService;
    }

    /**
     * Streams the authenticated user's watched movies from Trakt.
     *
     * @param exchange used to access the session and read the access token
     * @return 200 with a Flux of watched items when authorized; 401 if no token
     */
    @GetMapping("/watched")
    public Mono<ResponseEntity<Flux<TraktWatchedItemDTO>>> getWatched(ServerWebExchange exchange) {
        return exchange.getSession().map(session -> (String) session.getAttributes().get(AuthController.SESSION_TOKEN_KEY))
                .flatMap(token -> {
                    if (token == null || token.isBlank()) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }
                    Flux<TraktWatchedItemDTO> flux = traktService.getWatchedMovies(token);
                    return Mono.just(ResponseEntity.ok(flux));
                });
    }
}
