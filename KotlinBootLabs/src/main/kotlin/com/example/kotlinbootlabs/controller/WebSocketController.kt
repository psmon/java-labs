package com.example.kotlinbootlabs.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WebSocketController {

    @Operation(summary = "WebSocket endpoint", description = "WebSocket endpoint for real-time messaging")
    @GetMapping("/ws-info")
    fun websocketInfo(): String {
        return "Use /ws endpoint for WebSocket connection"
    }
}