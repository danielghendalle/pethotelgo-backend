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
            logger.debug("Request to: ${request.requestURI}")
            logger.debug("Authorization header: ${request.getHeader(AUTHORIZATION_HEADER)}")
            
            if (idToken != null) {
                logger.debug("Attempting to authenticate with Firebase token")
                authenticateWithFirebaseToken(idToken)
            } else {
                logger.debug("No Bearer token found in request")
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
        return if (bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    private fun authenticateWithFirebaseToken(token: String) {
        logger.debug("Verifying Firebase token...")
        
        // Try to verify as ID token first, then as custom token
        var email: String? = null
        var isCustomToken = false
        
        try {
            val decodedToken = firebaseAuth.verifyIdToken(token)
            email = decodedToken.email
            logger.debug("Token verified as ID token")
        } catch (e: FirebaseAuthException) {
            logger.debug("ID token verification failed, trying as custom token...")
            try {
                // For custom tokens, extract UID and get user
                val uid = extractUidFromCustomToken(token)
                if (uid != null) {
                    val user = firebaseAuth.getUser(uid)
                    email = user.email
                    isCustomToken = true
                    logger.debug("Token verified as custom token for user: ${user.email}")
                } else {
                    throw IllegalArgumentException("Unable to extract UID from custom token")
                }
            } catch (customTokenError: FirebaseAuthException) {
                logger.error("Custom token verification failed: ${customTokenError.message}")
                throw customTokenError
            }
        }
        
        if (email == null) {
            throw IllegalArgumentException("Unable to extract email from token")
        }
        
        logger.debug("Token verified for email: $email (Custom: $isCustomToken)")

        try {
            val userDetails = userDetailsService.loadUserByUsername(email)
            val authentication = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            SecurityContextHolder.getContext().authentication = authentication
            logger.debug("Authentication successful for user: $email")
        } catch (_: UsernameNotFoundException) {
            logger.warn("Firebase user not found in local database: $email")
            throw UsernameNotFoundException("User $email not found in local database")
        }
    }
    
    private fun extractUidFromCustomToken(customToken: String): String? {
        return try {
            // Custom tokens are JWT, we can decode the payload without verification
            val parts = customToken.split(".")
            if (parts.size >= 2) {
                val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                // Parse JSON manually to avoid dependency issues
                val uidPattern = """"uid":\s*"([^"]+)"""".toRegex()
                val match = uidPattern.find(payload)
                match?.groupValues?.get(1)
            } else null
        } catch (e: Exception) {
            logger.debug("Failed to extract UID from custom token: ${e.message}")
            null
        }
    }
}


