package com.example.kotlinbootlabs.ws

import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.TextMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketSessionManager {
    private val logger = LoggerFactory.getLogger(WebSocketSessionManager::class.java)
    val sessions = ConcurrentHashMap<String, WebSocketSession>()
    val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    fun addSession(session: WebSocketSession) {
        sessions[session.id] = session
        logger.info("Connected: ${session.id}")
    }

    fun removeSession(session: WebSocketSession) {
        sessions.remove(session.id)
        logger.info("Disconnected: ${session.id}")
    }

    fun subscribeToTopic(sessionId: String, topic: String) {
        topicSubscriptions.computeIfAbsent(topic) { mutableSetOf() }.add(sessionId)
        logger.info("Session $sessionId subscribed to topic $topic")
    }

    fun unsubscribeFromTopic(sessionId: String, topic: String) {
        topicSubscriptions[topic]?.remove(sessionId)
        logger.info("Session $sessionId unsubscribed from topic $topic")
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