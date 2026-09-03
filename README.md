# PetHotelGO — API

REST backend for a **pet hotel / pet boarding management system**: clients
(owners), their pets, boarding reservations with capacity control and per-stay
pricing, stay history, and vaccination cards. Consumed by a separate React
frontend (`../pethotelgo-frontend`).

## Stack

| | |
| --- | --- |
| Language / runtime | **Kotlin 2.2.21**, Java 17 |
| Framework | **Spring Boot 4.0.2** (Web, Security, Data JPA, Actuator) |
| Database | **MySQL 8+** |
| Migrations | **Flyway** (`src/main/resources/db/migration/`) |
| Auth | Self-issued **JWT** (HMAC-SHA, `io.jsonwebtoken:jjwt`) — no external IdP |
| API docs | springdoc-openapi (Swagger UI, dev only) |
| Build | Maven (`./mvnw`) |
| Deploy | Docker; OCI Always Free (see `../DEPLOY.md`) |

## Architecture

**Controller → Service → Repository**, DTOs at the edges (entities never leave
the service layer).

```
src/main/kotlin/com/api/pethotelgo/
├── controller/        REST endpoints (thin) — impl of the interfaces in controller/api/
│   └── api/           @RequestMapping interfaces (also drive the OpenAPI docs)
├── service/           business logic (interface) + service/impl/ (implementation)
├── repository/        Spring Data JPA repositories
├── model/
│   ├── entity/        @Entity classes (JPA)
│   ├── dto/           request/response payloads
│   └── enums/         PetSize, SociabilityLevel, ReservationStatus, UserRole
├── security/          JwtService (mints tokens) + JwtAuthenticationFilter (validates)
├── config/            WebSecurityConfig, SwaggerConfig
└── exception/         GlobalExceptionHandler + typed ErrorCode
```

### Authentication

- **Access token** — short-lived JWT, `Authorization: Bearer <token>`, subject =
  user id. Minted by `security/JwtService.kt`.
- `JwtAuthenticationFilter` re-reads the user from the DB on every request, so
  deactivating an account takes effect immediately.
- **Refresh token** — opaque 256-bit random value (not a JWT); only its SHA-256
  digest is stored. Every `POST /auth/refresh` rotates it; replaying a rotated
  token revokes the user's whole token family.
- **Passwords** — BCrypt. Emails normalized to lowercase.
- Public endpoints: `/auth/register`, `/auth/login`, `/auth/refresh`,
  `/actuator/health` (+ Swagger in dev). Everything else → 401 without a valid token.
- `app.jwt.secret` must decode to ≥ 32 bytes or the app won't start. In prod
  there is no default — `JWT_SECRET` is mandatory.

### Database & migrations

Flyway owns the schema; Hibernate runs `ddl-auto=validate` against it in `dev`
and `prod`. On MySQL the type match is strict — mirror what Hibernate expects:

| Kotlin / mapping | MySQL column |
| --- | --- |
| `@Enumerated(STRING)` | `ENUM('a','b',…)` (values in Hibernate's order) |
| `Boolean` | `BIT` |
| `LocalDateTime` / `Instant` | `DATETIME(6)` |
| `@Column(columnDefinition = "text")` | `TEXT` |
| `@Column(columnDefinition = "LONGTEXT")` | `LONGTEXT` (base64 vaccination cards) |

When adding a column, generate the DDL Hibernate wants
(`jakarta.persistence.schema-generation.scripts.action=create`) and copy it into
a new `V<n>__*.sql`. Migrations are append-only.

Current migrations: `V1__baseline_schema.sql` (full schema at the MySQL cut-over),
`V2__app_settings.sql` (operator-editable `app_settings` — the boarding daily rates).

### Global settings

`app_settings` is a single row (`id = 1`) exposed via `GET`/`PUT /settings`
(`SettingsController` / `AppSettingsService`). Today it holds `dailyRateStandard`
(small/medium pets) and `dailyRateLarge` (large pets). `ReservationServiceImpl`
uses `dailyRateFor(pet.size)` as the default when a reservation is created
without an explicit `dailyRate`.

## Local development

**Prerequisites:** Java 17+, Docker (for MySQL).

```bash
docker compose up -d                 # MySQL on localhost:3306
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

API at `http://localhost:8080/api`. Swagger UI at `/api/swagger-ui.html` (dev).

**Profiles**
- `dev` — `ddl-auto=validate` + Flyway, Swagger on, dev-only JWT secret baked in.
- `prod` — same, Swagger off, all secrets from env vars, `forward-headers-strategy=framework`.
- no profile — `ddl-auto=create-drop` + Flyway off (throwaway schema from entities).

Config: `application.properties` (base) + `application-{dev,prod}.properties`.
`application-dev.properties` is git-ignored — override the datasource there or via
`JDBC_DATABASE_*` env vars.

**Build:**
```bash
./mvnw clean package -DskipTests      # -> target/pethotelgo-*.jar
```

## Environment variables (prod)

| var | notes |
| --- | --- |
| `JDBC_DATABASE_URL` | `jdbc:mysql://host:3306/pethotel` |
| `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` | DB credentials |
| `JWT_SECRET` | `openssl rand -base64 64` — mandatory, ≥ 32 bytes decoded |
| `CORS_ALLOWED_ORIGINS` | comma-separated; empty ⇒ permissive `allowedOriginPatterns=*` |
| `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | ms, default 1 h / 7 d |
| `HIKARI_MAX_POOL_SIZE` | default 10 (5 on the small prod VM) |

See `.env.backend.example`.

## Deploy

See **`../DEPLOY.md`** at the workspace root — one runbook for both stacks.
`docker-compose.backend.yml` runs just this API against the managed MySQL;
`scripts/provision-mysql-oci.sh` provisions the OCI MySQL HeatWave instance.
