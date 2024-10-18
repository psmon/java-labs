package com.example.kotlinbootlabs.ws.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import com.example.kotlinbootlabs.actor.HelloActorResponse
import com.example.kotlinbootlabs.actor.HelloResponse
import org.slf4j.LoggerFactory
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

sealed class PrivacyRoomCommand
data class SendMessage(val message: String, val replyTo: ActorRef<HelloActorResponse>) : PrivacyRoomCommand()
object AutoOnceProcess : PrivacyRoomCommand()
data class SetTestProbe(val testProbe: ActorRef<PrivacyRoomResponse>) : PrivacyRoomCommand()
data class SetSocketSession(val socketSession: WebSocketSession) : PrivacyRoomCommand()
object ClearSocketSession : PrivacyRoomCommand()

sealed class PrivacyRoomResponse
data class PrivacyHelloResponse(val message: String) : PrivacyRoomResponse()


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
        val randomDuration = Duration.ofSeconds(ThreadLocalRandom.current().nextLong(3, 6))
        timers.startSingleTimer(AutoOnceProcess, randomDuration)
    }

    private val logger = LoggerFactory.getLogger(PrivacyRoomActor::class.java)

    private lateinit var testProbe: ActorRef<PrivacyRoomResponse>

    // TODO : StandAlone 에서 작동가능객체로 ~ 클러스터로 확장시 EventBus 개념적용필요
    private var socketSession: WebSocketSession? = null

    override fun createReceive(): Receive<PrivacyRoomCommand> {
        return newReceiveBuilder()
            .onMessage(SetTestProbe::class.java, this::onSetTestProbe)
            .onMessage(SendMessage::class.java, this::onSendMessage)
            .onMessage(AutoOnceProcess::class.java, this::onAutoOnceProcess)
            .onMessage(SetSocketSession::class.java, this::onSetSocketSession)
            .onMessage(ClearSocketSession::class.java, this::onClearSocketSession)
            .build()
    }

    private fun onClearSocketSession(clearSocketSession: ClearSocketSession): Behavior<PrivacyRoomCommand> {
        logger.info("ClearSocketSession received in PrivacyRoomActor $identifier")
        socketSession = null
        return this
    }

    private fun onSetSocketSession(command: SetSocketSession): Behavior<PrivacyRoomCommand> {
        logger.info("OnSetSocketSession received in PrivacyRoomActor $identifier")
        socketSession = command.socketSession
        return this
    }

    private fun onSetTestProbe(command: SetTestProbe): Behavior<PrivacyRoomCommand> {
        logger.info("OnSetTestProbe received in PrivacyRoomActor $identifier")
        testProbe = command.testProbe

        return this
    }

    private fun onAutoOnceProcess(command: AutoOnceProcess): Behavior<PrivacyRoomCommand> {
        logger.info("AutoOnceProcess received in PrivacyRoomActor $identifier")
        if (::testProbe.isInitialized) {
            testProbe.tell(PrivacyHelloResponse("Hello World"))
        } else {
            logger.warn("testProbe is not initialized")
        }

        if (socketSession != null) {
            socketSession!!.sendMessage(TextMessage("Hello World by AutoOnceProcess"))
        } else {
            logger.warn("socketSession is not initialized")
        }
        return this
    }

    private fun onSendMessage(command: SendMessage): Behavior<PrivacyRoomCommand> {
        logger.info("Message received in PrivacyRoomActor $identifier: ${command.message}")

        command.replyTo.tell(HelloResponse("Kotlin"))
        return this
    }
}