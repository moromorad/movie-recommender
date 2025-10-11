package com.moro.movie_recommender.controller;

import com.moro.movie_recommender.config.TraktProperties;
import com.moro.movie_recommender.service.TraktService;
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

    // test comment 2 
    // test comment
    
    /**
     * 
     * Constructs the controller with configuration and Trakt service client.
     *
     * @param props         Trakt configuration properties (client id/secret, redirect URI, bases)
     * @param traktService  service for exchanging tokens and calling Trakt APIs
     */
    public AuthController(TraktProperties props, TraktService traktService) {
        this.props = props;
        this.traktService = traktService;
    }

    /**
     * Starts the OAuth authorization by redirecting the user to Trakt's authorize URL.
     *
     * @return a 302 (Found) redirect response pointing to Trakt's authorize page
     */
    @GetMapping("/auth/trakt/login")
    public Mono<ResponseEntity<Void>> login() {
        String authorizeUrl = props.getWebBase() + "/oauth/authorize?response_type=code" +
                "&client_id=" + props.getClientId() +
                "&redirect_uri=" + props.getRedirectUri();
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizeUrl)).build());
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
                                               ServerWebExchange exchange) {
        if (error != null) {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/"))
                    .build());
        }
        if (code == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return traktService.exchangeCodeForToken(code)
                .flatMap(tokenMap -> exchange.getSession()
                        .doOnNext(session -> storeToken(session, tokenMap))
                        .thenReturn(ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/")).build())
                );
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


