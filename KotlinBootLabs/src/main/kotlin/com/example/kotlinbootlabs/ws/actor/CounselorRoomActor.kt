package com.example.kotlinbootlabs.ws.actor

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.*

enum class CounselorRoomStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED
}

sealed class CounselorRoomCommand
data class InvitePersnalRoomActor(val persnalRoomActor: ActorRef<PersnalRoomCommand>, val replyTo: ActorRef<CounselorRoomResponse>) : CounselorRoomCommand()
data class ChangeStatus(val status: CounselorRoomStatus, val replyTo: ActorRef<CounselorRoomResponse>) : CounselorRoomCommand()
data class AsingCounselor(val counselorActor: ActorRef<CounselorCommand>) : CounselorRoomCommand()

sealed class CounselorRoomResponse
object InvitationCompleted : CounselorRoomResponse()
data class StatusChangeCompleted(val status: CounselorRoomStatus) : CounselorRoomResponse()

class CounselorRoomActor private constructor(
    context: ActorContext<CounselorRoomCommand>,
    private val name: String
) : AbstractBehavior<CounselorRoomCommand>(context) {

    private var status: CounselorRoomStatus = CounselorRoomStatus.WAITING

    private lateinit var persnalRoom: ActorRef<PersnalRoomCommand>

    private lateinit var counselor: ActorRef<CounselorCommand>

    companion object {
        fun create(name: String): Behavior<CounselorRoomCommand> {
            return Behaviors.setup { context -> CounselorRoomActor(context, name) }
        }
    }

    override fun createReceive(): Receive<CounselorRoomCommand> {
        return newReceiveBuilder()
            .onMessage(InvitePersnalRoomActor::class.java, this::onInvitePersnalRoomActor)
            .onMessage(ChangeStatus::class.java, this::onChangeStatus)
            .onMessage(AsingCounselor::class.java, this::onAsingCounselor)
            .build()
    }

    private fun onAsingCounselor(asingCounselor: AsingCounselor): Behavior<CounselorRoomCommand> {
        counselor = asingCounselor.counselorActor
        return this
    }

    private fun onInvitePersnalRoomActor(command: InvitePersnalRoomActor): Behavior<CounselorRoomCommand> {
        // Logic to handle the invitation of PersnalRoomActor
        context.log.info("Invited PersnalRoomActor: ${command.persnalRoomActor}")
        persnalRoom = command.persnalRoomActor

        command.replyTo.tell(InvitationCompleted)
        return this
    }

    private fun onChangeStatus(command: ChangeStatus): Behavior<CounselorRoomCommand> {
        status = command.status
        context.log.info("CounselorRoom status changed to: $status")
        command.replyTo.tell(StatusChangeCompleted(status))
        return this
    }
}