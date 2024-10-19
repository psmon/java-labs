package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

sealed class CounselorManagerCommand
data class CreateCounselor(val name: String, val replyTo: ActorRef<CounselorManagerResponse>) : CounselorManagerCommand()
data class CreateRoom(val roomName: String, val replyTo: ActorRef<CounselorManagerResponse>) : CounselorManagerCommand()

sealed class CounselorManagerResponse
data class CounselorCreated(val name: String) : CounselorManagerResponse()
data class RoomCreated(val room: ActorRef<PersnalRoomCommand>) : CounselorManagerResponse()
data class ErrorResponse(val message: String) : CounselorManagerResponse()

class CounselorManagerActor private constructor(
    context: ActorContext<CounselorManagerCommand>
) : AbstractBehavior<CounselorManagerCommand>(context) {

    companion object {
        fun create(): Behavior<CounselorManagerCommand> {
            return Behaviors.setup { context -> CounselorManagerActor(context) }
        }
    }

    private val counselors = mutableMapOf<String, ActorRef<CounselorCommand>>()
    private val counselorRooms = mutableMapOf<String, ActorRef<CounselorRoomCommand>>()

    override fun createReceive(): Receive<CounselorManagerCommand> {
        return newReceiveBuilder()
            .onMessage(CreateCounselor::class.java, this::onCreateCounselor)
            .onMessage(CreateRoom::class.java, this::onCreateRoom)
            .build()
    }

    private fun onCreateCounselor(command: CreateCounselor): Behavior<CounselorManagerCommand> {
        if (counselors.containsKey(command.name)) {
            command.replyTo.tell(ErrorResponse("Counselor ${command.name} already exists."))
        } else {
            val counselorActor = context.spawn(CounselorActor.create(command.name), command.name)
            counselors[command.name] = counselorActor
            command.replyTo.tell(CounselorCreated(command.name))
        }
        return this
    }

    private fun onCreateRoom(command: CreateRoom): Behavior<CounselorManagerCommand> {
        if (counselorRooms.containsKey(command.roomName)) {
            command.replyTo.tell(ErrorResponse("Counselor ${command.roomName} already exists."))
        } else {
            val counselorRoomActor = context.spawn(CounselorRoomActor.create(command.roomName), command.roomName)
            counselorRooms[command.roomName] = counselorRoomActor
            command.replyTo.tell(CounselorCreated(command.roomName))
        }
        return this
    }
}