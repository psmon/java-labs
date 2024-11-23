package org.example.kotlinbootreactivelabs.controller


import org.example.kotlinbootreactivelabs.ws.WebSocketSessionManager
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api")
class WebSocketController(private val sessionManager: WebSocketSessionManager) {

    @PostMapping("/send-to-session")
    fun sendMessageToSession(@RequestParam sessionId: String, @RequestBody message: String): Mono<String> {
        return Mono.fromCallable {
            sessionManager.sendReactiveMessageToSession(sessionId, message)
            "Message sent to session $sessionId"
        }
    }

    @PostMapping("/send-to-topic")
    fun sendMessageToTopic(@RequestParam topic: String, @RequestBody message: String): Mono<String> {
        return Mono.fromCallable {
            sessionManager.sendReactiveMessageToTopic(topic, message)
            "Message sent to topic $topic"
        }
    }

    @GetMapping("/health")
    fun healthCheck(): Mono<String> {
        return Mono.just("WebSocketController is healthy")
    }
}