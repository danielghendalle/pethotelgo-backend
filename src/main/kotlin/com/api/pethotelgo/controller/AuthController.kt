package com.api.pethotelgo.controller

import com.api.pethotelgo.controller.api.AuthApi
import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RefreshTokenRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import com.api.pethotelgo.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(private val authService: AuthService) : AuthApi {

    override fun login(request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    override fun register(request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    override fun logout(authentication: Authentication): ResponseEntity<Void> {
        authService.logout(authentication.name)
        return ResponseEntity.noContent().build()
    }

    override fun refreshToken(request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    override fun getCurrentUser(authentication: Authentication): ResponseEntity<String> {
        return ResponseEntity.ok("Authenticated as: ${authentication.name}")
    }
}


