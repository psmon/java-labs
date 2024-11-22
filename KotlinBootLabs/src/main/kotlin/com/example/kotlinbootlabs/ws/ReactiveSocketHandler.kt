package com.example.kotlinbootlabs.ws

import com.example.kotlinbootlabs.ws.handler.auth.EventTextMessage
import com.example.kotlinbootlabs.ws.handler.auth.MessageFrom
import com.example.kotlinbootlabs.ws.handler.auth.MessageType
import com.example.kotlinbootlabs.ws.handler.auth.sendReactiveEventTextMessage
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class ReactiveSocketHandler(private val sessionManager: WebSocketSessionManager) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        sessionManager.addSession(session)

        return session.receive()
            .map { it.payloadAsText }
            .flatMap { payload ->
                when {
                    payload.startsWith("subscribe:") -> {
                        val topic = payload.substringAfter("subscribe:")
                        sessionManager.subscribeReactiveToTopic(session.id, topic)
                        Mono.empty<Void>()
                    }
                    payload.startsWith("unsubscribe:") -> {
                        val topic = payload.substringAfter("unsubscribe:")
                        sessionManager.unsubscribeReactiveFromTopic(session.id, topic)
                        Mono.empty<Void>()
                    }
                    else -> {
                        sendReactiveEventTextMessage(
                            session, EventTextMessage(
                                type = MessageType.CHAT,
                                message = "Echo: $payload",
                                from = MessageFrom.SYSTEM,
                                id = null,
                                jsondata = null,
                            )
                        )
                        Mono.empty<Void>()
                    }
                }
            }
            .then()
            .doFinally { sessionManager.removeSession(session) }
    }
}