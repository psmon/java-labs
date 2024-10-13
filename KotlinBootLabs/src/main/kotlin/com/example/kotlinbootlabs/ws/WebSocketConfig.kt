package com.example.kotlinbootlabs.ws

import akka.actor.typed.ActorRef
import com.example.kotlinbootlabs.ws.actor.ActorWebSocketHandler
import com.example.kotlinbootlabs.ws.actor.WebSocketSessionManagerCommand
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig(private val webSocketHandler: MyWebSocketHandler,
                      private val actorWebSocketHandler: ActorWebSocketHandler,
                      private val sessionManager: WebSocketSessionManager
) : WebSocketConfigurer {

    @Bean
    fun webSocketHandler() = MyWebSocketHandler(sessionManager)

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        // Local WebSocket handler
        registry.addHandler(webSocketHandler, "/ws")
            .addInterceptors(HttpSessionHandshakeInterceptor())
            .setAllowedOrigins("*")

        // Actor WebSocket handler
        registry.addHandler(actorWebSocketHandler, "/ws-actor")
            .addInterceptors(HttpSessionHandshakeInterceptor())
            .setAllowedOrigins("*")
    }


}