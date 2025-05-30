package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import com.example.kotlinbootlabs.ws.handler.auth.EventTextMessage
import com.example.kotlinbootlabs.ws.handler.auth.MessageFrom
import com.example.kotlinbootlabs.ws.handler.auth.MessageType
import com.example.kotlinbootlabs.ws.handler.auth.sendEventTextMessage
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
data class AssignRoom(var roomName:String, val customer: ActorRef<PersonalRoomCommand>, val room:ActorRef<CounselorRoomCommand> ) : CounselorCommand()
data class SetCounselorSocketSession(val socketSession: WebSocketSession) : CounselorCommand()
data class SendToCounselorHandlerTextMessage(val message: String) : CounselorCommand()
data class SendToRoomForPersonalTextMessage(val roomName: String, val message: String) : CounselorCommand()
data class SetCounselorTestProbe(val testProbe: ActorRef<CounselorResponse>) : CounselorCommand()

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

    private val counselorRooms = mutableMapOf<String, ActorRef<CounselorRoomCommand>>()

    private val personalRooms = mutableMapOf<String, ActorRef<PersonalRoomCommand>>()

    private lateinit var testProbe: ActorRef<CounselorResponse>

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
            .onMessage(AssignRoom::class.java, this::onAssignRoom)
            .onMessage(SetCounselorSocketSession::class.java, this::onSetCounselorSocketSession)
            .onMessage(SendToCounselorHandlerTextMessage::class.java, this::onSendToCounselorTextMessage)
            .onMessage(SendToRoomForPersonalTextMessage::class.java, this::onSendToRoomForPersonalTextMessage)
            .onMessage(SetCounselorTestProbe::class.java, this::onSetCounselorTestProbe)
            .build()
    }

    private fun onSetCounselorTestProbe(setCounselorTestProbe: SetCounselorTestProbe): Behavior<CounselorCommand> {
        testProbe = setCounselorTestProbe.testProbe
        return this
    }

    private fun onSendToRoomForPersonalTextMessage(sendToRoomForPersonalTextMessage: SendToRoomForPersonalTextMessage): Behavior<CounselorCommand> {
        if(counselorRooms.containsKey(sendToRoomForPersonalTextMessage.roomName)){
            counselorRooms[sendToRoomForPersonalTextMessage.roomName]?.tell(SendMessageToPersonalRoom(sendToRoomForPersonalTextMessage.message))
        }
        else{
            context.log.error("Room ${sendToRoomForPersonalTextMessage.roomName} not found")
        }
        return this
    }

    private fun onSendToCounselorTextMessage(command: SendToCounselorHandlerTextMessage): Behavior<CounselorCommand> {
        if(socketSession != null){
            //socketSession?.sendMessage(TextMessage("$command.message"))

            sendEventTextMessage(
                socketSession!!, EventTextMessage(
                type = MessageType.CHAT,
                message = "$command.message",
                from = MessageFrom.SYSTEM,
                id = null,
                jsondata = null,
            ))
        }
        else{
            context.log.error("Counselor socketSession  is not initialized - ${context.self.path()}")
        }
        return this
    }

    private fun onSetCounselorSocketSession(setCounselorSocketSession: SetCounselorSocketSession): Behavior<CounselorCommand> {
        context.log.info("Counselor ${context.self.path()} socket session set:  ${setCounselorSocketSession.socketSession}")
        socketSession = setCounselorSocketSession.socketSession

        //socketSession!!.sendMessage(TextMessage("Counselor $name is now connected"))

        sendEventTextMessage(
            socketSession!!, EventTextMessage(
                type = MessageType.INFO,
                message = "Counselor $name is now connected",
                from = MessageFrom.SYSTEM,
                id = null,
                jsondata = null,
            ))


        status = CounselorStatus.ONLINE
        return this
    }

    private fun onAssignRoom(assignRoom: AssignRoom): Behavior<CounselorCommand> {
        context.log.info("Room assigned to counselor $name: ${assignRoom.roomName}")
        counselorRooms[assignRoom.roomName] = assignRoom.room
        personalRooms[assignRoom.roomName] = assignRoom.customer

        if(::testProbe.isInitialized){
            testProbe.tell(TaskAssigned("Room assigned to counselor $name: ${assignRoom.roomName}"))
        }

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