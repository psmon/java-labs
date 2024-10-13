package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.TextMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

sealed class WebSocketSessionManagerCommand
data class AddSession(val session: WebSocketSession) : WebSocketSessionManagerCommand()
data class RemoveSession(val session: WebSocketSession) : WebSocketSessionManagerCommand()
data class SubscribeToTopic(val sessionId: String, val topic: String) : WebSocketSessionManagerCommand()
data class UnsubscribeFromTopic(val sessionId: String, val topic: String) : WebSocketSessionManagerCommand()
data class SendMessageToSession(val sessionId: String, val message: String) : WebSocketSessionManagerCommand()
data class SendMessageToTopic(val topic: String, val message: String) : WebSocketSessionManagerCommand()

class WebSocketSessionManagerActor private constructor(
    context: ActorContext<WebSocketSessionManagerCommand>
) : AbstractBehavior<WebSocketSessionManagerCommand>(context) {

    companion object {
        fun create(): Behavior<WebSocketSessionManagerCommand> {
            return Behaviors.setup { context -> WebSocketSessionManagerActor(context) }
        }
    }

    private val logger = LoggerFactory.getLogger(WebSocketSessionManagerActor::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    override fun createReceive(): Receive<WebSocketSessionManagerCommand> {
        return newReceiveBuilder()
            .onMessage(AddSession::class.java, this::onAddSession)
            .onMessage(RemoveSession::class.java, this::onRemoveSession)
            .onMessage(SubscribeToTopic::class.java, this::onSubscribeToTopic)
            .onMessage(UnsubscribeFromTopic::class.java, this::onUnsubscribeFromTopic)
            .onMessage(SendMessageToSession::class.java, this::onSendMessageToSession)
            .onMessage(SendMessageToTopic::class.java, this::onSendMessageToTopic)
            .build()
    }

    private fun onAddSession(command: AddSession): Behavior<WebSocketSessionManagerCommand> {
        sessions[command.session.id] = command.session
        logger.info("Connected: ${command.session.id}")
        return this
    }

    private fun onRemoveSession(command: RemoveSession): Behavior<WebSocketSessionManagerCommand> {
        sessions.remove(command.session.id)
        logger.info("Disconnected: ${command.session.id}")
        return this
    }

    private fun onSubscribeToTopic(command: SubscribeToTopic): Behavior<WebSocketSessionManagerCommand> {
        topicSubscriptions.computeIfAbsent(command.topic) { mutableSetOf() }.add(command.sessionId)
        logger.info("Session ${command.sessionId} subscribed to topic ${command.topic}")
        return this
    }

    private fun onUnsubscribeFromTopic(command: UnsubscribeFromTopic): Behavior<WebSocketSessionManagerCommand> {
        topicSubscriptions[command.topic]?.remove(command.sessionId)
        logger.info("Session ${command.sessionId} unsubscribed from topic ${command.topic}")
        return this
    }

    private fun onSendMessageToSession(command: SendMessageToSession): Behavior<WebSocketSessionManagerCommand> {
        sessions[command.sessionId]?.sendMessage(TextMessage(command.message))
        return this
    }

    private fun onSendMessageToTopic(command: SendMessageToTopic): Behavior<WebSocketSessionManagerCommand> {
        topicSubscriptions[command.topic]?.forEach { sessionId ->
            sessions[sessionId]?.sendMessage(TextMessage(command.message))
        }
        return this
    }
}