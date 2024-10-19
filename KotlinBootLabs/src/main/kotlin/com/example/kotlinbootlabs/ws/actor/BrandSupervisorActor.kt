package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*

sealed class BrandSupervisorCommand
data class CreateCounselorManager(val brand: String, val replyTo: ActorRef<BrandSupervisorResponse>) : BrandSupervisorCommand()
data class GetCounselorManager(val brand: String, val replyTo: ActorRef<BrandSupervisorResponse>) : BrandSupervisorCommand()

sealed class BrandSupervisorResponse
data class CounselorManagerCreated(val brand: String, val actorRef: ActorRef<CounselorManagerCommand>) : BrandSupervisorResponse()
data class CounselorManagerFound(val brand: String, val actorRef: ActorRef<CounselorManagerCommand>) : BrandSupervisorResponse()
data class BrandSupervisorErrorResponse(val message: String) : BrandSupervisorResponse()

class BrandSupervisorActor private constructor(
    context: ActorContext<BrandSupervisorCommand>
) : AbstractBehavior<BrandSupervisorCommand>(context) {

    companion object {
        fun create(): Behavior<BrandSupervisorCommand> {
            return Behaviors.setup { context -> BrandSupervisorActor(context) }
        }
    }

    private val counselorManagers = mutableMapOf<String, ActorRef<CounselorManagerCommand>>()

    override fun createReceive(): Receive<BrandSupervisorCommand> {
        return newReceiveBuilder()
            .onMessage(CreateCounselorManager::class.java, this::onCreateCounselorManager)
            .onMessage(GetCounselorManager::class.java, this::onGetCounselorManager)
            .build()
    }

    private fun onCreateCounselorManager(command: CreateCounselorManager): Behavior<BrandSupervisorCommand> {
        if (counselorManagers.containsKey(command.brand)) {
            command.replyTo.tell(BrandSupervisorErrorResponse("CounselorManager for brand ${command.brand} already exists."))
        } else {
            val counselorManagerActor = context.spawn(CounselorManagerActor.create(), "CounselorManager-${command.brand}")
            counselorManagers[command.brand] = counselorManagerActor
            command.replyTo.tell(CounselorManagerCreated(command.brand, counselorManagerActor))
        }
        return this
    }

    private fun onGetCounselorManager(command: GetCounselorManager): Behavior<BrandSupervisorCommand> {
        val counselorManagerActor = counselorManagers[command.brand]
        if (counselorManagerActor != null) {
            command.replyTo.tell(CounselorManagerFound(command.brand, counselorManagerActor))
        } else {
            command.replyTo.tell(BrandSupervisorErrorResponse("CounselorManager for brand ${command.brand} not found."))
        }
        return this
    }
}