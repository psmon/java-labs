package com.example.kotlinbootlabs.ws

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MyWebSocketHandler : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(MyWebSocketHandler::class.java)

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.info("Received message: ${message.payload}")
        session.sendMessage(TextMessage("Echo: ${message.payload}"))
    }
}