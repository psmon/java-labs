package com.example.kotlinbootlabs.ws.handler.auth

import com.example.kotlinbootlabs.actor.MainStageActorCommand
import org.apache.pekko.actor.typed.ActorRef
import com.example.kotlinbootlabs.service.AuthService
import com.example.kotlinbootlabs.ws.actor.*

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.AskPattern
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletionStage

data class PersnalWsMessage(val type: String, val topic: String? = null, val data: String? = null)

@Component
class SocketHandlerForPersnalRoom(
    private val sessionManagerActor: ActorRef<UserSessionCommand>,
    private val authService: AuthService,
    private val supervisorChannelActor: ActorRef<SupervisorChannelCommand>,
    private val actorSystem: ActorSystem<MainStageActorCommand>
) : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()

    private lateinit var persnalRoomActor: ActorRef<PersnalRoomCommand>

    private lateinit var counselorManager: ActorRef<CounselorManagerCommand>

    private lateinit var counselorRoomActor: ActorRef<CounselorRoomCommand>

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessionManagerActor.tell(AddSession(session))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessionManagerActor.tell(RemoveSession(session))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        val webSocketMessage: PersnalWsMessage = objectMapper.readValue(payload)

        when (webSocketMessage.type) {
            "login" -> handleLogin(session, webSocketMessage.data)
            "requestCounseling" -> handleCounselingRequest(session, webSocketMessage.topic,
                webSocketMessage.data)
            else -> handleOtherMessages(session, webSocketMessage)
        }
    }

    private fun handleLogin(session: WebSocketSession, token: String?) {
        if (token != null) {
            try {
                val authResponse = authService.getClaimsFromToken(token)

                if (authResponse.authType == "user") {
                    session.attributes["authType"] = "user"
                    session.attributes["token"] = token
                    session.attributes["id"] = authResponse.id
                    session.attributes["nick"] = authResponse.nick
                    session.attributes["identifier"] = authResponse.identifier
                    session.sendMessage(TextMessage("Login successful from User"))
                    sessionManagerActor.tell(UpdateSession(session, authResponse))

                    val response: CompletionStage<UserSessionResponse> = AskPattern.ask(
                        sessionManagerActor,
                        { replyTo: ActorRef<UserSessionResponse> ->
                            authResponse.identifier?.let { GetPersonalRoomActor(it, replyTo) }
                        },
                        Duration.ofSeconds(3),
                        actorSystem.scheduler()
                    )

                    response.whenComplete { res, ex ->
                        if (res is FoundPersonalRoomActor) {
                            persnalRoomActor = res.actorRef
                            session.sendMessage(TextMessage("PersonalRoomActor reference obtained."))
                        } else {
                            session.sendMessage(TextMessage("Failed to obtain CounselorRoomActor reference."))
                        }
                    }
                } else {
                    session.sendMessage(TextMessage("Login failed: Invalid user type"))
                }

            } catch (e: Exception) {
                session.sendMessage(TextMessage("Login failed: ${e.message}"))
            }
        } else {
            session.sendMessage(TextMessage("Login failed: Missing id or password"))
        }
    }

    private fun handleCounselingRequest(session: WebSocketSession, channel: String?
                                        , roomName: String?) {
        val token = session.attributes["token"] as String?
        if (token == null || !isValidToken(token)) {
            session.sendMessage(TextMessage("Invalid or missing token"))
            return
        }

        if (channel != null) {
            AskPattern.ask(
                supervisorChannelActor,
                { replyTo: ActorRef<SupervisorChannelResponse> -> GetCounselorManager(channel, replyTo) },
                Duration.ofSeconds(3),
                actorSystem.scheduler()
            ).thenAccept { res ->
                if (res is CounselorManagerFound) {
                    counselorManager = res.actorRef

                    AskPattern.ask(
                        counselorManager,
                        { replyTo: ActorRef<CounselorManagerResponse> -> roomName?.let { GetCounselorRoom(it, replyTo) } },
                        Duration.ofSeconds(3),
                        actorSystem.scheduler()
                    ).thenAccept { res2 ->
                        if (res2 is CounselorRoomFound) {
                            counselorRoomActor = res2.actorRef
                        } else {
                            session.sendMessage(TextMessage("Counselor room not found for roomName: $roomName"))
                        }
                    }
                } else {
                    session.sendMessage(TextMessage("Counselor manager not found for channel: $channel"))
                }
            }
        } else {
            session.sendMessage(TextMessage("Counseling request failed: Missing channel"))
        }
    }

    private fun handleOtherMessages(session: WebSocketSession, webSocketMessage: PersnalWsMessage) {
        val token = session.attributes["token"] as String?
        if (token == null || !isValidToken(token)) {
            session.sendMessage(TextMessage("Invalid or missing token"))
            return
        }

        when (webSocketMessage.type) {
            "action" -> webSocketMessage.data?.let { data -> sessionManagerActor.tell(OnUserAction(session, data)) }
            "subscribe" -> webSocketMessage.topic?.let { topic -> sessionManagerActor.tell(SubscribeToTopic(session.id, topic)) }
            "unsubscribe" -> webSocketMessage.topic?.let { topic -> sessionManagerActor.tell(UnsubscribeFromTopic(session.id, topic)) }
            "message" -> session.attributes["identifier"]?.let { identifier -> sessionManagerActor.tell(SendMessageToActor(identifier.toString(), webSocketMessage.data.toString())) }
            else -> session.sendMessage(TextMessage("Unknown message type: ${webSocketMessage.type}"))
        }
    }

    private fun isValidToken(token: String): Boolean {
        return try {
            authService.getClaimsFromToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }

}