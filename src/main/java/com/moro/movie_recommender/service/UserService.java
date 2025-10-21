package com.moro.movie_recommender.service;

import com.moro.movie_recommender.dto.ManualMovie;
import com.moro.movie_recommender.dto.Movie;
import com.moro.movie_recommender.dto.TraktAccount;
import com.moro.movie_recommender.dto.User;
import com.moro.movie_recommender.dto.trakt.TraktIdsDTO;
import com.moro.movie_recommender.dto.trakt.TraktMovieDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing users and their Trakt accounts.
 * In a real application, this would use a database, but for now
 * we'll use in-memory storage for simplicity.
 */
@Service
public class UserService {
    
    // In-memory storage - in production, this would be a database
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    /**
     * Creates a new user with the given name.
     * 
     * @param name the user's name
     * @return the created user
     */
    public Mono<User> createUser(String name) {
        // Prevent overwriting existing users; ensure atomicity via putIfAbsent
        User newUser = new User(name);
        User existing = users.putIfAbsent(name, newUser);
        if (existing != null) {
            return Mono.error(new IllegalStateException("User already exists: " + name));
        }
        return Mono.just(newUser);
    }
    
    /**
     * Gets a user by name.
     * 
     * @param name the user's name
     * @return the user or empty if not found
     */
    public Mono<User> getUser(String name) {
        User user = users.get(name);
        return user != null ? Mono.just(user) : Mono.empty();
    }
    
    /**
     * Links a Trakt account to a user.
     * 
     * @param userName the user's name
     * @param accessToken the Trakt access token
     * @param refreshToken the Trakt refresh token (optional)
     * @return the updated user or empty if user not found
     */
    public Mono<User> linkTraktAccount(String userName, String accessToken, String refreshToken) {
        return getUser(userName)
                .map(user -> {
                    user.linkTraktAccount(accessToken, refreshToken);
                    return user;
                });
    }
    
    /**
     * Unlinks the Trakt account from a user.
     * 
     * @param userName the user's name
     * @return the updated user or empty if user not found
     */
    public Mono<User> unlinkTraktAccount(String userName) {
        return getUser(userName)
                .map(user -> {
                    user.unlinkTraktAccount();
                    return user;
                });
    }

    /**
     * Replaces the stored user with the provided instance if present.
     * Returns empty if the user does not exist.
     */
    public Mono<User> replaceUser(User user) {
        if (user == null || user.getName() == null) {
            return Mono.empty();
        }
        return getUser(user.getName())
                .map(existing -> {
                    users.put(user.getName(), user);
                    return user;
                });
    }
    
    /**
     * Gets all users.
     * 
     * @return all users
     */
    public Mono<Map<String, User>> getAllUsers() {
        // Create a deep, read-only snapshot so callers cannot mutate internal state
        Map<String, User> snapshotSource = new HashMap<>(users); // weakly consistent snapshot
        Map<String, User> copies = new HashMap<>(snapshotSource.size());
        snapshotSource.forEach((name, user) -> copies.put(name, deepCopyUser(user)));
        return Mono.just(Map.copyOf(copies));
    }
    
    /**
     * Deletes a user.
     * 
     * @param userName the user's name
     * @return true if user was deleted, false if not found
     */
    public Mono<Boolean> deleteUser(String userName) {
        User removed = users.remove(userName);
        return Mono.just(removed != null);
    }

    // --------- Defensive copy helpers ---------
    private User deepCopyUser(User original) {
        if (original == null) return null;
        List<Movie> manualCopy = original.getManualMovies().stream()
                .map(this::copyMovie)
                .toList();
        List<Movie> traktCopy = original.getTraktMovies().stream()
                .map(this::copyMovie)
                .toList();
        TraktAccount accountCopy = copyTraktAccount(original.getTraktAccount());
        return new User(original.getName(), manualCopy, traktCopy, accountCopy);
    }

    private Movie copyMovie(Movie m) {
        if (m == null) return null;
        if (m instanceof ManualMovie mm) {
            return new ManualMovie(mm.getTitle(), mm.getYear(), mm.getUserRating());
        }
        if (m instanceof TraktMovieDTO tm) {
            TraktMovieDTO copy = new TraktMovieDTO();
            copy.setTitle(tm.getTitle());
            copy.setYear(tm.getYear());
            copy.setUserRating(tm.getUserRating());
            TraktIdsDTO ids = tm.getIds();
            if (ids != null) {
                TraktIdsDTO idsCopy = new TraktIdsDTO();
                idsCopy.setTrakt(ids.getTrakt());
                idsCopy.setImdb(ids.getImdb());
                idsCopy.setTmdb(ids.getTmdb());
                idsCopy.setSlug(ids.getSlug());
                copy.setIds(idsCopy);
            }
            return copy;
        }
        // Fallback: snapshot using a ManualMovie with common fields only
        return new ManualMovie(m.getTitle(), m.getYear(), m.getUserRating());
    }

    private TraktAccount copyTraktAccount(TraktAccount acct) {
        if (acct == null) return null;
        TraktAccount c = new TraktAccount();
        c.setAccessToken(acct.getAccessToken());
        c.setRefreshToken(acct.getRefreshToken());
        c.setLinkedAt(acct.getLinkedAt());
        c.setTraktUsername(acct.getTraktUsername());
        c.setTraktUserId(acct.getTraktUserId());
        return c;
    }
}
