package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.example.kotlinbootlabs.service.TokenClaims
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.TextMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

sealed class WebSocketSessionManagerCommand
sealed class WebSocketSessionManagerResponse

data class AddSession(val session: WebSocketSession) : WebSocketSessionManagerCommand()
data class RemoveSession(val session: WebSocketSession) : WebSocketSessionManagerCommand()

data class SubscribeToTopic(val sessionId: String, val topic: String) : WebSocketSessionManagerCommand()
data class UnsubscribeFromTopic(val sessionId: String, val topic: String) : WebSocketSessionManagerCommand()

data class UpdateSession(val session: WebSocketSession, val claims: TokenClaims) : WebSocketSessionManagerCommand()
data class OnUserAction(val session: WebSocketSession, val action: String) : WebSocketSessionManagerCommand()

data class SendMessageToSession(val sessionId: String, val message: String) : WebSocketSessionManagerCommand()
data class SendMessageToTopic(val topic: String, val message: String) : WebSocketSessionManagerCommand()
data class SendMessageToAll(val message: String) : WebSocketSessionManagerCommand()

data class GetSessions(val replyTo: ActorRef<WebSocketSessionManagerResponse>) : WebSocketSessionManagerCommand()
data class SessionsResponse(val sessions: Map<String, WebSocketSession>) : WebSocketSessionManagerResponse()

data class Ping(val replyTo: ActorRef<WebSocketSessionManagerResponse>) : WebSocketSessionManagerCommand()
data class Pong(val message: String) : WebSocketSessionManagerResponse()

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
            .onMessage(UpdateSession::class.java, this::onUpdateSession)
            .onMessage(SubscribeToTopic::class.java, this::onSubscribeToTopic)
            .onMessage(UnsubscribeFromTopic::class.java, this::onUnsubscribeFromTopic)
            .onMessage(SendMessageToSession::class.java, this::onSendMessageToSession)
            .onMessage(SendMessageToTopic::class.java, this::onSendMessageToTopic)
            .onMessage(SendMessageToAll::class.java, this::onSendMessageToAll)
            .onMessage(GetSessions::class.java, this::onGetSessions)
            .onMessage(OnUserAction::class.java, this::onUserAction)
            .onMessage(Ping::class.java, this::onPing)
            .build()
    }

    private fun onUserAction(command: OnUserAction): Behavior<WebSocketSessionManagerCommand> {
        when (command.action) {
            "cart" -> {
                command.session.sendMessage(TextMessage("장바구니에 상품을 담으셨군요 00한 상품은 어떤가요?"))
            }
            "buy" -> {
                command.session.sendMessage(TextMessage("상품을 구매하셨네요~"))
            }
        }
        return this
    }

    private fun onUpdateSession(command: UpdateSession): Behavior<WebSocketSessionManagerCommand> {
        command.session.sendMessage(TextMessage("Welcome ${command.claims.nick}"))

        command.claims.identifier?.let { createPrivacyRoom(it) }

        return this
    }

    private fun onPing(command: Ping): Behavior<WebSocketSessionManagerCommand> {
        command.replyTo.tell(Pong("Pong"))
        return this
    }

    private fun onGetSessions(command: GetSessions): Behavior<WebSocketSessionManagerCommand> {
        command.replyTo.tell(SessionsResponse(sessions.toMap()))
        return this
    }

    private fun onAddSession(command: AddSession): Behavior<WebSocketSessionManagerCommand> {
        sessions[command.session.id] = command.session
        logger.info("Connected: ${command.session.id}")
        command.session.sendMessage(TextMessage("{\"type\": \"sessionId\", \"id\": \"${command.session.id}\"}"))
        return this
    }

    private fun onRemoveSession(command: RemoveSession): Behavior<WebSocketSessionManagerCommand> {
        sessions.remove(command.session.id)
        logger.info("Disconnected: ${command.session.id}")

        // Remove the session from all topic subscriptions
        topicSubscriptions.forEach { (topic, sessionIds) ->
            sessionIds.remove(command.session.id)
            if (sessionIds.isEmpty()) {
                topicSubscriptions.remove(topic)
            }
        }

        return this
    }

    private fun onSendMessageToAll(command: SendMessageToAll): Behavior<WebSocketSessionManagerCommand> {
        sessions.values.forEach { session ->
            session.sendMessage(TextMessage(command.message))
        }
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

    private fun createPrivacyRoom(identifier: String) {
        val actorName = "PrivacyRoomActor-${identifier}"
        context.spawn(PrivacyRoomActor.create(identifier), actorName)
        logger.info("PrivacyRoomActor created with identifier: $identifier")
    }

    private fun removePrivacyRoom(identifier: String) {
        val actorName = "PrivacyRoomActor-${identifier}"
        val actorRef = context.children.find { it.path().name() == actorName }
        actorRef?.let { context.stop(it) }
        logger.info("PrivacyRoomActor removed with identifier: $identifier")
    }

    private fun getPrivacyRoomActor(identifier: String): ActorRef<Any>? {
        val actorName = "PrivacyRoomActor-${identifier}"
        val actorRef = context.children.find { it.path().name() == actorName }?.unsafeUpcast<Any>()
        return actorRef;
    }

}