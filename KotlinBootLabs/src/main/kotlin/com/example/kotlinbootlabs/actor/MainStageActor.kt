package com.example.kotlinbootlabs.actor

import com.example.kotlinbootlabs.ws.actor.SupervisorChannelCommand
import com.example.kotlinbootlabs.ws.actor.SupervisorChannelActor
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.SupervisorStrategy
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import com.example.kotlinbootlabs.ws.actor.UserSessionManagerActor
import com.example.kotlinbootlabs.ws.actor.UserSessionCommand

sealed class MainStageActorCommand
data class CreateSocketSessionManager(val replyTo: ActorRef<MainStageActorResponse>) : MainStageActorCommand()
data class CreateSupervisorChannelActor(val replyTo: ActorRef<MainStageActorResponse>) : MainStageActorCommand()

sealed class MainStageActorResponse
data class SocketSessionManagerCreated(val actorRef: ActorRef<UserSessionCommand>) : MainStageActorResponse()
data class SupervisorChannelActorCreated(val actorRef: ActorRef<SupervisorChannelCommand>) : MainStageActorResponse()


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
            .onMessage(CreateSupervisorChannelActor::class.java, this::onCreateSupervisorChannelActor)
            .build()
    }

    private fun onSocketSessionManager(command: CreateSocketSessionManager): Behavior<MainStageActorCommand> {
        val sessionManagerActor = context.spawn(
            Behaviors.supervise(UserSessionManagerActor.create())
                .onFailure(SupervisorStrategy.resume()),
            "sessionManagerActor"
        )
        context.watch(sessionManagerActor)

        command.replyTo.tell(SocketSessionManagerCreated(sessionManagerActor))

        return this
    }

    private fun onCreateSupervisorChannelActor(command: CreateSupervisorChannelActor): Behavior<MainStageActorCommand> {
        val supervisorChannelActor = context.spawn(
            Behaviors.supervise(SupervisorChannelActor.create())
                .onFailure(SupervisorStrategy.resume()),
            "supervisorChannelActor"
        )
        context.watch(supervisorChannelActor)

        command.replyTo.tell(SupervisorChannelActorCreated(supervisorChannelActor))

        return this
    }

}