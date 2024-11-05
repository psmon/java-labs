package com.example.kotlinbootlabs.ws.handler.auth

import com.example.kotlinbootlabs.actor.MainStageActorCommand
import akka.actor.typed.ActorRef
import com.example.kotlinbootlabs.service.AuthService
import com.example.kotlinbootlabs.ws.actor.*

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletionStage

data class PersnalWsMessage(val type: String,val channel: String? ,val topic: String? = null, val data: String? = null)

@Component
class SocketHandlerForPersnalRoom(
    private val sessionManagerActor: ActorRef<UserSessionCommand>,
    private val authService: AuthService,
    private val supervisorChannelActor: ActorRef<SupervisorChannelCommand>,
    private val actorSystem: ActorSystem<MainStageActorCommand>
) : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()

    private lateinit var persnalRoomActor: ActorRef<PersonalRoomCommand>

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
            "requestCounseling" -> handleCounselingRequest(session, webSocketMessage.channel)
            "sendchat" -> handleSendChat(session, webSocketMessage.data)
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

                    //session.sendMessage(TextMessage("Login successful from User"))

                    sendEventTextMessage(session, EventTextMessage(
                        type = MessageType.INFO,
                        message = "Login successful from User",
                        from = MessageFrom.SYSTEM,
                        id = null,
                        jsondata = null,
                    ))

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
                            //session.sendMessage(TextMessage("PersonalRoomActor reference obtained."))

                            sendEventTextMessage(session, EventTextMessage(
                                type = MessageType.INFO,
                                message = "PersonalRoomActor reference obtained.",
                                from = MessageFrom.SYSTEM,
                                id = null,
                                jsondata = null,
                            ))

                        } else {
                            //session.sendMessage(TextMessage("Failed to obtain CounselorRoomActor reference."))

                            sendEventTextMessage(session, EventTextMessage(
                                type = MessageType.ERROR,
                                message = "Failed to obtain CounselorRoomActor reference.",
                                from = MessageFrom.SYSTEM,
                                id = null,
                                jsondata = null,
                            ))
                        }
                    }
                } else {
                    //session.sendMessage(TextMessage("Login failed: Invalid user type"))

                    sendEventTextMessage(session, EventTextMessage(
                        type = MessageType.ERROR,
                        message = "Login failed: Invalid user type",
                        from = MessageFrom.SYSTEM,
                        id = null,
                        jsondata = null,
                    ))
                }

            } catch (e: Exception) {
                //session.sendMessage(TextMessage("Login failed: ${e.message}"))

                sendEventTextMessage(session, EventTextMessage(
                    type = MessageType.ERROR,
                    message = "Login failed: ${e.message}",
                    from = MessageFrom.SYSTEM,
                    id = null,
                    jsondata = null,
                ))
            }
        } else {
            //session.sendMessage(TextMessage("Login failed: Missing id or password"))

            sendEventTextMessage(session, EventTextMessage(
                type = MessageType.ERROR,
                message = "Login failed: Missing id or password",
                from = MessageFrom.SYSTEM,
                id = null,
                jsondata = null,
            ))
        }
    }

    private fun handleCounselingRequest(session: WebSocketSession, channel: String?) {
        val token = session.attributes["token"] as String?
        if (token == null || !isValidToken(token)) {
            //session.sendMessage(TextMessage("Invalid or missing token"))

            sendEventTextMessage(session, EventTextMessage(
                type = MessageType.ERROR,
                message = "Invalid or missing token",
                from = MessageFrom.SYSTEM,
                id = null,
                jsondata = null,
            ))
            return
        }

        if (channel != null) {
            val roomName = "${channel}_${UUID.randomUUID()}"

            AskPattern.ask(
                supervisorChannelActor,
                { replyTo: ActorRef<SupervisorChannelResponse> -> GetCounselorManager(channel, replyTo) },
                Duration.ofSeconds(3),
                actorSystem.scheduler()
            ).thenAccept { res ->
                if (res is CounselorManagerFound) {
                    counselorManager = res.actorRef
                    //session.sendMessage(TextMessage("Counseling CounselorManagerFound : ${res.channel}"))

                    sendEventTextMessage(session, EventTextMessage(
                        type = MessageType.INFO,
                        message = "Counseling CounselorManagerFound : ${res.channel}",
                        from = MessageFrom.SYSTEM,
                        id = null,
                        jsondata = null,
                    ))

                    AskPattern.ask(
                        counselorManager,
                        { replyTo: ActorRef<CounselorManagerResponse> -> RequestCounseling(roomName,
                            generateRandomSkillInfo(), persnalRoomActor, replyTo) },
                        Duration.ofSeconds(3),
                        actorSystem.scheduler()
                    ).thenAccept() { res2 ->
                        if (res2 is CounselorRoomFound) {
                            //session.sendMessage(TextMessage("Counseling room created: $roomName"))

                            sendEventTextMessage(session, EventTextMessage(
                                type = MessageType.INFO,
                                message = "Counseling room created: $roomName",
                                from = MessageFrom.SYSTEM,
                                id = null,
                                jsondata = null,
                            ))
                            counselorRoomActor = res2.actorRef
                        } else {
                            //session.sendMessage(TextMessage("Counseling request failed: $roomName"))

                            sendEventTextMessage(session, EventTextMessage(
                                type = MessageType.ERROR,
                                message = "Counseling request failed: $roomName",
                                from = MessageFrom.SYSTEM,
                                id = null,
                                jsondata = null,
                            ))
                        }
                    }

                } else {
                    //session.sendMessage(TextMessage("Counselor manager not found for channel: $channel"))

                    sendEventTextMessage(
                        session, EventTextMessage(
                            type = MessageType.ERROR,
                            message = "Counselor manager not found for channel: $channel",
                            from = MessageFrom.SYSTEM,
                            id = null,
                            jsondata = null,
                        )
                    )
                }
            }
        } else {
            //session.sendMessage(TextMessage("Counseling request failed: Missing channel"))

            sendEventTextMessage(
                session, EventTextMessage(
                    type = MessageType.ERROR,
                    message = "Counseling request failed: Missing channel",
                    from = MessageFrom.SYSTEM,
                    id = null,
                    jsondata = null,
                )
            )

        }
    }

    private fun handleSendChat(session: WebSocketSession, chatMessage: String?) {
        if (chatMessage != null) {
            persnalRoomActor.tell(SendToCounselorRoomForCounseling(chatMessage))
            //session.sendMessage(TextMessage("Chat message sent: $chatMessage"))
        } else {
            //session.sendMessage(TextMessage("Chat message is missing"))

            sendEventTextMessage(
                session, EventTextMessage(
                    type = MessageType.ERROR,
                    message = "Chat message is missing",
                    from = MessageFrom.SYSTEM,
                    id = null,
                    jsondata = null,
                )
            )

        }
    }

    private fun handleOtherMessages(session: WebSocketSession, webSocketMessage: PersnalWsMessage) {
        val token = session.attributes["token"] as String?
        if (token == null || !isValidToken(token)) {
            //session.sendMessage(TextMessage("Invalid or missing token"))

            sendEventTextMessage(
                session, EventTextMessage(
                    type = MessageType.ERROR,
                    message = "Invalid or missing token",
                    from = MessageFrom.SYSTEM,
                    id = null,
                    jsondata = null,
                )
            )
            return
        }

        when (webSocketMessage.type) {
            "action" -> webSocketMessage.data?.let { data -> sessionManagerActor.tell(OnUserAction(session, data)) }
            "subscribe" -> webSocketMessage.topic?.let { topic -> sessionManagerActor.tell(SubscribeToTopic(session.id, topic)) }
            "unsubscribe" -> webSocketMessage.topic?.let { topic -> sessionManagerActor.tell(UnsubscribeFromTopic(session.id, topic)) }
            "message" -> session.attributes["identifier"]?.let { identifier -> sessionManagerActor.tell(SendMessageToActor(identifier.toString(), webSocketMessage.data.toString())) }
            else -> {

                //session.sendMessage(TextMessage("Unknown message type: ${webSocketMessage.type}"))

                sendEventTextMessage(
                    session, EventTextMessage(
                        type = MessageType.ERROR,
                        message = "Unknown message type: ${webSocketMessage.type}",
                        from = MessageFrom.SYSTEM,
                        id = null,
                        jsondata = null,
                    )
                )
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