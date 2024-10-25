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

data class CounselorWsMessage(val type: String, val channel: String? = null, val counselorName: String? = null, val data: String? = null)

@Component
class SocketHandleForCounselor(
    private val supervisorChannelActor: ActorRef<SupervisorChannelCommand>,
    private val authService: AuthService,
    private val actorSystem: ActorSystem<MainStageActorCommand>
) : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()
    private lateinit var counselorActor: ActorRef<CounselorCommand>

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val webSocketMessage: CounselorWsMessage = objectMapper.readValue(message.payload)

        when (webSocketMessage.type) {
            "login" -> handleLogin(session, webSocketMessage.data)
            else -> handleOtherMessages(session, webSocketMessage)
        }
    }

    private fun handleLogin(session: WebSocketSession, token: String?) {
        if (token == null) {
            session.sendMessage(TextMessage("Login failed: Missing id or password"))
            return
        }

        try {
            val authResponse = authService.getClaimsFromToken(token)
            if (authResponse.authType != "counselor") {
                session.sendMessage(TextMessage("Login failed: Invalid user type"))
                return
            }

            session.attributes.apply {
                put("authType", "counselor")
                put("token", token)
                put("id", authResponse.id)
                put("nick", authResponse.nick)
                put("identifier", authResponse.identifier)
            }
            session.sendMessage(TextMessage("Login successful from Counselor"))

            val response: CompletionStage<SupervisorChannelResponse> = AskPattern.ask(
                supervisorChannelActor,
                { replyTo: ActorRef<SupervisorChannelResponse> ->
                    authResponse.identifier?.let { GetCounselorFromManager(it, authResponse.nick, replyTo) }
                },
                Duration.ofSeconds(3),
                actorSystem.scheduler()
            )

            response.whenComplete { res, _ ->
                if (res is CounselorActorFound) {
                    counselorActor = res.actorRef
                    counselorActor.tell(SetCounselorSocketSession(session))
                    session.sendMessage(TextMessage("CounselorActor reference obtained."))
                } else {
                    session.sendMessage(TextMessage("Failed to obtain CounselorActor reference."))
                }
            }
        } catch (e: Exception) {
            session.sendMessage(TextMessage("Login failed: ${e.message}"))
        }
    }

    private fun handleOtherMessages(session: WebSocketSession, webSocketMessage: CounselorWsMessage) {
        val token = session.attributes["token"] as String?
        if (token == null || !isValidToken(token)) {
            session.sendMessage(TextMessage("Invalid or missing token"))
            return
        }

        when (webSocketMessage.type) {
            "action" -> webSocketMessage.data?.let { /* Handle action */ }
            "subscribe" -> webSocketMessage.channel?.let { /* Handle subscribe */ }
            "unsubscribe" -> webSocketMessage.channel?.let { /* Handle unsubscribe */ }
            "message" -> session.attributes["identifier"]?.let { /* Handle message */ }
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