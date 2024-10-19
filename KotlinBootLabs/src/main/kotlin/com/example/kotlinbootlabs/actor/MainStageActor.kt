package com.example.kotlinbootlabs.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.example.kotlinbootlabs.ws.actor.UserSessionManagerActor
import com.example.kotlinbootlabs.ws.actor.UserSessionCommand

sealed class MainStageActorCommand
data class CreateSocketSessionManager(val replyTo: ActorRef<MainStageActorResponse>) : MainStageActorCommand()

sealed class MainStageActorResponse
data class SocketSessionManagerCreated(val actorRef: ActorRef<UserSessionCommand>) : MainStageActorResponse()

class MainStageActor private constructor(
    private val context: ActorContext<MainStageActorCommand>,
) : AbstractBehavior<MainStageActorCommand>(context) {

    companion object {
        fun create(): Behavior<MainStageActorCommand> {
            return Behaviors.setup { context -> MainStageActor(context) }
        }
    }

    override fun createReceive(): Receive<MainStageActorCommand> {
        return newReceiveBuilder()
            .onMessage(CreateSocketSessionManager::class.java, this::onSocketSessionManager)
            .build()
    }

    private fun onSocketSessionManager(command: CreateSocketSessionManager): Behavior<MainStageActorCommand> {
        val sessionManagerActor = context.spawn(
            Behaviors.supervise(UserSessionManagerActor.create())
                .onFailure(SupervisorStrategy.resume()),
            "socketSessionManager"
        )
        context.watch(sessionManagerActor)

        command.replyTo.tell(SocketSessionManagerCreated(sessionManagerActor))

        return this
    }
}