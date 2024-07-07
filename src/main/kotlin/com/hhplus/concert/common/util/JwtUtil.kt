package com.hhplus.concert.common.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-days}") private val expirationDays: Long,
) {
    fun generateToken(userId: Long): String {
        val claims = Jwts.claims().setSubject(userId.toString())
        val now = Instant.now()
        val expirationDate = now.plus(expirationDays, ChronoUnit.DAYS)

        return Jwts
            .builder()
            .setClaims(claims)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expirationDate))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()), SignatureAlgorithm.ES512)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long? =
        try {
            val claims =
                Jwts
                    .parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.toByteArray()))
                    .build()
                    .parseClaimsJws(token)
                    .body

            claims.subject.toLong()
        } catch (e: Exception) {
            null
        }

    fun validateToken(token: String): Boolean =
        try {
            Jwts
                .parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.toByteArray()))
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
}
