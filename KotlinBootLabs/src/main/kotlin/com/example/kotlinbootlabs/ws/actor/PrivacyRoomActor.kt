package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import com.example.kotlinbootlabs.actor.HelloActorResponse
import com.example.kotlinbootlabs.actor.HelloResponse
import org.slf4j.LoggerFactory
import java.time.Duration

sealed class PrivacyRoomCommand
data class SendMessage(val message: String, val replyTo: ActorRef<HelloActorResponse>) : PrivacyRoomCommand()
object AutoOnceProcess : PrivacyRoomCommand()
data class SetTestProbe(val testProbe: ActorRef<HelloActorResponse>) : PrivacyRoomCommand()

class PrivacyRoomActor private constructor(
    context: ActorContext<PrivacyRoomCommand>,
    private val identifier: String,
    private val timers: TimerScheduler<PrivacyRoomCommand>
) : AbstractBehavior<PrivacyRoomCommand>(context) {

    companion object {
        fun create(identifier: String): Behavior<PrivacyRoomCommand> {
            return Behaviors.withTimers { timers ->
                Behaviors.setup { context -> PrivacyRoomActor(context, identifier, timers) }
            }
        }
    }

    init {
        timers.startSingleTimer(AutoOnceProcess, Duration.ofSeconds(2))
    }

    private val logger = LoggerFactory.getLogger(PrivacyRoomActor::class.java)

    private lateinit var testProbe: ActorRef<HelloActorResponse>

    override fun createReceive(): Receive<PrivacyRoomCommand> {
        return newReceiveBuilder()
            .onMessage(SetTestProbe::class.java, this::onSetTestProbe)
            .onMessage(SendMessage::class.java, this::onSendMessage)
            .onMessage(AutoOnceProcess::class.java, this::onAuthOnceProcess)
            .build()
    }

    private fun onSetTestProbe(command: SetTestProbe): Behavior<PrivacyRoomCommand> {
        logger.info("OnSetTestProbe received in PrivacyRoomActor $identifier")
        testProbe = command.testProbe

        return this
    }

    private fun onAuthOnceProcess(command: AutoOnceProcess): Behavior<PrivacyRoomCommand> {
        logger.info("AutoOnceProcess received in PrivacyRoomActor $identifier")
        if (::testProbe.isInitialized) {
            testProbe.tell(HelloResponse("Kotlin"))
        } else {
            logger.warn("testProbe is not initialized")
        }

        return this
    }

    private fun onSendMessage(command: SendMessage): Behavior<PrivacyRoomCommand> {
        logger.info("Message received in PrivacyRoomActor $identifier: ${command.message}")

        command.replyTo.tell(HelloResponse("Kotlin"))
        return this
    }
}