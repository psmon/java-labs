package com.example.kotlinbootlabs.ws.handler.auth

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

enum class MessageType {
    CHAT, CHATBLOCK
}

enum class MessageFrom {
    USER, COUNSELOR, SYSTEM
}

class EventTextMessage(
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val type: MessageType,

    val message: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val from: MessageFrom,

    var id: String? = null,
    val jsondata: String? = null
)

fun sendEventTextMessage(session: WebSocketSession, eventTextMessage: EventTextMessage) {
    val objectMapper = jacksonObjectMapper()
    val jsonPayload = objectMapper.writeValueAsString(eventTextMessage)
    session.sendMessage(TextMessage(jsonPayload))
}