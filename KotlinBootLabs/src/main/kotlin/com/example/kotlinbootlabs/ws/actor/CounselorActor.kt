package com.example.kotlinbootlabs.ws.actor

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.*
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

enum class CounselorStatus {
    Connecting,
    ONLINE,
    OFFLINE
}

enum class AwayStatus {
    BREAK,
    LUNCH,
    TRAINING,
    MEETING,
    OTHER_TASK,
    ADDITIONAL_TASK
}

sealed class CounselorCommand
data class AssignTask(val task: String, val replyTo: ActorRef<CounselorResponse>) : CounselorCommand()
data class GoOffline(val awayStatus: AwayStatus, val replyTo: ActorRef<CounselorResponse>) : CounselorCommand()
data class GoOnline(val replyTo: ActorRef<CounselorResponse>) : CounselorCommand()
data class AsignRoom(val customer: ActorRef<PersnalRoomCommand>, val room:ActorRef<CounselorRoomCommand> ) : CounselorCommand()
data class SetCounselorSocketSession(val socketSession: WebSocketSession) : CounselorCommand()
data class SendToCounselorHandelerTextMessage(val message: String) : CounselorCommand()

sealed class CounselorResponse
data class TaskAssigned(val task: String) : CounselorResponse()
data class StatusChanged(val status: CounselorStatus, val awayStatus: AwayStatus? = null) : CounselorResponse()

class CounselorActor private constructor(
    context: ActorContext<CounselorCommand>,
    private val name: String
) : AbstractBehavior<CounselorCommand>(context) {

    private var status: CounselorStatus = CounselorStatus.OFFLINE
    private var awayStatus: AwayStatus? = null

    private var socketSession: WebSocketSession? = null

    companion object {
        fun create(name: String): Behavior<CounselorCommand> {
            return Behaviors.setup { context -> CounselorActor(context, name) }
        }
    }

    override fun createReceive(): Receive<CounselorCommand> {
        return newReceiveBuilder()
            .onMessage(AssignTask::class.java, this::onAssignTask)
            .onMessage(GoOffline::class.java, this::onGoOffline)
            .onMessage(GoOnline::class.java, this::onGoOnline)
            .onMessage(AsignRoom::class.java, this::onAsignRoom)
            .onMessage(SetCounselorSocketSession::class.java, this::onSetCounselorSocketSession)
            .onMessage(SendToCounselorHandelerTextMessage::class.java, this::onSendToCounselorTextMessage)
            .build()
    }

    private fun onSendToCounselorTextMessage(command: SendToCounselorHandelerTextMessage): Behavior<CounselorCommand> {

        if(socketSession!=null){
            socketSession!!.sendMessage(TextMessage("${command.message}"))
        }
        return this
    }

    private fun onSetCounselorSocketSession(setCounselorSocketSession: SetCounselorSocketSession?): Behavior<CounselorCommand>? {
        context.log.info("Counselor $name socket session set: ${setCounselorSocketSession?.socketSession}")
        socketSession = setCounselorSocketSession?.socketSession
        socketSession?.sendMessage(TextMessage("Counselor $name is now connected"))
        return this
    }

    private fun onAsignRoom(asignRoom: AsignRoom): Behavior<CounselorCommand> {

        return this
    }

    private fun onAssignTask(command: AssignTask): Behavior<CounselorCommand> {
        context.log.info("Task assigned to counselor $name: ${command.task}")
        command.replyTo.tell(TaskAssigned(command.task))
        return this
    }

    private fun onGoOffline(command: GoOffline): Behavior<CounselorCommand> {
        context.log.info("Counselor $name is now offline: ${command.awayStatus}")
        status = CounselorStatus.OFFLINE
        awayStatus = command.awayStatus
        command.replyTo.tell(StatusChanged(status, awayStatus))
        return this
    }

    private fun onGoOnline(command: GoOnline): Behavior<CounselorCommand> {
        context.log.info("Counselor $name is now online")
        status = CounselorStatus.ONLINE
        awayStatus = null
        command.replyTo.tell(StatusChanged(status, awayStatus))
        return this
    }
}