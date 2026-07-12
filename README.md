# TinyLink

A URL shortener built for learning system design concepts using Spring Boot backend, Angular frontend, Redis caching,
and Docker Compose for local orchestration.

## Features

- **Shorten URLs** with randomly generated Base62 codes or custom aliases
- **Set expiration** on short links with automatic scheduled cleanup
- **302 redirects** with Redis cache-aside lookup for fast resolution
- **Click analytics** track total clicks, clicks by referrer, by hour, and by day
- **IP-based rate limiting** with dual sliding windows (per-minute + per-hour), backed by Redis with in-memory fallback
- **Soft deletes** deactivate links without losing historical data
- **Dockerized** one command to spin up frontend, backend, and Redis

## Architecture

The system has two core flows:

**URL Creation** the frontend sends `POST /api/shorten`. The backend extracts the client IP, checks the rate limiter,
validates the URL format, generates or accepts a short code, stores the mapping in an in-memory `ConcurrentHashMap`,
caches it in Redis, and returns the shortened URL.

**URL Redirection** when a user opens `/api/{shortCode}`, the backend checks the Redis cache first. On a miss, it falls
back to in-memory storage, checks expiry, records a click event, and returns an HTTP 302 redirect to the original URL.

```
┌─────────┐     POST /api/shorten      ┌──────────────────┐      ┌───────────┐
│ Angular │ ──────────────────────────▶ │  Spring Boot API │ ───▶ │   Redis   │
│   SPA   │                             │                  │      │  (cache)  │
│         │ ◀────────────────────────── │  In-Memory Store │      └───────────┘
└─────────┘     short URL + metadata    │  (ConcurrentMap) │
                                        └──────────────────┘
```

## Tech Stack

| Layer    | Technology                       |
|----------|----------------------------------|
| Backend  | Java 17, Spring Boot 4.1, Lombok |
| Cache    | Redis 7 (Lettuce client)         |
| Frontend | Angular (standalone component)   |
| Infra    | Docker Compose                   |

## Quick Start

```bash
docker compose up --build
```

- **Frontend UI** → [http://localhost:8082](http://localhost:8082)
- **Backend API** → [http://localhost:8080](http://localhost:8080)
- **Redis** → `localhost:6379`

To stop everything:

```bash
docker compose down
```

## API Reference

### Create Short URL

```http
POST /api/shorten
Content-Type: application/json

{
  "originalUrl": "https://example.com/very/long/url",
  "customAlias": "my-link",          // optional
  "expiresAt": "2026-12-31T23:59:59" // optional
}
```

**Response (200):**

```json
{
  "shortUrl": "http://localhost:8080/api/abc123",
  "shortCode": "abc123",
  "originalUrl": "https://example.com/very/long/url",
  "createdAt": "2026-07-12T10:30:00",
  "expiresAt": "2026-12-31T23:59:59"
}
```

**Rate limited (429):**

```json
{
  "error": "Rate limit exceeded",
  "remainingRequests": 0,
  "timeUntilReset": 45
}
```

### Redirect

```http
GET /api/{shortCode}
→ HTTP 302 (Location: original URL)
```

### Get Stats

```http
GET /api/stats/{shortCode}
```

Returns click count, creation time, expiry, active status, and creator IP.

### Get Analytics

```http
GET /api/analytics/{shortCode}
```

Returns total clicks, last 10 click events, and aggregated data: clicks by referrer, by hour of day, and by date.

### Delete

```http
DELETE /api/{shortCode}
```

Soft-deletes the link (marks it inactive, removes from cache).

## Project Structure

```
TinyLink/
├── backend/
│   └── src/main/java/com/shahbytes/tinylink/
│       ├── config/
│       │   ├── RedisConfig.java          # RedisTemplate with JSON serialization
│       │   ├── WebConfig.java            # CORS configuration
│       │   └── CleanupScheduler.java     # Scheduled expired-URL sweep
│       ├── controllers/
│       │   ├── UrlShortenerController.java  # REST endpoints
│       │   └── UrlStatsResponse.java        # Stats response DTO
│       ├── dto/
│       │   ├── ShortenUrlRequest.java    # Input with validation
│       │   ├── ShortenUrlResponse.java   # Creation response
│       │   └── UrlAnalyticsResponse.java # Detailed analytics response
│       ├── models/
│       │   ├── UrlData.java              # Core URL entity
│       │   ├── ClickEvent.java           # Individual click record
│       │   └── RateLimitData.java        # Per-IP rate limit state
│       └── services/
│           ├── UrlShortenerService.java  # URL CRUD, caching, analytics
│           └── RateLimitService.java     # Dual-window rate limiting
├── frontend/
│   └── src/
│       ├── app/app.component.ts
│       └── main.ts
├── docker-compose.yml
└── README.md
```

## Configuration

All backend settings live in `application.yaml`:

| Property                                  | Default                 | Description                                 |
|-------------------------------------------|-------------------------|---------------------------------------------|
| `tinylink.base-url`                       | `http://localhost:8080` | Base URL for generated short links          |
| `tinylink.short-code.length`              | `6`                     | Length of generated Base62 codes            |
| `tinylink.short-code.max-attempts`        | `10`                    | Retries before giving up on unique code     |
| `tinylink.rate-limit.requests-per-minute` | `2`                     | Max shortening requests per IP per minute   |
| `tinylink.rate-limit.requests-per-hour`   | `10`                    | Max shortening requests per IP per hour     |
| `tinylink.cache.ttl-minutes`              | `30`                    | Redis cache TTL for URL lookups             |
| `tinylink.cleanup.interval-minutes`       | `1`                     | How often the scheduler sweeps expired URLs |

## How Redis Is Used

Redis serves two purposes in this project, both as a supporting layer rather than the source of truth:

**URL Cache** — keys follow the pattern `url:{shortCode}` and store the original URL string with a configurable TTL.
This implements a cache-aside pattern: check Redis first on redirect, fall back to in-memory, and re-cache on miss.

**Rate Limit State** — keys follow the pattern `ratelimit:{clientIp}` and store serialized `RateLimitData` objects with
a 1-hour TTL. This allows rate-limit state to be shared across multiple app instances. If Redis is unavailable, the
service falls back to a local `ConcurrentHashMap`.

## Current Limitations

This is a learning project, not production-grade:

- All data lives in `ConcurrentHashMap` — lost on restart
- No persistent database
- Rate-limit updates are not atomic
- No authentication or URL ownership
- Click analytics are memory-only
- Soft deletes don't provide audit history

## Ideas for Extending

- Replace in-memory maps with PostgreSQL or MongoDB
- Use Redis atomic counters or Lua scripts for rate limiting
- Add user authentication and URL ownership
- Persist click analytics to a time-series store
- Support custom domains
- Add integration tests
- Add observability with Micrometer/Prometheus

## Development

**Backend** (requires Java 17+, Maven, Redis running on 6379):

```bash
cd backend
mvn spring-boot:run
```

**Frontend** (requires Node.js):

```bash
cd frontend
npm install
npm start
```

Frontend dev server runs at [http://localhost:4200](http://localhost:4200).

## License

This project is for educational purposes.