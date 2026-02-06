package com.api.pethotelgo.service

import com.api.pethotelgo.model.dto.AuthResponse
import com.api.pethotelgo.model.dto.LoginRequest
import com.api.pethotelgo.model.dto.RegisterRequest
import com.api.pethotelgo.model.entity.User

interface AuthService {
    fun login(request: LoginRequest): AuthResponse
    fun register(request: RegisterRequest): AuthResponse
    fun logout(userId: String)
    fun refreshToken(refreshToken: String): AuthResponse
    fun validateCredentials(email: String, password: String): User
}

