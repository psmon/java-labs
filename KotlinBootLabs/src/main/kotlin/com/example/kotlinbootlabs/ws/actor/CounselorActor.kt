package com.example.kotlinbootlabs.ws.actor

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.*

enum class CounselorStatus {
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

sealed class CounselorResponse
data class TaskAssigned(val task: String) : CounselorResponse()
data class StatusChanged(val status: CounselorStatus, val awayStatus: AwayStatus? = null) : CounselorResponse()

class CounselorActor private constructor(
    context: ActorContext<CounselorCommand>,
    private val name: String
) : AbstractBehavior<CounselorCommand>(context) {

    private var status: CounselorStatus = CounselorStatus.ONLINE
    private var awayStatus: AwayStatus? = null

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
            .build()
    }

    private fun onAssignTask(command: AssignTask): Behavior<CounselorCommand> {
        context.log.info("Task assigned to counselor $name: ${command.task}")
        command.replyTo.tell(TaskAssigned(command.task))
        return this
    }

    private fun onGoOffline(command: GoOffline): Behavior<CounselorCommand> {
        status = CounselorStatus.OFFLINE
        awayStatus = command.awayStatus
        context.log.info("Counselor $name is now offline: ${command.awayStatus}")
        command.replyTo.tell(StatusChanged(status, awayStatus))
        return this
    }

    private fun onGoOnline(command: GoOnline): Behavior<CounselorCommand> {
        status = CounselorStatus.ONLINE
        awayStatus = null
        context.log.info("Counselor $name is now online")
        command.replyTo.tell(StatusChanged(status, awayStatus))
        return this
    }
}