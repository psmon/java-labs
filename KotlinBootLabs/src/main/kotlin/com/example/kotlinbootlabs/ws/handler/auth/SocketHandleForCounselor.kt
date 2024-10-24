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
    private val actorSystem : ActorSystem<MainStageActorCommand>
) : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()

    private lateinit var counselorActor: ActorRef<CounselorCommand>

    override fun afterConnectionEstablished(session: WebSocketSession) {
        // Handle connection established logic if needed
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        // Handle connection closed logic if needed
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        val webSocketMessage: PersnalWsMessage = objectMapper.readValue(payload)

        when (webSocketMessage.type) {
            "login" -> {
                val token = webSocketMessage.data
                if (token != null ) {
                    try {
                        val authResponse = authService.getClaimsFromToken(token)

                        if(authResponse.authType == "counselor") {
                            session.attributes["authType"] = "counselor"
                            session.attributes["token"] = token
                            session.attributes["id"] = authResponse.id
                            session.attributes["nick"] = authResponse.nick
                            session.attributes["identifier"] = authResponse.identifier
                            session.sendMessage(TextMessage("Login successful from Counselor"))
                            //sessionManagerActor.tell(UpdateSession(session, authResponse))

                            val response: CompletionStage<SupervisorChannelResponse> = AskPattern.ask(
                                supervisorChannelActor,
                                { replyTo: ActorRef<SupervisorChannelResponse> ->
                                    authResponse.identifier?.let { GetCounselorFromManager(it, authResponse.nick, replyTo) }
                                },
                                Duration.ofSeconds(3),
                                actorSystem.scheduler()
                            )

                            response.whenComplete { res, ex ->
                                if (res is CounselorActorFound) {
                                    counselorActor = res.actorRef
                                    counselorActor.tell(SetCounselorSocketSession(session))
                                    session.sendMessage(TextMessage("CounselorActor reference obtained."))
                                } else {
                                    session.sendMessage(TextMessage("Failed to obtain CounselorRoomActor reference."))
                                }
                            }
                        }
                        else {
                            session.sendMessage(TextMessage("Login failed: Invalid user type"))
                        }

                    } catch (e: Exception) {
                        session.sendMessage(TextMessage("Login failed: ${e.message}"))
                    }
                } else {
                    session.sendMessage(TextMessage("Login failed: Missing id or password"))
                }
            }
            else -> {
                val token = session.attributes["token"] as String?
                if (token == null || !isValidToken(token)) {
                    session.sendMessage(TextMessage("Invalid or missing token"))
                    return
                }

                when (webSocketMessage.type) {
                    "action" -> {
                        webSocketMessage.data?.let { data ->
                            //sessionManagerActor.tell(OnUserAction(session, data))
                        }
                    }
                    "subscribe" -> {
                        webSocketMessage.topic?.let { topic ->
                            //sessionManagerActor.tell(SubscribeToTopic(session.id, topic))
                        }
                    }
                    "unsubscribe" -> {
                        webSocketMessage.topic?.let { topic ->
                            //sessionManagerActor.tell(UnsubscribeFromTopic(session.id, topic))
                        }
                    }
                    "message" -> {

                        session.attributes["identifier"]?.let { identifier ->
                            //sessionManagerActor.tell(SendMessageToActor(identifier.toString(), webSocketMessage.data.toString() ))
                        }
                        /*
                        webSocketMessage.data?.let { data ->
                            session.sendMessage(TextMessage("Echo: $data"))
                        }*/

                    }
                    else -> {
                        session.sendMessage(TextMessage("Unknown message type: ${webSocketMessage.type}"))
                    }
                }
            }
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