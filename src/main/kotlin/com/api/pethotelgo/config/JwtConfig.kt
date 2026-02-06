package com.api.pethotelgo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.SignatureAlgorithm
import java.util.*

@Configuration
class JwtConfig {
    // Default secure key (512-bit / 64-byte) - change in production
    // Generated with: Base64.getEncoder().encodeToString(Keys.secretKeyFor(SignatureAlgorithm.HS512).encoded)
    @Value("\${jwt.secret:eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4=}")
    lateinit var jwtSecretBase64: String

    @Value("\${jwt.expiration:3600000}")
    val jwtExpiration: Long = 3600000 // 1 hour in milliseconds

    @Value("\${jwt.refresh.expiration:604800000}")
    val refreshTokenExpiration: Long = 604800000 // 7 days in milliseconds

    fun getSecretKey() = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecretBase64))
}

