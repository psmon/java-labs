package com.example.kotlinbootlabs.ws

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class MyWebSocketHandler : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(MyWebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        logger.info("Connected: ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessions.remove(session.id)
        logger.info("Disconnected: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        logger.info("Received message: $payload from session: ${session.id}")

        // Example: Handle subscription to a topic
        if (payload.startsWith("subscribe:")) {
            val topic = payload.substringAfter("subscribe:")
            topicSubscriptions.computeIfAbsent(topic) { mutableSetOf() }.add(session.id)
            logger.info("Session ${session.id} subscribed to topic $topic")
        } else {
            session.sendMessage(TextMessage("Echo: $payload"))
        }
    }

    fun sendMessageToSession(sessionId: String, message: String) {
        sessions[sessionId]?.sendMessage(TextMessage(message))
    }

    fun sendMessageToTopic(topic: String, message: String) {
        topicSubscriptions[topic]?.forEach { sessionId ->
            sessions[sessionId]?.sendMessage(TextMessage(message))
        }
    }
}