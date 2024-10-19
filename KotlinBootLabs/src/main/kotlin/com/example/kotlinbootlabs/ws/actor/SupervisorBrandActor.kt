package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

sealed class SupervisorBrandCommand
data class CreateCounselorManager(val brand: String, val replyTo: ActorRef<SupervisorBrandResponse>) : SupervisorBrandCommand()
data class GetCounselorManager(val brand: String, val replyTo: ActorRef<SupervisorBrandResponse>) : SupervisorBrandCommand()

sealed class SupervisorBrandResponse
data class CounselorManagerCreated(val brand: String) : SupervisorBrandResponse()
data class CounselorManagerFound(val brand: String, val actorRef: ActorRef<CounselorManagerCommand>) : SupervisorBrandResponse()
data class SupervisorErrorBrandResponse(val message: String) : SupervisorBrandResponse()

class SupervisorBrandActor private constructor(
    context: ActorContext<SupervisorBrandCommand>
) : AbstractBehavior<SupervisorBrandCommand>(context) {

    companion object {
        fun create(): Behavior<SupervisorBrandCommand> {
            return Behaviors.setup { context -> SupervisorBrandActor(context) }
        }
    }

    private val counselorManagers = mutableMapOf<String, ActorRef<CounselorManagerCommand>>()

    override fun createReceive(): Receive<SupervisorBrandCommand> {
        return newReceiveBuilder()
            .onMessage(CreateCounselorManager::class.java, this::onCreateCounselorManager)
            .onMessage(GetCounselorManager::class.java, this::onGetCounselorManager)
            .build()
    }

    private fun onCreateCounselorManager(command: CreateCounselorManager): Behavior<SupervisorBrandCommand> {
        if (counselorManagers.containsKey(command.brand)) {
            command.replyTo.tell(SupervisorErrorBrandResponse("CounselorManager for brand ${command.brand} already exists."))
        } else {
            val counselorManagerActor = context.spawn(CounselorManagerActor.create(), "CounselorManager-${command.brand}")
            counselorManagers[command.brand] = counselorManagerActor
            command.replyTo.tell(CounselorManagerCreated(command.brand))
        }
        return this
    }

    private fun onGetCounselorManager(command: GetCounselorManager): Behavior<SupervisorBrandCommand> {
        val counselorManagerActor = counselorManagers[command.brand]
        if (counselorManagerActor != null) {
            command.replyTo.tell(CounselorManagerFound(command.brand, counselorManagerActor))
        } else {
            command.replyTo.tell(SupervisorErrorBrandResponse("CounselorManager for brand ${command.brand} not found."))
        }
        return this
    }
}