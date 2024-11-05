package com.example.kotlinbootlabs.ws

import com.example.kotlinbootlabs.ws.handler.auth.EventTextMessage
import com.example.kotlinbootlabs.ws.handler.auth.MessageFrom
import com.example.kotlinbootlabs.ws.handler.auth.MessageType
import com.example.kotlinbootlabs.ws.handler.auth.sendEventTextMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component

@Component
class MyWebSocketHandler(private val sessionManager: WebSocketSessionManager) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessionManager.addSession(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessionManager.removeSession(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload

        when {
            payload.startsWith("subscribe:") -> {
                val topic = payload.substringAfter("subscribe:")
                sessionManager.subscribeToTopic(session.id, topic)
            }
            payload.startsWith("unsubscribe:") -> {
                val topic = payload.substringAfter("unsubscribe:")
                sessionManager.unsubscribeFromTopic(session.id, topic)
            }
            else -> {
                //session.sendMessage(TextMessage("Echo: $payload"))

                sendEventTextMessage(session, EventTextMessage(
                    type = MessageType.CHAT,
                    message = "Echo: $payload",
                    from = MessageFrom.SYSTEM,
                    id = null,
                    jsondata = null,
                ))
            }
        }
    }
}