# Trakt API Integration Plan

## Overview

Set up a Spring Boot application that connects to Trakt API using OAuth 2.0, fetches user watched movies, stores them in PostgreSQL, and exposes REST endpoints.

## Implementation Steps

### 1. Configure Dependencies

Add to `pom.xml`:

- `spring-boot-starter-webflux` - REST API support and WebClient for Trakt API calls
- `spring-boot-starter-data-jpa` - Database access
- `postgresql` - PostgreSQL driver
- `spring-boot-starter-oauth2-client` - OAuth 2.0 support
- `lombok` - Reduce boilerplate code

### 2. Database Configuration

Update `application.properties`:

- PostgreSQL connection settings (URL, username, password)
- JPA/Hibernate configuration
- OAuth 2.0 client registration for Trakt
- Trakt API credentials (client-id, client-secret)

### 3. Create Domain Models

Create entities in `src/main/java/com/moro/movie_recommender/model/`:

- `User.java` - Store user info and Trakt access tokens
- `Movie.java` - Store movie details (title, year, genres, ratings)
- `WatchedMovie.java` - Link users to watched movies with watch date

### 4. Create Repositories

Create JPA repositories in `src/main/java/com/moro/movie_recommender/repository/`:

- `UserRepository.java`
- `MovieRepository.java`
- `WatchedMovieRepository.java`

### 5. Create Trakt DTOs

Create DTOs in `src/main/java/com/moro/movie_recommender/dto/trakt/`:

- `TraktMovieDTO.java` - Response from Trakt API
- `TraktWatchedItemDTO.java` - Watched movie response
- `TraktIdsDTO.java` - Movie identifiers (Trakt, IMDB, TMDB ids)

### 6. Implement Trakt Service

Create `TraktService.java` in `src/main/java/com/moro/movie_recommender/service/`:

- `getWatchedMovies(String accessToken)` - Fetch user's watched movies from Trakt
- Parse and map Trakt API responses to domain objects
- Use WebClient for HTTP requests to Trakt API (included in WebFlux)

### 7. Implement Movie Service

Create `MovieService.java` in `src/main/java/com/moro/movie_recommender/service/`:

- `saveOrUpdateMovie(Movie movie)` - Store movie in database
- `getUserWatchedMovies(Long userId)` - Retrieve user's watched movies
- `syncWatchedMoviesFromTrakt(Long userId, String accessToken)` - Fetch from Trakt and store

### 8. Create REST Controllers

Create controllers in `src/main/java/com/moro/movie_recommender/controller/`:

- `AuthController.java` - Handle OAuth 2.0 callback and token storage
- `MovieController.java` - Endpoints:
  - `GET /api/movies/watched` - Get current user's watched movies
  - `POST /api/movies/sync` - Sync watched movies from Trakt

### 9. Configure Security

Create `SecurityConfig.java` in `src/main/java/com/moro/movie_recommender/config/`:

- Configure OAuth 2.0 login with Trakt
- Set up security for endpoints
- Handle token storage and retrieval

### 10. Testing

Update test file or create integration tests to verify:

- Trakt API connection
- Database persistence
- OAuth flow (manual testing via browser)

## Key Files to Create/Modify

- `pom.xml` - Add dependencies (WebFlux only)
- `application.properties` - Configuration
- Model classes: `User`, `Movie`, `WatchedMovie`
- Repository interfaces for JPA
- Service classes: `TraktService`, `MovieService`
- Controller classes: `AuthController`, `MovieController`
- Security configuration: `SecurityConfig`

## Notes

- User will need to register an app at https://trakt.tv/oauth/applications to get client ID and secret
- Trakt OAuth redirect URI should be configured as `http://localhost:8080/login/oauth2/code/trakt`
- Movie data includes: title, year, genres array, rating (0-10), votes count from Trakt API
- Using WebFlux provides both REST endpoints and WebClient for API calls without conflicts

### To-dos

- [ ] Add required dependencies to pom.xml (webflux, jpa, postgresql, oauth2-client, lombok)
- [ ] Configure application.properties with PostgreSQL and Trakt OAuth settings
- [ ] Create entity classes: User, Movie, WatchedMovie
- [ ] Create JPA repository interfaces for User, Movie, and WatchedMovie
- [ ] Create DTOs for Trakt API responses (TraktMovieDTO, TraktWatchedItemDTO, TraktIdsDTO)
- [ ] Implement TraktService to call Trakt API and fetch watched movies
- [ ] Implement MovieService for database operations and sync logic
- [ ] Create AuthController and MovieController with REST endpoints
- [ ] Create SecurityConfig for OAuth 2.0 and endpoint security
