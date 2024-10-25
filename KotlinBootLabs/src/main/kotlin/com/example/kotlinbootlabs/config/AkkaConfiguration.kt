package com.example.kotlinbootlabs.config

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.AskPattern
import com.example.kotlinbootlabs.actor.*
import com.example.kotlinbootlabs.ws.actor.*
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage


@Configuration
class AkkaConfiguration {

    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(AkkaConfiguration::class.java)

    private lateinit var actorSystem: ActorSystem<MainStageActorCommand>

    private lateinit var sessionManagerActor: CompletableFuture<ActorRef<UserSessionCommand>>

    private lateinit var supervisorChannelActor: CompletableFuture<ActorRef<SupervisorChannelCommand>>

    @PostConstruct
    fun init() {
        actorSystem = ActorSystem.create(MainStageActor.create(), "MainStageActor")

        sessionManagerActor = AskPattern.ask(
            actorSystem,
            { replyTo: ActorRef<MainStageActorResponse> -> CreateSocketSessionManager(replyTo) },
            Duration.ofSeconds(3),
            actorSystem.scheduler()
        ).toCompletableFuture().thenApply { res ->
            if (res is SocketSessionManagerCreated) {
                logger.info("SocketSessionManager created: ${res.actorRef.path()}")
                res.actorRef
            } else {
                throw IllegalStateException("Failed to create SocketSessionManager")
            }
        }

        supervisorChannelActor = AskPattern.ask(
            actorSystem,
            { replyTo: ActorRef<MainStageActorResponse> -> CreateSupervisorChannelActor(replyTo) },
            Duration.ofSeconds(3),
            actorSystem.scheduler()
        ).toCompletableFuture().thenApply { res ->
            if (res is SupervisorChannelActorCreated) {
                logger.info("SupervisorChannelActor created: ${res.actorRef.path()}")
                res.actorRef
            } else {
                throw IllegalStateException("Failed to create SupervisorChannelActor")
            }
        }

    }

    @PreDestroy
    fun shutdown() {
        actorSystem.terminate()
    }

    @Bean
    fun actorSystem(): ActorSystem<MainStageActorCommand> {
        return actorSystem
    }

    @Bean
    fun sessionManagerActor(): ActorRef<UserSessionCommand> {
        return sessionManagerActor.get()
    }

    @Bean
    fun supervisorChannelActor(): ActorRef<SupervisorChannelCommand> {
        return supervisorChannelActor.get()
    }
}