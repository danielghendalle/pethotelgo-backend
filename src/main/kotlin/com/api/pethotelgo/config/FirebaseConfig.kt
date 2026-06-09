package com.api.pethotelgo.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.Base64

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials.path:}")
    private val firebaseCredentialsPath: String
) {

    @Bean
    fun firebaseAuth(): FirebaseAuth {
        if (FirebaseApp.getApps().isEmpty()) {
            val credentials = resolveCredentials()
            FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build())
        }
        return FirebaseAuth.getInstance()
    }

    private fun resolveCredentials(): GoogleCredentials {
        // 1. Prefer base64-encoded JSON in environment variable (best for cloud deployments)
        val base64 = System.getenv("FIREBASE_CREDENTIALS_BASE64")
        if (!base64.isNullOrBlank()) {
            val jsonBytes = Base64.getDecoder().decode(base64)
            return GoogleCredentials.fromStream(ByteArrayInputStream(jsonBytes))
        }

        // 2. Fall back to file path (local dev or Docker volume)
        if (firebaseCredentialsPath.isNotBlank()) {
            return GoogleCredentials.fromStream(FileInputStream(firebaseCredentialsPath))
        }

        // 3. Last resort: Application Default Credentials (GCP / Cloud Run)
        return GoogleCredentials.getApplicationDefault()
    }
}
