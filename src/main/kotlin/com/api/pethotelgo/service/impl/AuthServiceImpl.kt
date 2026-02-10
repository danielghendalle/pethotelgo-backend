package com.api.pethotelgo.service.impl

import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import com.api.pethotelgo.model.dto.UserDTO
import com.api.pethotelgo.model.entity.User
import com.api.pethotelgo.model.enums.UserRole
import com.api.pethotelgo.repository.UserRepository
import com.api.pethotelgo.repository.RefreshTokenRepository
import com.api.pethotelgo.service.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.security.MessageDigest

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val firebaseAuth: FirebaseAuth
) : AuthService {

    override fun login(request: LoginRequest): AuthResponse {
        // Validate credentials on Firebase side
        // Note: Firebase Admin SDK cannot validate password directly
        // Clients should authenticate with Firebase client SDK and send ID token
        // This endpoint is for backward compatibility or server-side auth flows

        val user = validateUserExists(request.email)

        // Return user data - client should use Firebase to get ID token
        return AuthResponse(
            user = user.toDTO(),
            token = "firebase-id-token-from-client",
            refreshToken = ""
        )
    }

    override fun register(request: RegisterRequest): AuthResponse {
        validateRegisterRequest(request)

        // Check if email already exists in local DB
        if (userRepository.existsByEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")
        }

        try {
            // Create user in Firebase
            // Using the proper Firebase Admin SDK syntax
            val userRecord = firebaseAuth.createUser(
                UserRecord.CreateRequest()
                    .setEmail(request.email)
                    .setPassword(request.password)
                    .setDisplayName(request.name)
                    .setDisabled(false)
            )

            // Create user in local database
            val user = User(
                email = request.email,
                name = request.name,
                passwordHash = userRecord.uid, // Store Firebase UID instead of password hash
                role = UserRole.USER
            )

            userRepository.save(user)

            return AuthResponse(
                user = user.toDTO(),
                token = "firebase-id-token-from-client",
                refreshToken = ""
            )
        } catch (e: FirebaseAuthException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Failed to create Firebase user: ${e.message}"
            )
        }
    }

    override fun logout(userId: String) {
        val tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
        tokens.forEach { it.revokedAt = Instant.now() }
        refreshTokenRepository.saveAll(tokens)
    }

    override fun refreshToken(refreshToken: String): AuthResponse {
        // With Firebase, refresh tokens are handled by Firebase client SDK
        // This endpoint is for backward compatibility
        throw ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "Token refresh is managed by Firebase client SDK. Use Firebase to refresh tokens."
        )
    }

    override fun validateCredentials(email: String, password: String): User {
        return validateUserExists(email)
    }

    private fun validateUserExists(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found") }
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


    private fun User.toDTO(): UserDTO = UserDTO(
        id = this.id,
        email = this.email,
        name = this.name,
        role = this.role.name
    )
}

