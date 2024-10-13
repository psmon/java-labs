package com.example.kotlinbootlabs.ws.handler.basic

import akka.actor.typed.ActorRef
import com.example.kotlinbootlabs.ws.actor.*
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component

data class WebSocketMessage(val type: String, val topic: String? = null, val data: String? = null)

@Component
class SocketHandlerWithActor(private val sessionManagerActor: ActorRef<WebSocketSessionManagerCommand>)
    : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessionManagerActor.tell(AddSession(session))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessionManagerActor.tell(RemoveSession(session))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        val webSocketMessage: WebSocketMessage = objectMapper.readValue(payload)

        when (webSocketMessage.type) {
            "subscribe" -> {
                webSocketMessage.topic?.let { topic ->
                    sessionManagerActor.tell(SubscribeToTopic(session.id, topic))
                }
            }
            "unsubscribe" -> {
                webSocketMessage.topic?.let { topic ->
                    sessionManagerActor.tell(UnsubscribeFromTopic(session.id, topic))
                }
            }
            "message" -> {
                webSocketMessage.data?.let { data ->
                    session.sendMessage(TextMessage("Echo: $data"))
                }
            }
            else -> {
                session.sendMessage(TextMessage("Unknown message type: ${webSocketMessage.type}"))
            }
        }
    }
}