package com.example.kotlinbootlabs.controller

import com.example.kotlinbootlabs.ws.MyWebSocketHandler
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*

@RestController
class WebSocketController(private val webSocketHandler: MyWebSocketHandler) {

    @PostMapping("/send-to-session")
    fun sendMessageToSession(@RequestParam sessionId: String, @RequestBody message: String): String {
        webSocketHandler.sendMessageToSession(sessionId, message)
        return "Message sent to session $sessionId"
    }

    @PostMapping("/send-to-topic")
    fun sendMessageToTopic(@RequestParam topic: String, @RequestBody message: String): String {
        webSocketHandler.sendMessageToTopic(topic, message)
        return "Message sent to topic $topic"
    }
}