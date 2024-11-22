package com.example.kotlinbootlabs.ws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.socket.server.support.WebSocketHandlerMapping


//@Configuration
//@EnableWebFlux
class ReactiveWebSocketConfig(private val reactiveSocketHandler: ReactiveSocketHandler) {

    @Bean
    fun reactiveWebSocketHandlerMapping(): WebSocketHandlerMapping {
        val map = mapOf("/ws-reactive" to reactiveSocketHandler)
        val mapping = WebSocketHandlerMapping()
        mapping.urlMap = map
        mapping.order = -1 // before annotated controllers
        return mapping
    }

    @Bean
    fun handlerAdapter() = WebSocketHandlerAdapter()
}