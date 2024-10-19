package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*


sealed class CounselorCommand
data class AsignTask(val task: String, val replyTo: ActorRef<CounselorResponse>) : CounselorCommand()

sealed class CounselorResponse
data class TaskAssigned(val task: String) : CounselorResponse()


class CounselorActor private constructor(
    context: ActorContext<CounselorCommand>,
    private val name: String
) : AbstractBehavior<CounselorCommand>(context) {

    companion object {
        fun create(name: String): Behavior<CounselorCommand> {
            return Behaviors.setup { context -> CounselorActor(context, name) }
        }
    }

    override fun createReceive(): Receive<CounselorCommand> {
        return newReceiveBuilder()
            .onMessage(AsignTask::class.java, this::onAssignTask)
            .build()
    }

    private fun onAssignTask(command: AsignTask): Behavior<CounselorCommand> {
        context.log.info("Task assigned to counselor $name: ${command.task}")
        command.replyTo.tell(TaskAssigned(command.task))
        return this
    }
}