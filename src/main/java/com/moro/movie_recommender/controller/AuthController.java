package com.moro.movie_recommender.controller;

import com.moro.movie_recommender.config.TraktProperties;
import com.moro.movie_recommender.service.TraktService;
import com.moro.movie_recommender.service.UserService;
import com.moro.movie_recommender.service.MovieSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * Handles the Trakt OAuth flow for the application.
 *
 * <p>Flow summary:
 * <ol>
 *   <li>User visits the login endpoint and is redirected to Trakt's authorize page.</li>
 *   <li>After consenting, Trakt redirects back with an authorization code.</li>
 *   <li>We exchange the code for an access token and store it in the reactive session.</li>
 * </ol>
 * The access token is later used by API endpoints to call Trakt on behalf of the user.
 */
@Controller
public class AuthController {
    public static final String SESSION_TOKEN_KEY = "TRAKT_ACCESS_TOKEN";

    private final TraktProperties props;
    private final TraktService traktService;
    private final UserService userService;
    private final MovieSyncService movieSyncService;
    
    /**
     * 
     * Constructs the controller with configuration and Trakt service client.
     *
     * @param props         Trakt configuration properties (client id/secret, redirect URI, bases)
     * @param traktService  service for exchanging tokens and calling Trakt APIs
     */
    public AuthController(TraktProperties props, TraktService traktService, UserService userService, MovieSyncService movieSyncService) {
        this.props = props;
        this.traktService = traktService;
        this.userService = userService;
        this.movieSyncService = movieSyncService;
    }

    /**
     * Starts the OAuth authorization by redirecting the user to Trakt's authorize URL.
     *
     * @return a 302 (Found) redirect response pointing to Trakt's authorize page
     */
    @GetMapping("/auth/trakt/login")
    public Mono<ResponseEntity<Void>> login(@RequestParam(name = "state", required = false) String state) {
        String redirect = java.net.URLEncoder.encode(props.getRedirectUri(), java.nio.charset.StandardCharsets.UTF_8);
        String clientId = java.net.URLEncoder.encode(props.getClientId(), java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder(props.getWebBase())
                .append("/oauth/authorize?response_type=code")
                .append("&client_id=").append(clientId)
                .append("&redirect_uri=").append(redirect)
                .append("&prompt=login"); // Encourage fresh login
        if (state != null && !state.isBlank()) {
            url.append("&state=")
               .append(java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url.toString())).build());
    }

    /**
     * OAuth callback endpoint that receives the authorization code from Trakt, exchanges it
     * for an access token, then stores that token in the reactive WebSession.
     *
     * @param code     authorization code returned by Trakt after user consent
     * @param error    optional error returned by Trakt when authorization fails or is cancelled
     * @param exchange the server exchange, used to access the session
     * @return a redirect to the home page on success; 400 if code is missing
     */
    @GetMapping("/login/oauth2/code/trakt")
    public Mono<ResponseEntity<Void>> callback(@RequestParam(name = "code", required = false) String code,
                                               @RequestParam(name = "error", required = false) String error,
                                               @RequestParam(name = "state", required = false) String state,
                                               ServerWebExchange exchange) {
        if (error != null) {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html"))
                    .build());
        }
        if (code == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return traktService.exchangeCodeForToken(code)
                .flatMap(tokenMap -> {
                    String accessToken = (String) tokenMap.get("access_token");
                    String refreshToken = (String) tokenMap.get("refresh_token");
                    Mono<ResponseEntity<Void>> redirect = Mono.just(
                            ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build()
                    );
                    if (state != null && !state.isBlank()) {
                        return userService.linkTraktAccount(state, accessToken, refreshToken)
                                .flatMap(user -> traktService.getCurrentUser(accessToken)
                                        .onErrorReturn(java.util.Map.of())
                                        .map(profile -> {
                                            String username = extractTraktUsername(profile);
                                            if (user.getTraktAccount() != null) {
                                                user.getTraktAccount().setTraktUsername(username);
                                            }
                                            return user;
                                        }))
                                .flatMap(movieSyncService::syncTraktMovies)
                                .flatMap(userService::replaceUser)
                                .then(redirect);
                    }
                    return exchange.getSession()
                            .doOnNext(session -> storeToken(session, tokenMap))
                            .then(redirect);
                });
    }

    private String extractTraktUsername(Map<String, Object> profile) {
        if (profile == null) return null;
        Object u = profile.get("username");
        if (u instanceof String s && !s.isBlank()) return s;
        Object idsObj = profile.get("ids");
        if (idsObj instanceof Map<?, ?> ids) {
            Object slug = ids.get("slug");
            if (slug instanceof String s2 && !s2.isBlank()) return s2;
        }
        Object nestedUser = profile.get("user");
        if (nestedUser instanceof Map<?, ?> userMap) {
            Object u2 = userMap.get("username");
            if (u2 instanceof String s3 && !s3.isBlank()) return s3;
            Object ids2 = userMap.get("ids");
            if (ids2 instanceof Map<?, ?> ids2m) {
                Object slug2 = ids2m.get("slug");
                if (slug2 instanceof String s4 && !s4.isBlank()) return s4;
            }
        }
        return null;
    }

    /**
     * Stores the access token from the token response into the session.
     * The token is later read by API controllers when calling Trakt.
     *
     * @param session  reactive WebSession associated with the user
     * @param tokenMap token response map (expects an "access_token" key)
     */
    private void storeToken(WebSession session, Map<String, Object> tokenMap) {
        Object token = tokenMap.get("access_token");
        if (token instanceof String t) {
            session.getAttributes().put(SESSION_TOKEN_KEY, t);
        }
    }
}
