package com.example.template.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${app.security.jwt.secret}")
    private val jwtSecret: String,
    @Value("\${app.security.jwt.expiration}")
    private val jwtExpiration: Long,
    @Value("\${app.security.jwt.refresh-expiration}")
    private val refreshExpiration: Long
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetails
        return createToken(userPrincipal.username, jwtExpiration)
    }

    fun generateTokenFromUsername(username: String): String {
        return createToken(username, jwtExpiration)
    }

    fun generateRefreshToken(username: String): String {
        return createToken(username, refreshExpiration)
    }

    private fun createToken(subject: String, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        val claims = getClaims(token)
        return claims.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (ex: SecurityException) {
            logger.error("Invalid JWT signature: ${ex.message}")
            false
        } catch (ex: MalformedJwtException) {
            logger.error("Invalid JWT token: ${ex.message}")
            false
        } catch (ex: ExpiredJwtException) {
            logger.error("JWT token is expired: ${ex.message}")
            false
        } catch (ex: UnsupportedJwtException) {
            logger.error("JWT token is unsupported: ${ex.message}")
            false
        } catch (ex: IllegalArgumentException) {
            logger.error("JWT claims string is empty: ${ex.message}")
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getExpirationDateFromToken(token: String): Date {
        return getClaims(token).expiration
    }

    fun isTokenExpired(token: String): Boolean {
        val expiration = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }
}