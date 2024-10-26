package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

enum class CounselorRoomStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED
}

sealed class CounselorRoomCommand
data class InvitePersonalRoomActor(val personalRoomActor: ActorRef<PersonalRoomCommand>, val replyTo: ActorRef<CounselorRoomResponse>) : CounselorRoomCommand()
data class ChangeStatus(val status: CounselorRoomStatus, val replyTo: ActorRef<CounselorRoomResponse>) : CounselorRoomCommand()
data class AssignCounselor(val counselorActor: ActorRef<CounselorCommand>) : CounselorRoomCommand()
data class SendMessageToPersonalRoom(val message: String) : CounselorRoomCommand()
data class SendToCounselor(val message: String) : CounselorRoomCommand()

sealed class CounselorRoomResponse
data object InvitationCompleted : CounselorRoomResponse()
data class StatusChangeCompleted(val status: CounselorRoomStatus) : CounselorRoomResponse()

class CounselorRoomActor private constructor(
    context: ActorContext<CounselorRoomCommand>,
    private val name: String
) : AbstractBehavior<CounselorRoomCommand>(context) {

    private var status: CounselorRoomStatus = CounselorRoomStatus.WAITING

    private lateinit var personalRoom: ActorRef<PersonalRoomCommand>

    private lateinit var counselor: ActorRef<CounselorCommand>

    companion object {
        fun create(name: String): Behavior<CounselorRoomCommand> {
            return Behaviors.setup { context -> CounselorRoomActor(context, name) }
        }
    }

    override fun createReceive(): Receive<CounselorRoomCommand> {
        return newReceiveBuilder()
            .onMessage(InvitePersonalRoomActor::class.java, this::onInvitePersonalRoomActor)
            .onMessage(ChangeStatus::class.java, this::onChangeStatus)
            .onMessage(AssignCounselor::class.java, this::onAssignCounselor)
            .onMessage(SendMessageToPersonalRoom::class.java, this::onSendMessageToPersonalRoom)
            .onMessage(SendToCounselor::class.java, this::onSendToCounselor)
            .build()
    }

    private fun onSendToCounselor(sendToCounselor: SendToCounselor?): Behavior<CounselorRoomCommand>? {
        if(::counselor.isInitialized){
            counselor.tell(SendToCounselorHandlerTextMessage(sendToCounselor!!.message))
        }
        else{
            context.log.error("CounselorActor is not initialized")
        }
        return this
    }

    private fun onSendMessageToPersonalRoom(sendMessageToPersonalRoom: SendMessageToPersonalRoom): Behavior<CounselorRoomCommand> {
        if(::personalRoom.isInitialized){
            personalRoom.tell(SendTextMessage(sendMessageToPersonalRoom.message))
        }
        else
        {
            context.log.error("PersonalRoomActor is not initialized")
        }

        return this
    }

    private fun onAssignCounselor(command: AssignCounselor): Behavior<CounselorRoomCommand> {
        counselor = command.counselorActor
        return this
    }

    private fun onInvitePersonalRoomActor(command: InvitePersonalRoomActor): Behavior<CounselorRoomCommand> {
        // Logic to handle the invitation of PersonalRoomActor
        context.log.info("Invited PersonalRoomActor: ${command.personalRoomActor}")
        personalRoom = command.personalRoomActor

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