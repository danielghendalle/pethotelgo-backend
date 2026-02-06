package com.api.pethotelgo.controller

import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import com.api.pethotelgo.model.dto.RefreshTokenRequest
import com.api.pethotelgo.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication endpoints (login, register, refresh token)")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with email and password",
        responses = [
            ApiResponse(responseCode = "200", description = "Login successful"),
            ApiResponse(responseCode = "401", description = "Invalid credentials")
        ]
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

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
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

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
    fun logout(authentication: Authentication): ResponseEntity<Void> {
        authService.logout(authentication.name)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh token",
        description = "Get a new access token using refresh token",
        responses = [
            ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        ]
    )
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }

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
    fun getCurrentUser(authentication: Authentication): ResponseEntity<String> {
        return ResponseEntity.ok("Authenticated as: ${authentication.name}")
    }
}



