package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.slf4j.LoggerFactory

sealed class PrivacyRoomCommand
data class SendMessage(val message: String) : PrivacyRoomCommand()

class PrivacyRoomActor private constructor(
    context: ActorContext<PrivacyRoomCommand>,
    private val identifier: String
) : AbstractBehavior<PrivacyRoomCommand>(context) {

    companion object {
        fun create(identifier: String): Behavior<PrivacyRoomCommand> {
            return Behaviors.setup { context -> PrivacyRoomActor(context, identifier) }
        }
    }

    private val logger = LoggerFactory.getLogger(PrivacyRoomActor::class.java)

    override fun createReceive(): Receive<PrivacyRoomCommand> {
        return newReceiveBuilder()
            .onMessage(SendMessage::class.java, this::onSendMessage)
            .build()
    }

    private fun onSendMessage(command: SendMessage): Behavior<PrivacyRoomCommand> {
        logger.info("Message received in PrivacyRoomActor $identifier: ${command.message}")
        return this
    }
}