package com.example.kotlinbootlabs.config

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import com.example.kotlinbootlabs.ws.actor.WebSocketSessionManagerActor
import com.example.kotlinbootlabs.ws.actor.WebSocketSessionManagerCommand
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AkkaConfiguration {

    private lateinit var actorSystem: ActorSystem<WebSocketSessionManagerCommand>

    @PostConstruct
    fun init() {
        actorSystem = ActorSystem.create(WebSocketSessionManagerActor.create(), "WebSocketSystem")
    }

    @PreDestroy
    fun shutdown() {
        actorSystem.terminate()
    }

    @Bean
    fun sessionManagerActor(): ActorRef<WebSocketSessionManagerCommand> {
        return actorSystem
    }
}