package com.example.kotlinbootlabs.ws

import com.example.kotlinbootlabs.ws.handler.auth.EventTextMessage
import com.example.kotlinbootlabs.ws.handler.auth.MessageFrom
import com.example.kotlinbootlabs.ws.handler.auth.MessageType
import com.example.kotlinbootlabs.ws.handler.auth.sendEventTextMessage
import com.example.kotlinbootlabs.ws.handler.auth.sendReactiveEventTextMessage

import org.springframework.web.socket.WebSocketSession
import org.springframework.web.reactive.socket.WebSocketSession as ReactiveWebSocketSession

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketSessionManager {
    private val logger = LoggerFactory.getLogger(WebSocketSessionManager::class.java)

    // SpringBoot 는 MVC또는 Reactive 방식중 하나만 작동함으로 MVC Socket과 Reactive Socket을 동시에 사용할 수 없습니다.
    val sessions = ConcurrentHashMap<String, WebSocketSession>()

    val reactiveSessions = ConcurrentHashMap<String, ReactiveWebSocketSession>()

    val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    fun addSession(session: WebSocketSession) {
        sessions[session.id] = session
        logger.info("Connected: ${session.id}")
    }

    fun addSession(session: ReactiveWebSocketSession) {
        reactiveSessions[session.id] = session
        logger.info("Connected: ${session.id}")
    }

    fun removeSession(session: WebSocketSession) {
        sessions.remove(session.id)
        logger.info("Disconnected: ${session.id}")
    }

    fun removeSession(session: ReactiveWebSocketSession) {
        reactiveSessions.remove(session.id)
        logger.info("Disconnected: ${session.id}")
    }

    fun subscribeToTopic(sessionId: String, topic: String) {
        topicSubscriptions.computeIfAbsent(topic) { mutableSetOf() }.add(sessionId)
        logger.info("Session $sessionId subscribed to topic $topic")
    }

    fun subscribeReactiveToTopic(sessionId: String, topic: String) {
        topicSubscriptions.computeIfAbsent(topic) { mutableSetOf() }.add(sessionId)
        logger.info("Session $sessionId subscribed to topic $topic")
    }

    fun unsubscribeFromTopic(sessionId: String, topic: String) {
        topicSubscriptions[topic]?.remove(sessionId)
        logger.info("Session $sessionId unsubscribed from topic $topic")
    }

    fun unsubscribeReactiveFromTopic(sessionId: String, topic: String) {
        topicSubscriptions[topic]?.remove(sessionId)
        logger.info("Session $sessionId unsubscribed from topic $topic")
    }

    fun sendMessageToSession(sessionId: String, message: String) {
        sessions[sessionId]?.let {
            sendEventTextMessage(
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

    fun sendMessageToTopic(topic: String, message: String) {
        topicSubscriptions[topic]?.forEach { sessionId ->
            sessions[sessionId]?.let {
                sendEventTextMessage(
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