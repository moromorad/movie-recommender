package com.moro.movie_recommender.controller;

import com.moro.movie_recommender.config.TraktProperties;
import com.moro.movie_recommender.dto.User;
import com.moro.movie_recommender.service.TraktService;
import com.moro.movie_recommender.service.MovieSyncService;
import com.moro.movie_recommender.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * Controller for managing users and their Trakt account linking.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final TraktService traktService;
    private final MovieSyncService movieSyncService;
    private final TraktProperties traktProperties;
    
    public UserController(UserService userService, TraktService traktService, TraktProperties traktProperties, MovieSyncService movieSyncService) {
        this.userService = userService;
        this.traktService = traktService;
        this.traktProperties = traktProperties;
        this.movieSyncService = movieSyncService;
    }
    
    /**
     * Creates a new user.
     * 
     * @param request containing the user name
     * @return the created user
     */
    @PostMapping
    public Mono<ResponseEntity<User>> createUser(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return userService.createUser(name.trim())
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalStateException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }
    
    /**
     * Gets a user by name.
     * 
     * @param name the user's name
     * @return the user or 404 if not found
     */
    @GetMapping("/{name}")
    public Mono<ResponseEntity<User>> getUser(@PathVariable String name) {
        return userService.getUser(name)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.<User>notFound().build()));
    }
    
    /**
     * Gets all users.
     * 
     * @return all users
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, User>>> getAllUsers() {
        return userService.getAllUsers()
                .map(ResponseEntity::ok);
    }
    
    /**
     * Deletes a user.
     * 
     * @param name the user's name
     * @return success or 404 if not found
     */
    @DeleteMapping("/{name}")
    public Mono<ResponseEntity<Map<String, String>>> deleteUser(@PathVariable String name) {
        return userService.deleteUser(name)
                .map(deleted -> {
                    if (deleted) {
                        return ResponseEntity.ok(Map.of("message", "User deleted: " + name));
                    } else {
                        return ResponseEntity.<Map<String, String>>notFound().build();
                    }
                });
    }
    
    /**
     * Starts the Trakt OAuth flow for linking a user's account.
     * 
     * @param userName the user's name
     * @return redirect to Trakt OAuth
     */
    @GetMapping("/{userName}/trakt/link")
    public Mono<ResponseEntity<Void>> linkTraktAccount(@PathVariable String userName) {
        return userService.getUser(userName)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + userName)))
                .map(user -> {
                    String redirect = java.net.URLEncoder.encode(traktProperties.getRedirectUri(), java.nio.charset.StandardCharsets.UTF_8);
                    String state = java.net.URLEncoder.encode(userName, java.nio.charset.StandardCharsets.UTF_8);
                    String clientId = java.net.URLEncoder.encode(traktProperties.getClientId(), java.nio.charset.StandardCharsets.UTF_8);
                    String authorizeUrl = traktProperties.getWebBase() + "/oauth/authorize?response_type=code" +
                            "&client_id=" + clientId +
                            "&redirect_uri=" + redirect +
                            "&state=" + state +
                            "&prompt=login"; // Hint provider to show login
                    
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(authorizeUrl)).build();
                });
    }
    
    /**
     * OAuth callback for linking Trakt account to a user.
     * 
     * @param userName the user's name (from state parameter)
     * @param code authorization code from Trakt
     * @param error optional error from Trakt
     * @return success or error response
     */
    @GetMapping("/{userName}/trakt/callback")
    public Mono<ResponseEntity<Void>> traktCallback(
            @PathVariable String userName,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error) {
        
        if (error != null) {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/index.html"))
                    .build());
        }
        
        if (code == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return traktService.exchangeCodeForToken(code)
                .flatMap(tokenMap -> {
                    String accessToken = (String) tokenMap.get("access_token");
                    String refreshToken = (String) tokenMap.get("refresh_token");
                    
                    return userService.linkTraktAccount(userName, accessToken, refreshToken)
                            // Enrich with Trakt username, auto-sync, then persist
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
                            .thenReturn(ResponseEntity.status(HttpStatus.FOUND)
                                    .location(URI.create("/index.html"))
                                    .build());
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
     * Unlinks the Trakt account from a user.
     * 
     * @param userName the user's name
     * @return success response
     */
    @DeleteMapping("/{userName}/trakt")
    public Mono<ResponseEntity<Map<String, Object>>> unlinkTraktAccount(@PathVariable String userName) {
        return userService.unlinkTraktAccount(userName)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "message", "Trakt account unlinked successfully",
                        "user", user.getName(),
                        "hasTraktAccount", user.hasTraktAccount()
                )))
                .switchIfEmpty(Mono.just(ResponseEntity.<Map<String, Object>>notFound().build()));
    }
}
