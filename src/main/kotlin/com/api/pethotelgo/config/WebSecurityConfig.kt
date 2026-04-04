package com.api.pethotelgo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import com.api.pethotelgo.security.FirebaseAuthenticationFilter

private const val CORS_ALLOW_ALL = "/**"
private const val CONTENT_TYPE = "Content-Type"
private const val AUTHORIZATION = "Authorization"
private const val CACHE_CONTROL = "Cache-Control"

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val firebaseAuthenticationFilter: FirebaseAuthenticationFilter,
    @Value("\${app.cors.allowed-origins:}")
    private val allowedOrigins: String,
    @Value("\${app.swagger.enabled:false}")
    private val swaggerEnabled: Boolean
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { authorizer ->
                authorizer
                    .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/auth/firebase-login", "/auth/debug/token", "/auth/debug/firebase", "/auth/sync-firebase-users").permitAll()

                if (swaggerEnabled) {
                    authorizer.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                }

                authorizer
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().apply {
            allowedOrigins = parseCorsOrigins()
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf(AUTHORIZATION, CACHE_CONTROL, CONTENT_TYPE)
            allowCredentials = true
        }
        source.registerCorsConfiguration(CORS_ALLOW_ALL, config)
        return source
    }

    private fun parseCorsOrigins(): List<String> =
        if (allowedOrigins.isNotBlank()) {
            allowedOrigins.split(',').map { it.trim() }
        } else {
            listOf("*")
        }
}
