package com.example.kotlinbootlabs.ws.handler.auth

import org.apache.pekko.actor.typed.ActorRef
import com.example.kotlinbootlabs.service.AuthService
import com.example.kotlinbootlabs.ws.actor.*

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.stereotype.Component

data class WebSocketMessageEx1(val type: String, val topic: String? = null, val data: String? = null)

@Component
class SocketHandlerForPersnalRoom(
    private val sessionManagerActor: ActorRef<UserSessionCommand>,
    private val authService: AuthService
) : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessionManagerActor.tell(AddSession(session))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessionManagerActor.tell(RemoveSession(session))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        val webSocketMessage: WebSocketMessageEx1 = objectMapper.readValue(payload)

        when (webSocketMessage.type) {
            "login" -> {
                val token = webSocketMessage.data
                if (token != null ) {
                    try {
                        val authResponse = authService.getClaimsFromToken(token)

                        if(authResponse.authType == "user") {
                            session.attributes["authType"] = "user"
                            session.attributes["token"] = token
                            session.attributes["id"] = authResponse.id
                            session.attributes["nick"] = authResponse.nick
                            session.attributes["identifier"] = authResponse.identifier
                            session.sendMessage(TextMessage("Login successful from User"))
                            sessionManagerActor.tell(UpdateSession(session, authResponse))
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
                            sessionManagerActor.tell(OnUserAction(session, data))
                        }
                    }
                    "subscribe" -> {
                        webSocketMessage.topic?.let { topic ->
                            sessionManagerActor.tell(SubscribeToTopic(session.id, topic))
                        }
                    }
                    "unsubscribe" -> {
                        webSocketMessage.topic?.let { topic ->
                            sessionManagerActor.tell(UnsubscribeFromTopic(session.id, topic))
                        }
                    }
                    "message" -> {

                        session.attributes["identifier"]?.let { identifier ->
                            sessionManagerActor.tell(SendMessageToActor(identifier.toString(), webSocketMessage.data.toString() ))
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