package com.api.pethotelgo.service.impl

import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import com.api.pethotelgo.model.dto.UserDTO
import com.api.pethotelgo.model.entity.User
import com.api.pethotelgo.model.entity.RefreshToken
import com.api.pethotelgo.model.enums.UserRole
import com.api.pethotelgo.repository.UserRepository
import com.api.pethotelgo.repository.RefreshTokenRepository
import com.api.pethotelgo.security.JwtTokenProvider
import com.api.pethotelgo.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
import java.security.MessageDigest

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder
) : AuthService {

    override fun login(request: LoginRequest): AuthResponse {
        val user = validateCredentials(request.email, request.password)
        val accessToken = jwtTokenProvider.generateToken(user)
        val refreshToken = createRefreshToken(user)

        return AuthResponse(
            user = user.toDTO(),
            token = accessToken,
            refreshToken = refreshToken.token ?: ""
        )
    }

    override fun register(request: RegisterRequest): AuthResponse {
        validateRegisterRequest(request)

        if (userRepository.existsByEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")
        }

        val user = User(
            email = request.email,
            name = request.name,
            passwordHash = passwordEncoder.encode(request.password) ?: "",
            role = UserRole.USER
        )

        userRepository.save(user)

        val accessToken = jwtTokenProvider.generateToken(user)
        val refreshToken = createRefreshToken(user)

        return AuthResponse(
            user = user.toDTO(),
            token = accessToken,
            refreshToken = refreshToken.token ?: ""
        )
    }

    override fun logout(userId: String) {
        val tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
        tokens.forEach { it.revokedAt = Instant.now() }
        refreshTokenRepository.saveAll(tokens)
    }

    override fun refreshToken(refreshToken: String): AuthResponse {
        val tokenHash = hashToken(refreshToken)
        var token = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null)

        // Fallback: check plaintext token for backward compatibility and migrate
        if (token == null) {
            val plaintext = refreshTokenRepository.findByToken(refreshToken)
            if (plaintext.isPresent) {
                token = plaintext.get()
                // Migrate: set token_hash and remove plaintext token to avoid storing raw tokens
                token.tokenHash = tokenHash
                token.token = null
                refreshTokenRepository.save(token)
            }
        }

        if (token == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
        }

        if (!token.isValid()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked")
        }

        val user = token.user ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val newAccessToken = jwtTokenProvider.generateToken(user)

        return AuthResponse(
            user = user.toDTO(),
            token = newAccessToken,
            refreshToken = refreshToken
        )
    }

    override fun validateCredentials(email: String, password: String): User {
        val user = userRepository.findByEmail(email)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password") }

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive")
        }

        return user
    }

    private fun createRefreshToken(user: User): RefreshToken {
        val oldTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.id)
        oldTokens.forEach { it.revokedAt = Instant.now() }
        refreshTokenRepository.saveAll(oldTokens)

        val rawToken = UUID.randomUUID().toString()
        val tokenHash = hashToken(rawToken)

        val refreshToken = RefreshToken(
            token = null,
            tokenHash = tokenHash,
            user = user,
            expiresAt = Instant.now().plusSeconds(604800)
        )
        val saved = refreshTokenRepository.save(refreshToken)
        // set token only for returning to user (not stored)
        saved.token = rawToken
        return saved
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        if (request.name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required")
        }

        if (request.name.length > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot exceed 100 characters")
        }

        if (request.email.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required")
        }

        if (!isValidEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format")
        }

        if (request.password.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required")
        }

        if (request.password.length < 6) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters")
        }

        if (request.password.length > 50) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot exceed 50 characters")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$".toRegex()
        return emailPattern.matches(email)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(token.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun User.toDTO(): UserDTO = UserDTO(
        id = this.id,
        email = this.email,
        name = this.name,
        role = this.role.name
    )
}
