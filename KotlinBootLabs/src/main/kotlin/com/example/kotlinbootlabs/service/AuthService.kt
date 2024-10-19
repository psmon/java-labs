package com.example.kotlinbootlabs.service

import com.example.kotlinbootlabs.exception.LoginFailedException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class AuthService {

    private val secretKey: SecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)
    private val refreshTokenSecretKey: SecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)

    fun authenticate(id: String, password: String, identifier: String, nick: String, authType: String): AuthResponse? {
        if (id != password) {
            throw LoginFailedException("Login failed: Invalid id or password")
        }
        else  {
            val token = generateToken(id, identifier, nick, authType)
            val refreshToken = generateRefreshToken(id)
            return AuthResponse(token, refreshToken)
        }
    }

    private fun generateToken(id: String, identifier: String, nick: String, authType: String): String {
        val now = Date()
        val expiryDate = Date(now.time + 1800000) // 30 minutes

        return Jwts.builder()
            .setSubject(id)
            .claim("nick", nick)
            .claim("identifier", identifier)
            .claim("authType", authType)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    private fun generateRefreshToken(id: String): String {
        val now = Date()
        val expiryDate = Date(now.time + 2592000000) // 30 days

        return Jwts.builder()
            .setSubject(id)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(refreshTokenSecretKey)
            .compact()
    }

    fun getClaimsFromToken(token: String): TokenClaims {
        val claims = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body

        val id = claims.subject
        val nick = claims["nick"] as String
        val identifier = claims["identifier"] as String
        val authType = claims["authType"] as String

        return TokenClaims(id, nick, identifier, authType)
    }
}

data class AuthResponse(val token: String, val refreshToken: String)

data class TokenClaims(val id: String, val nick: String, var identifier: String? = null, val authType: String)