package com.example.kotlinbootlabs.controller

import com.example.kotlinbootlabs.service.AuthService
import com.example.kotlinbootlabs.service.AuthResponse
import com.example.kotlinbootlabs.service.TokenClaims
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestParam id: String, @RequestParam password: String,
              @RequestParam identifier: String): AuthResponse? {
        return authService.authenticate(id, password, identifier)
    }

    @GetMapping("/claims")
    fun getClaims(@RequestParam token: String): TokenClaims? {
        return authService.getClaimsFromToken(token)
    }
}