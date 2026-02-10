package com.api.pethotelgo.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

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
            val idToken = getIdTokenFromRequest(request)
            if (idToken != null) {
                // Verify Firebase ID token
                val decodedToken = firebaseAuth.verifyIdToken(idToken)
                val email = decodedToken.email

                if (email != null) {
                    // Load user details from local database
                    // If user doesn't exist in local DB, createOrUpdate on first login
                    try {
                        val userDetails = userDetailsService.loadUserByUsername(email)
                        val authentication = UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities
                        )
                        SecurityContextHolder.getContext().authentication = authentication
                    } catch (_: Exception) {
                        // User not found in local database - could auto-create or require manual setup
                        // For now, log and continue (request will be denied by authorization rules)
                        logger.warn("Firebase user not found in local database: $email")
                    }
                }
            }
        } catch (e: FirebaseAuthException) {
            // Invalid or expired token - continue without authentication
            logger.debug("Firebase token verification failed: ${e.message}")
        } catch (e: Exception) {
            // Other errors - continue without authentication
            logger.debug("Authentication filter error: ${e.message}")
        }
        filterChain.doFilter(request, response)
    }

    private fun getIdTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}

