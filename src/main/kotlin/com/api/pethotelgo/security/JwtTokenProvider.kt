package com.api.pethotelgo.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import com.api.pethotelgo.config.JwtConfig
import java.util.*

@Component
class JwtTokenProvider(private val jwtConfig: JwtConfig) {

    private val secretKey = jwtConfig.getSecretKey()

    fun generateToken(userDetails: UserDetails): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtConfig.jwtExpiration)

        return Jwts.builder()
            .subject(userDetails.username)
            .claim("roles", userDetails.authorities.map { it.authority })
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getExpirationDateFromToken(token: String): Date {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .expiration
    }
}

