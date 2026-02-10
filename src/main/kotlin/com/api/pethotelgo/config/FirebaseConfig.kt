package com.api.pethotelgo.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials.path:}")
    private val firebaseCredentialsPath: String
) {

    @Bean
    fun firebaseAuth(): FirebaseAuth {
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps().isEmpty()) {
            val options = if (firebaseCredentialsPath.isNotBlank()) {
                // Load from JSON file (e.g., service account key)
                val serviceAccount = FileInputStream(firebaseCredentialsPath)
                val credentials = GoogleCredentials.fromStream(serviceAccount)
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()
            } else {
                // Use Application Default Credentials (ADC)
                // Requires GOOGLE_APPLICATION_CREDENTIALS environment variable or gcloud auth
                val credentials = GoogleCredentials.getApplicationDefault()
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()
            }
            FirebaseApp.initializeApp(options)
        }

        return FirebaseAuth.getInstance()
    }
}

