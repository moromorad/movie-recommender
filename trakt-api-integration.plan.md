# Trakt API Integration Plan

## Overview

Set up a Spring Boot application that connects to Trakt API using OAuth 2.0, fetches user watched movies, and exposes REST endpoints. Defer database/persistence work until later.

## Implementation Steps

### 1. Configure Dependencies

Add to `pom.xml`:

- `spring-boot-starter-webflux` - REST API support and WebClient for Trakt API calls
- `spring-boot-starter-oauth2-client` - OAuth 2.0 support
- `lombok` - Reduce boilerplate code

### 2. App Configuration (no DB)

Update `application.properties`:

- OAuth 2.0 client registration for Trakt
- Trakt API credentials (client-id, client-secret)

### 3. Create Trakt DTOs

Create DTOs in `src/main/java/com/moro/movie_recommender/dto/trakt/`:

- `TraktMovieDTO.java` - Response from Trakt API
- `TraktWatchedItemDTO.java` - Watched movie response
- `TraktIdsDTO.java` - Movie identifiers (Trakt, IMDB, TMDB ids)

### 4. Implement Trakt Service

Create `TraktService.java` in `src/main/java/com/moro/movie_recommender/service/`:

- `getWatchedMovies(String accessToken)` - Fetch user's watched movies from Trakt
- Parse and map Trakt API responses to simple domain objects (no persistence)
- Use WebClient for HTTP requests to Trakt API (included in WebFlux)

### 5. Implement Movie Facade/Service (no persistence)

Create `MovieService.java` in `src/main/java/com/moro/movie_recommender/service/`:

- Orchestrate calls to `TraktService` to fetch watched movies for the authenticated user
- Optionally provide in-memory caching while the app runs
- Leave extension points to add persistence later

### 6. Create REST Controllers

Create controllers in `src/main/java/com/moro/movie_recommender/controller/`:

- `AuthController.java` - Handle OAuth 2.0 login/callback and manage tokens in session for now
- `MovieController.java` - Endpoints:
  - `GET /api/movies/watched` - Get current user's watched movies from Trakt

### 7. Configure Security

Create `SecurityConfig.java` in `src/main/java/com/moro/movie_recommender/config/`:

- Configure OAuth 2.0 login with Trakt
- Secure API endpoints as needed

### 8. Testing

Add tests to verify:

- Trakt API connection and DTO mapping
- OAuth flow basics (manual validation via browser for now)

## Key Files to Create/Modify

- `pom.xml` - Add dependencies (WebFlux, OAuth2 client, Lombok)
- `application.properties` - OAuth/Trakt configuration
- DTOs for Trakt responses
- Services: `TraktService`, `MovieService`
- Controllers: `AuthController`, `MovieController`
- Security configuration: `SecurityConfig`

## Notes

- Register an app at https://trakt.tv/oauth/applications to get client ID and secret
- Trakt OAuth redirect URI: `http://localhost:8080/login/oauth2/code/trakt`
- Movie data includes: title, year, genres array, rating (0-10), votes count from Trakt API
- WebFlux provides REST endpoints and WebClient for API calls

### To-dos (Later: Persistence)

- [ ] Add `spring-boot-starter-data-jpa` and PostgreSQL driver
- [ ] Create entity classes: User, Movie, WatchedMovie
- [ ] Add JPA repositories for persistence
- [ ] Move token and movie storage from session/in-memory to database
