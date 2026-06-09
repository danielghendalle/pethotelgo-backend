package com.api.pethotelgo.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val BEARER_PREFIX = "Bearer "
private const val AUTHORIZATION_HEADER = "Authorization"

@Component
class FirebaseAuthenticationFilter(
    private val firebaseAuth: FirebaseAuth,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val idToken = extractBearerToken(request)
            if (idToken != null) {
                authenticateWithFirebaseToken(idToken)
            }
        } catch (e: FirebaseAuthException) {
            logger.error("Firebase token verification failed: ${e.message} - ErrorCode: ${e.authErrorCode}")
        } catch (e: Exception) {
            logger.error("Authentication filter error: ${e.message}", e)
        }
        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        return if (bearerToken.startsWith(BEARER_PREFIX)) bearerToken.substring(BEARER_PREFIX.length) else null
    }

    private fun authenticateWithFirebaseToken(token: String) {
        var email: String? = null

        try {
            email = firebaseAuth.verifyIdToken(token).email
        } catch (e: FirebaseAuthException) {
            try {
                val uid = extractUidFromCustomToken(token)
                    ?: throw IllegalArgumentException("Unable to extract UID from custom token")
                email = firebaseAuth.getUser(uid).email
            } catch (customTokenError: FirebaseAuthException) {
                logger.error("Custom token verification failed: ${customTokenError.message}")
                throw customTokenError
            }
        }

        if (email == null) {
            throw IllegalArgumentException("Unable to extract email from token")
        }

        try {
            val userDetails = userDetailsService.loadUserByUsername(email)
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
        } catch (_: UsernameNotFoundException) {
            logger.warn("Firebase user not found in local database: $email")
            throw UsernameNotFoundException("User $email not found in local database")
        }
    }

    private fun extractUidFromCustomToken(customToken: String): String? {
        return try {
            val parts = customToken.split(".")
            if (parts.size >= 2) {
                val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                """"uid":\s*"([^"]+)"""".toRegex().find(payload)?.groupValues?.get(1)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
