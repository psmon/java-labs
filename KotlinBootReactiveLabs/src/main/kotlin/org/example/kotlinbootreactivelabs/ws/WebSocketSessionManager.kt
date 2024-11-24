package org.example.kotlinbootreactivelabs.ws

import org.example.kotlinbootreactivelabs.ws.model.EventTextMessage
import org.example.kotlinbootreactivelabs.ws.model.MessageFrom
import org.example.kotlinbootreactivelabs.ws.model.MessageType
import org.example.kotlinbootreactivelabs.ws.model.sendReactiveEventTextMessage
import org.springframework.web.reactive.socket.WebSocketSession as ReactiveWebSocketSession

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap


@Component
class WebSocketSessionManager {

    private val logger = LoggerFactory.getLogger(WebSocketSessionManager::class.java)

    val reactiveSessions = ConcurrentHashMap<String, ReactiveWebSocketSession>()

    val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()


    fun addSession(session: ReactiveWebSocketSession) {
        reactiveSessions[session.id] = session
        logger.info("[SessionManager] Connected: ${session.id}")

        sendReactiveEventTextMessage(session, EventTextMessage(
            type = MessageType.INFO,
            message = "You are connected - ${session.id}",
            from = MessageFrom.SYSTEM,
            id = null,
            jsondata = null,
        ))
    }

    fun removeSession(session: ReactiveWebSocketSession) {
        reactiveSessions.remove(session.id)
        logger.info("[SessionManager] Disconnected: ${session.id}")
    }


    fun subscribeReactiveToTopic(sessionId: String, topic: String) {
        topicSubscriptions.computeIfAbsent(topic) { mutableSetOf() }.add(sessionId)
        logger.info("Session $sessionId subscribed to topic $topic")
    }

    fun unsubscribeReactiveFromTopic(sessionId: String, topic: String) {
        topicSubscriptions[topic]?.remove(sessionId)
        logger.info("Session $sessionId unsubscribed from topic $topic")
    }

    fun sendReactiveMessageToSession(sessionId: String, message: String) {
        reactiveSessions[sessionId]?.let {
            sendReactiveEventTextMessage(
                it, EventTextMessage(
                    type = MessageType.PUSH,
                    message = message,
                    from = MessageFrom.SYSTEM,
                    id = null,
                    jsondata = null,
                )
            )
        }
    }

    fun sendReactiveMessageToTopic(topic: String, message: String) {
        topicSubscriptions[topic]?.forEach { sessionId ->
            reactiveSessions[sessionId]?.let {
                sendReactiveEventTextMessage(
                    it, EventTextMessage(
                        type = MessageType.PUSH,
                        message = message,
                        from = MessageFrom.SYSTEM,
                        id = null,
                        jsondata = null,
                    )
                )
            }
        }
    }
}