package com.example.kotlinbootlabs.ws.actor

import com.example.kotlinbootlabs.actor.CreateSocketSessionManager
import com.example.kotlinbootlabs.actor.MainStageActorResponse
import com.example.kotlinbootlabs.actor.SocketSessionManagerCreated
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.*
import java.time.Duration
import java.util.concurrent.CompletionStage
import kotlin.math.log

sealed class CounselorManagerCommand
data class CreateCounselor(val name: String, val replyTo: ActorRef<CounselorManagerResponse>) : CounselorManagerCommand()
data class CreateRoom(val roomName: String, val replyTo: ActorRef<CounselorManagerResponse>) : CounselorManagerCommand()
data class RequestCounseling(val roomName: String, val persnalRoomActor: ActorRef<PersnalRoomCommand>, val replyTo: ActorRef<CounselorRoomResponse>) : CounselorManagerCommand()
data class GetCounselor(val name: String, val replyTo: ActorRef<CounselorManagerResponse>) : CounselorManagerCommand()
data class GetCounselorRoom(val roomName: String, val replyTo: ActorRef<CounselorManagerResponse>) : CounselorManagerCommand()

sealed class CounselorManagerResponse
data class CounselorCreated(val name: String) : CounselorManagerResponse()
data class RoomCreated(val room: ActorRef<PersnalRoomCommand>) : CounselorManagerResponse()
data class ErrorResponse(val message: String) : CounselorManagerResponse()
data class CounselorFound(val name: String, val actorRef: ActorRef<CounselorCommand>) : CounselorManagerResponse()
data class CounselorRoomFound(val roomName: String, val actorRef: ActorRef<CounselorRoomCommand>) : CounselorManagerResponse()

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
            .onMessage(RequestCounseling::class.java, this::onRequestCounseling)
            .onMessage(GetCounselor::class.java, this::onGetCounselor)
            .onMessage(GetCounselorRoom::class.java, this::onGetCounselorRoom)
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

    private fun onRequestCounseling(command: RequestCounseling): Behavior<CounselorManagerCommand> {
        val availableCounselor = counselors.values.firstOrNull()
        if (availableCounselor == null) {
            //command.replyTo.tell(ErrorResponse("No available counselors."))
            context.log.error("No available counselors.")
            return this
        }

        // Create Room
        val roomName = command.roomName
        val counselorRoomActor = context.spawn(CounselorRoomActor.create(roomName), roomName)
        counselorRooms[roomName] = counselorRoomActor

        // Invite PersnalRoomActor
        //counselorRoomActor.tell(InvitePersnalRoomActor(command.persnalRoomActor, context.self.narrow() ))

        // Send CreateSocketSessionManager event and handle the response
        val response: CompletionStage<CounselorRoomResponse> = AskPattern.ask(
            counselorRoomActor,
            { replyTo: ActorRef<CounselorRoomResponse> -> InvitePersnalRoomActor(command.persnalRoomActor, replyTo) },
            Duration.ofSeconds(3),
            context.system.scheduler()
        )

        response.whenComplete { res, ex ->
            if (res is CounselorRoomResponse) {
                //command.replyTo.tell(RoomCreated(command.persnalRoomActor))
                //command.persnalRoomActor.tell(SetTestProbe(context.self.narrow()))
                command.persnalRoomActor.tell(SendTextMessage("Invitation to counseling room completed."))

                availableCounselor.tell(AsignRoom(command.persnalRoomActor, counselorRoomActor))

            } else {
                ex?.printStackTrace()
            }
        }

        command.replyTo.tell(InvitationCompleted)

        return this
    }

    private fun onGetCounselor(command: GetCounselor): Behavior<CounselorManagerCommand> {
        var counselorActor = counselors[command.name]
        if(counselorActor == null) {
            counselorActor = context.spawn(CounselorActor.create(command.name), command.name)
            counselors[command.name] = counselorActor
        }

        command.replyTo.tell(counselorActor?.let { CounselorFound(command.name, it) })

        return this
    }

    private fun onGetCounselorRoom(command: GetCounselorRoom): Behavior<CounselorManagerCommand> {

        var counselorRoomActor = counselorRooms[command.roomName]
        if(counselorRoomActor == null) {
            counselorRoomActor = context.spawn(CounselorRoomActor.create(command.roomName), command.roomName)
            counselorRooms[command.roomName] = counselorRoomActor
        }

        command.replyTo.tell(counselorRoomActor?.let { CounselorRoomFound(command.roomName, it) })
        return this
    }

}