package com.api.pethotelgo.service.impl

import com.api.pethotelgo.exception.ConflictException
import com.api.pethotelgo.exception.ValidationException
import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import com.api.pethotelgo.model.entity.User
import com.api.pethotelgo.model.enums.UserRole
import com.api.pethotelgo.repository.RefreshTokenRepository
import com.api.pethotelgo.repository.UserRepository
import com.api.pethotelgo.service.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import com.google.firebase.auth.UserRecord
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val firebaseAuth: FirebaseAuth
) : AuthService {

    override fun login(request: LoginRequest): AuthResponse {
        try {
            // First verify user exists in local database
            val user = validateUserExists(request.email)
            
            // Then verify user exists in Firebase
            val firebaseUser = firebaseAuth.getUserByEmail(request.email)
            
            // Create custom token for client-side authentication
            val customToken = firebaseAuth.createCustomToken(firebaseUser.uid)
            
            return AuthResponse(
                user = user.toDTO(),
                token = customToken,
                refreshToken = ""
            )
        } catch (e: FirebaseAuthException) {
            println("Firebase Auth Error: ${e.message} - Code: ${e.authErrorCode}")
            when (e.authErrorCode?.name) {
                "USER_NOT_FOUND" -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in Firebase. Please register first.")
                "INVALID_EMAIL" -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format")
                "USER_DISABLED" -> throw ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled")
                else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Firebase authentication error: ${e.message}")
            }
        } catch (e: ResponseStatusException) {
            // Re-throw ResponseStatusException as is
            throw e
        } catch (e: Exception) {
            println("General Auth Error: ${e.message}")
            e.printStackTrace()
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: ${e.message}")
        }
    }

    override fun register(request: RegisterRequest): AuthResponse {
        validateRegisterRequest(request)

        // Check if email already exists in local DB
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already registered")
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

            val savedUser = userRepository.save(user)

            return AuthResponse(
                user = savedUser.toDTO(),
                token = "user-created-in-firebase",
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

    override fun verifyFirebaseToken(idToken: String): User {
        try {
            val decodedToken: FirebaseToken = firebaseAuth.verifyIdToken(idToken)
            val firebaseUid = decodedToken.uid
            val email = decodedToken.email ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email not found in token")
            
            // Find user by Firebase UID or email
            return userRepository.findByEmail(email)
                .orElseGet {
                    // Create user if not exists (optional - depends on your business logic)
                    val newUser = User(
                        email = email,
                        name = decodedToken.name ?: "Unknown",
                        passwordHash = firebaseUid, // Store Firebase UID
                        role = UserRole.USER
                    )
                    userRepository.save(newUser)
                }
        } catch (e: FirebaseAuthException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Firebase token: ${e.message}")
        }
    }

    override fun getUserByFirebaseUid(firebaseUid: String): User {
        return userRepository.findAll()
            .find { it.passwordHash == firebaseUid }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for Firebase UID")
    }

    override fun debugFirebase(): Map<String, Any> {
        return try {
            val users = firebaseAuth.listUsers(null)
            val userList = mutableListOf<Map<String, Any>>()
            
            users.iterateAll().forEach { user ->
                userList.add(mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to (user.displayName ?: ""),
                    "disabled" to user.isDisabled
                ))
            }
            
            mapOf(
                "firebaseConnected" to true,
                "totalUsers" to userList.size,
                "users" to userList
            )
        } catch (e: Exception) {
            mapOf(
                "firebaseConnected" to false,
                "error" to (e.message ?: "Unknown error"),
                "errorType" to e.javaClass.simpleName
            )
        }
    }

    override fun syncFirebaseUsers(): Map<String, Any> {
        return try {
            val firebaseUsers = firebaseAuth.listUsers(null)
            val syncedUsers = mutableListOf<String>()
            val skippedUsers = mutableListOf<String>()
            
            firebaseUsers.iterateAll().forEach { firebaseUser ->
                val email = firebaseUser.email
                if (email != null) {
                    if (!userRepository.existsByEmail(email)) {
                        val newUser = User(
                            email = email,
                            name = firebaseUser.displayName ?: "Unknown",
                            passwordHash = firebaseUser.uid, // Store Firebase UID
                            role = UserRole.USER
                        )
                        userRepository.save(newUser)
                        syncedUsers.add(email)
                    } else {
                        skippedUsers.add(email)
                    }
                }
            }
            
            mapOf(
                "success" to true,
                "syncedUsers" to syncedUsers,
                "skippedUsers" to skippedUsers,
                "totalSynced" to syncedUsers.size,
                "totalSkipped" to skippedUsers.size
            )
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Unknown error"),
                "errorType" to e.javaClass.simpleName
            )
        }
    }

    private fun validateUserExists(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found") }
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        if (request.name.isBlank()) {
            throw ValidationException("Name is required")
        }

        if (request.name.length > 100) {
            throw ValidationException("Name cannot exceed 100 characters")
        }

        if (request.email.isBlank()) {
            throw ValidationException("Email is required")
        }

        if (!isValidEmail(request.email)) {
            throw ValidationException("Invalid email format")
        }

        if (request.password.isBlank()) {
            throw ValidationException("Password is required")
        }

        if (request.password.length < 6) {
            throw ValidationException("Password must be at least 6 characters")
        }

        if (request.password.length > 50) {
            throw ValidationException("Password cannot exceed 50 characters")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$".toRegex()
        return emailPattern.matches(email)
    }
}

