package com.example.kotlinbootlabs.ws.actor


import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

sealed class CounselorRoomCommand
data class InvitePersnalRoomActor(val persnalRoomActor: ActorRef<PersnalRoomCommand>) : CounselorRoomCommand()

class CounselorRoomActor private constructor(
    context: ActorContext<CounselorRoomCommand>,
    private val name: String
) : AbstractBehavior<CounselorRoomCommand>(context) {

    companion object {
        fun create(name: String): Behavior<CounselorRoomCommand> {
            return Behaviors.setup { context -> CounselorRoomActor(context, name) }
        }
    }

    override fun createReceive(): Receive<CounselorRoomCommand> {
        return newReceiveBuilder()
            .onMessage(InvitePersnalRoomActor::class.java, this::onInvitePersnalRoomActor)
            .build()
    }

    private fun onInvitePersnalRoomActor(command: InvitePersnalRoomActor): Behavior<CounselorRoomCommand> {
        // Logic to handle the invitation of PersnalRoomActor
        context.log.info("Invited PersnalRoomActor: ${command.persnalRoomActor}")
        return this
    }
}