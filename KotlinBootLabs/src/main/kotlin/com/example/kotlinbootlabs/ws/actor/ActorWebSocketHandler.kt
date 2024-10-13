package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component

@Component
class ActorWebSocketHandler(private val sessionManagerActor: ActorRef<WebSocketSessionManagerCommand>)
    : TextWebSocketHandler() {

    //private lateinit var sessionManagerActor: ActorRef<WebSocketSessionManagerCommand>

    fun setSessionManagerActor(actorRef: ActorRef<WebSocketSessionManagerCommand>) {
        //this.sessionManagerActor = actorRef
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessionManagerActor.tell(AddSession(session))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessionManagerActor.tell(RemoveSession(session))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload

        when {
            payload.startsWith("subscribe:") -> {
                val topic = payload.substringAfter("subscribe:")
                sessionManagerActor.tell(SubscribeToTopic(session.id, topic))
            }
            payload.startsWith("unsubscribe:") -> {
                val topic = payload.substringAfter("unsubscribe:")
                sessionManagerActor.tell(UnsubscribeFromTopic(session.id, topic))
            }
            else -> {
                session.sendMessage(TextMessage("Echo: $payload"))
            }
        }
    }
}