package com.example.kotlinbootlabs.controller

import akka.actor.typed.ActorRef
import com.example.kotlinbootlabs.ws.WebSocketSessionManager
import com.example.kotlinbootlabs.ws.actor.SendMessageToSession
import com.example.kotlinbootlabs.ws.actor.SendMessageToTopic
import com.example.kotlinbootlabs.ws.actor.WebSocketSessionManagerCommand
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*

@RestController
class WebSocketController(private val sessionManager: WebSocketSessionManager,
                          private val sessionManagerActor: ActorRef<WebSocketSessionManagerCommand>
) {

    @PostMapping("/send-to-session")
    fun sendMessageToSession(@RequestParam sessionId: String, @RequestBody message: String): String {
        sessionManager.sendMessageToSession(sessionId, message)
        return "Message sent to session $sessionId"
    }

    @PostMapping("/send-to-topic")
    fun sendMessageToTopic(@RequestParam topic: String, @RequestBody message: String): String {
        sessionManager.sendMessageToTopic(topic, message)
        return "Message sent to topic $topic"
    }

    @PostMapping("actor/send-to-session")
    fun sendMessageToSessionByActor(@RequestParam sessionId: String, @RequestBody message: String): String {
        sessionManagerActor.tell(SendMessageToSession(sessionId, message))
        return "Message sent to session $sessionId"
    }

    @PostMapping("actor/send-to-topic")
    fun sendMessageToTopicByActor(@RequestParam topic: String, @RequestBody message: String): String {
        sessionManagerActor.tell(SendMessageToTopic(topic, message))
        return "Message sent to topic $topic"
    }

}