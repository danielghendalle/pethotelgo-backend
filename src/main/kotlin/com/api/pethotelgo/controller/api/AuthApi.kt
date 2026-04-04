package com.api.pethotelgo.controller.api

import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RefreshTokenRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

data class FirebaseTokenRequest(val idToken: String)

@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication endpoints (login, register, refresh token)")
interface AuthApi {

    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with email and password, returns Firebase custom token for client-side authentication",
        responses = [
            ApiResponse(responseCode = "200", description = "Login successful, Firebase custom token returned"),
            ApiResponse(responseCode = "401", description = "Invalid credentials or user not found"),
            ApiResponse(responseCode = "400", description = "Invalid email format")
        ]
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse>

    @PostMapping("/firebase-login")
    @Operation(
        summary = "Login with Firebase token",
        description = "Authenticate user using Firebase ID token",
        responses = [
            ApiResponse(responseCode = "200", description = "Firebase login successful"),
            ApiResponse(responseCode = "401", description = "Invalid Firebase token")
        ]
    )
    fun firebaseLogin(@RequestBody request: FirebaseTokenRequest): ResponseEntity<AuthResponse>

    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Create a new user account",
        responses = [
            ApiResponse(responseCode = "201", description = "User registered successfully"),
            ApiResponse(responseCode = "400", description = "Invalid input data"),
            ApiResponse(responseCode = "409", description = "Email already exists")
        ]
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse>

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Logout user",
        description = "Revoke all user tokens",
        responses = [
            ApiResponse(responseCode = "204", description = "Logout successful"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun logout(authentication: Authentication): ResponseEntity<Void>

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh token",
        description = "Get a new access token using refresh token",
        responses = [
            ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        ]
    )
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse>

    @GetMapping("/debug/token")
    @Operation(
        summary = "Debug token extraction",
        description = "Check if token is being extracted from headers",
        responses = [
            ApiResponse(responseCode = "200", description = "Token debug information")
        ]
    )
    fun debugToken(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, String>>

    @GetMapping("/debug/firebase")
    @Operation(
        summary = "Debug Firebase connection",
        description = "Check Firebase connection and list users (debug only)",
        responses = [
            ApiResponse(responseCode = "200", description = "Debug information")
        ]
    )
    fun debugFirebase(): ResponseEntity<Map<String, Any>>

    @GetMapping("/me")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Get current user",
        description = "Get authenticated user information",
        responses = [
            ApiResponse(responseCode = "200", description = "User information retrieved"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getCurrentUser(authentication: Authentication): ResponseEntity<String>

    @PostMapping("/sync-firebase-users")
    @Operation(
        summary = "Sync Firebase users to local database",
        description = "Create local users for all Firebase users that don't exist locally",
        responses = [
            ApiResponse(responseCode = "200", description = "Users synced successfully")
        ]
    )
    fun syncFirebaseUsers(): ResponseEntity<Map<String, Any>>
}

