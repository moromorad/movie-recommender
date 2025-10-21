# Movie Recommender API Documentation

This document lists all current endpoints, what they do, and example usages.

## Base URL

- Local development: `http://localhost:8080`

## Overview

- Users can be:
  - Trakt‑linked: movies synced from Trakt, plus optional manual movies.
  - Manual‑only: movies managed entirely via API.
- GET movie endpoints do not trigger Trakt sync. Call the sync endpoint first to refresh.
- After completing Trakt OAuth, the app performs a one‑time auto‑sync.

## Trakt Linking

### Link Trakt (per user)
- GET `/api/users/{userName}/trakt/link`
- Redirects the browser to Trakt’s OAuth page with `state={userName}` and `prompt=login` so you can sign into a different Trakt account if needed.
- After consent, Trakt redirects to the configured redirect URI (see `src/main/resources/application.properties`), which includes the `state` value.

Callback behavior (handled by `/login/oauth2/code/trakt`):
- Links the app user identified by `state`.
- Fetches the Trakt profile and stores `traktUsername` on the user.
- Auto‑syncs watched movies once and persists the user.
- Redirects back to `/index.html`.

### Generic Trakt login (optional)
- GET `/auth/trakt/login?state={optionalUserName}`
- Same as above, but not user‑specific. If `state` is provided, the callback links that user; otherwise, the token is stored in the browser session only.

## Users

### Create user
- POST `/api/users`
- Body
  - `{ "name": "alice" }`
- Responses
  - 200 OK with `User` JSON
  - 400 Bad Request if name is missing/blank
  - 409 Conflict if the user already exists

### Get user
- GET `/api/users/{name}`
- Responses
  - 200 OK with `User` JSON
  - 404 Not Found

### Get all users
- GET `/api/users`
- Response
  - 200 OK with `Map<String, User>` (keyed by user name). Returned objects are defensive copies.

### Delete user
- DELETE `/api/users/{name}`
- Responses
  - 200 OK `{ "message": "User deleted: {name}" }`
  - 404 Not Found

### Unlink Trakt
- DELETE `/api/users/{userName}/trakt`
- Responses
  - 200 OK `{ "message": "Trakt account unlinked successfully", "user": "...", "hasTraktAccount": false }`
  - 404 Not Found

## Movies

Notes
- GET endpoints do not sync. For Trakt‑linked users, call POST `/api/movies/{userName}/sync` first to refresh.

### Watched (flat list)
- GET `/api/movies/{userName}/watched`
- Responses
  - 200 OK with `List<Movie>` (manual + Trakt)
  - 404 Not Found

### All (categorized)
- GET `/api/movies/{userName}/all`
- Responses
  - 200 OK with `{ "traktMovies": Movie[], "manualMovies": Movie[], "allMovies": Movie[] }`
  - 404 Not Found

### Sync from Trakt
- POST `/api/movies/{userName}/sync`
- Responses
  - 200 OK `{ "message": "Trakt movies synced successfully", "user": "...", "totalMovies": 12, "traktMovies": 10, "manualMovies": 2 }`
  - 400 Bad Request if user has no linked Trakt account
  - 404 Not Found

### Add manual movie
- POST `/api/movies/{userName}/manual`
- Body
  - `{ "title": "The Matrix", "year": 1999, "userRating": 8 }`
  - Validation: `title` required non‑empty string; `year` required integer (numeric strings allowed); `userRating` optional integer 1..10.
- Responses
  - 200 OK with the stored movie
  - 400 Bad Request with `{ "error": "..." }` when validation fails
  - 404 Not Found

### Remove manual movie
- DELETE `/api/movies/{userName}/manual`
- Body
  - `{ "title": "The Matrix", "year": 1999 }`
- Responses
  - 200 OK `{ "message": "Movie removed successfully", "title": "...", "year": 1999 }`
  - 400 Bad Request `{ "error": "Movie not found" }` or validation error
  - 404 Not Found

## Data Models

### User
```json
{
  "name": "string",
  "manualMovies": [Movie],
  "traktMovies": [Movie],
  "traktAccount": TraktAccount | null
}
```

### TraktAccount
```json
{
  "accessToken": "string",
  "refreshToken": "string | null",
  "linkedAt": "ISO 8601 datetime",
  "traktUsername": "string | null",
  "traktUserId": "string | null"
}
```

### Movie (union)
- Manual movie
```json
{ "title": "string", "year": 1999, "userRating": 8 }
```
- Trakt movie (subset)
```json
{ "title": "string", "year": 2010, "userRating": 9, "ids": { "trakt": 12345, "imdb": "tt...", "tmdb": 27205, "slug": "..." } }
```

## Examples

### Create two users
```bash
curl -s -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"alice"}'
curl -s -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"bob"}'
```

### Link Trakt for alice (open in browser)
```text
http://localhost:8080/api/users/alice/trakt/link
```

### Add a manual movie for bob
```bash
curl -s -X POST localhost:8080/api/movies/bob/manual \
  -H 'Content-Type: application/json' \
  -d '{"title":"The Matrix","year":1999,"userRating":8}'
```

### (Optional) Manually sync alice from Trakt
```bash
curl -s -X POST localhost:8080/api/movies/alice/sync
```

### List all users
```bash
curl -s localhost:8080/api/users | jq
```

### Get alice’s movies (categorized)
```bash
curl -s localhost:8080/api/movies/alice/all | jq
```

## Notes

- User names are case-sensitive; URL‑encode when used in paths.
- In‑memory storage: data resets when the app restarts.
- Trakt OAuth settings (client id/secret, redirect URI, etc.) are in `src/main/resources/application.properties`.
- To link different Trakt accounts for different users, the app’s link flow requests `prompt=login`; you can also use a private/incognito window to ensure a fresh login at Trakt.

