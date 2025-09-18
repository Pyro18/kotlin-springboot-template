package com.example.template.config

import com.example.template.security.JwtAuthenticationFilter
import com.example.template.security.JwtAuthenticationEntryPoint
import com.example.template.security.JwtTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    @Value("\${app.security.cors.allowed-origins}")
    private val allowedOrigins: List<String>,
    @Value("\${app.security.cors.allowed-methods}")
    private val allowedMethods: String,
    @Value("\${app.security.cors.allowed-headers}")
    private val allowedHeaders: String,
    @Value("\${app.security.cors.exposed-headers}")
    private val exposedHeaders: String,
    @Value("\${app.security.cors.allow-credentials}")
    private val allowCredentials: Boolean,
    @Value("\${app.security.cors.max-age}")
    private val maxAge: Long
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager =
        authConfig.authenticationManager

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(userDetailsService)
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtTokenProvider, userDetailsService)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints
                    .requestMatchers(
                        "/api/v1/auth/**",
                        "/api/v1/users/register",
                        "/api/v1/health/**"
                    ).permitAll()

                    // Swagger/OpenAPI
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/v3/api-docs/**"
                    ).permitAll()

                    // Actuator endpoints
                    .requestMatchers(
                        "/actuator/health/**",
                        "/actuator/info",
                        "/actuator/metrics/**",
                        "/actuator/prometheus"
                    ).permitAll()

                    // Static resources
                    .requestMatchers(
                        "/",
                        "/favicon.ico",
                        "/**/*.png",
                        "/**/*.gif",
                        "/**/*.svg",
                        "/**/*.jpg",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js"
                    ).permitAll()

                    // Public GET endpoints
                    .requestMatchers(HttpMethod.GET, "/api/v1/users/username/{username}").permitAll()

                    // Admin only endpoints
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                    // Moderator endpoints
                    .requestMatchers("/api/v1/moderate/**").hasAnyRole("ADMIN", "MODERATOR")

                    // All other requests require authentication
                    .anyRequest().authenticated()
            }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers ->
                headers
                    .frameOptions { it.sameOrigin() }
                    .contentSecurityPolicy {
                        it.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';")
                    }
                    .xssProtection { it.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK) }
                    .contentTypeOptions { }
                    .referrerPolicy { it.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                    .permissionsPolicy {
                        it.policy("geolocation=(), microphone=(), camera=()")
                    }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = allowedOrigins
            allowedMethods = this@SecurityConfig.allowedMethods.split(",").map { it.trim() }
            allowedHeaders = this@SecurityConfig.allowedHeaders.split(",").map { it.trim() }
            exposedHeaders = this@SecurityConfig.exposedHeaders.split(",").map { it.trim() }
            allowCredentials = this@SecurityConfig.allowCredentials
            maxAge = this@SecurityConfig.maxAge
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}