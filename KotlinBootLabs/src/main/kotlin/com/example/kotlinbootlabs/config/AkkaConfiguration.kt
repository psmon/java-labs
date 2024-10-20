package com.example.kotlinbootlabs.config

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.AskPattern
import com.example.kotlinbootlabs.actor.*
import com.example.kotlinbootlabs.ws.actor.UserSessionCommand
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.CompletionStage


@Configuration
class AkkaConfiguration {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(AkkaConfiguration::class.java)

    private lateinit var actorSystem: ActorSystem<MainStageActorCommand>

    private lateinit var sessionManagerActor: ActorRef<UserSessionCommand>

    @PostConstruct
    fun init() {
        actorSystem = ActorSystem.create(MainStageActor.create(), "MainStageActor")

        // Send CreateSocketSessionManager event and handle the response
        val response: CompletionStage<MainStageActorResponse> = AskPattern.ask(
            actorSystem,
            { replyTo: ActorRef<MainStageActorResponse> -> CreateSocketSessionManager(replyTo) },
            Duration.ofSeconds(3),
            actorSystem.scheduler()
        )

        response.whenComplete { res, ex ->
            if (res is SocketSessionManagerCreated) {
                sessionManagerActor = res.actorRef
                logger.info("SocketSessionManager created: ${sessionManagerActor.path()}")
            } else {
                ex?.printStackTrace()
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        actorSystem.terminate()
    }

    @Bean
    fun sessionManagerActor(): ActorRef<UserSessionCommand> {
        return sessionManagerActor
    }
}