Security review for PetHotelGo

Summary
-------
This document summarizes findings from an automated review of the codebase, highlights security issues and gaps, and lists recommended fixes and mitigations for production.

High-priority findings
----------------------
1. Refresh tokens are stored as plain tokens in the database.
   - File: `src/main/kotlin/com/api/pethotelgo/model/entity/RefreshToken.kt`
   - Risk: If DB is compromised, attacker can use refresh tokens to mint new access tokens.
   - Recommendation: Store only a hashed version (e.g. HMAC or SHA-256) of the refresh token; on refresh, hash the submitted token and compare. Rotate existing refresh tokens or force re-login.

2. CORS default allows `*` when `app.cors.allowed-origins` is empty.
   - File: `WebSecurityConfig.kt`
   - Risk: In production, if env var isn't set, all origins allowed, which may be insecure.
   - Recommendation: Fail fast or default to no origins allowed unless env var is explicitly set. We currently document this in `application-prod.properties` and allow operator to set allowed origins; consider changing default to denyAll.

3. Swagger exposure
   - File: `WebSecurityConfig.kt` + `application-prod.properties`
   - Risk: API docs can expose endpoints; in production they should be disabled or protected.
   - Recommendation: Keep disabled in `application-prod.properties` (done), and only enable for internal staging with a secure firewall.

4. Rate limiting & brute-force protection
   - Finding: only placeholder properties exist; no enforcement.
   - Risk: Credential stuffing, brute-force attacks.
   - Recommendation: Implement rate limiting (API gateway / load balancer / Spring filter) on login and register endpoints. Implement exponential backoff / account lockout.

5. JWT secret management
   - Finding: JWT secret must be provided as Base64 env var `JWT_SECRET` (documented).
   - Risk: Secret leakage if checked into VCS.
   - Recommendation: Use secrets manager / environment variables in production; never commit keys to repo.

6. HTTPS enforcement
   - Recommendation: Terminate TLS at the load balancer and ensure `X-Forwarded-Proto` is validated. Configure `security.require-ssl` or rely on infrastructure.

Medium/Low priority findings
---------------------------
- H2 console disabled in production (good). Ensure it remains disabled.
- Password hashing uses BCrypt (good). Consider increasing work factor if computation budget allows.
- No CSRF issue for API (stateless JWT), but if any cookie-based auth is added, enable CSRF.
- Input validation is present in registration but consider server-side validation on all endpoints (reservations, pets) to avoid injection/overflows.
- Log sanitization: ensure no secrets are logged. I found no `System.out.println` or logger prints of sensitive values in core code; docs include examples—OK.

Implementation recommendations (concrete)
---------------------------------------
1. Hash refresh tokens before storage
   - When creating refresh token: store `sha256(token)` instead of token.
   - Return raw token to client only once.
   - On refresh: hash provided token and look up hashed value.
   - Add `revokedAt` and `expiresAt` checks (already implemented).

2. Enforce CORS strictness
   - Default `app.cors.allowed-origins` to empty and fail the app start if not set in prod profile.
   - Alternatively, change production default to explicit allowed origins.

3. Implement rate limiting for login/register
   - Use Redis-based sliding window counter or API gateway rule; for simple case, store counter in DB with TTL.

4. Protect Swagger and Actuator in production (already configured in `application-prod.properties`)

5. Secrets & configuration
   - Use environment variables, Kubernetes secrets, or cloud secrets manager to store `JWT_SECRET`, DB credentials.

6. TLS and security headers
   - Ensure `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security` headers are applied by reverse proxy or via Spring Security config.

7. Database migrations
   - Add Flyway or Liquibase for schema migrations instead of relying on `hibernate.ddl-auto` in production. We currently use `validate` in `application-prod.properties`.

8. Logging
   - Keep `spring.jpa.show-sql=false` in production. Avoid logging request bodies containing PII.

Next steps I can take now
------------------------
- Implement refresh token hashing (non-breaking for new installs, but requires migration/rotation for existing tokens). I can implement hashing logic in `AuthServiceImpl.createRefreshToken` and `refreshTokenRepository.findByToken` usage by adding a `findByTokenHash` method and hashing submitted token before query.
- Enforce CORS default deny by failing app startup when `app.cors.allowed-origins` is blank and profile=prod.

Do you want me to:
A) Implement refresh-token hashing and the code changes now (this will change DB contents for new tokens and require rotation for old tokens), or
B) Only produce the `application-prod.properties` and the security report (done), leaving token hashing as a manual future step?




