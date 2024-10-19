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

sealed class PersnalRoomCommand
data class SendMessage(val message: String, val replyTo: ActorRef<HelloActorResponse>) : PersnalRoomCommand()
data class SendTextMessage(val message: String) : PersnalRoomCommand()

object AutoOnceProcess : PersnalRoomCommand()
data class SetTestProbe(val testProbe: ActorRef<PersnalRoomResponse>) : PersnalRoomCommand()
data class SetSocketSession(val socketSession: WebSocketSession) : PersnalRoomCommand()
object ClearSocketSession : PersnalRoomCommand()

sealed class PersnalRoomResponse
data class PrivacyHelloResponse(val message: String) : PersnalRoomResponse()


class PersnalRoomActor private constructor(
    context: ActorContext<PersnalRoomCommand>,
    private val identifier: String,
    private val timers: TimerScheduler<PersnalRoomCommand>
) : AbstractBehavior<PersnalRoomCommand>(context) {

    companion object {
        fun create(identifier: String): Behavior<PersnalRoomCommand> {
            return Behaviors.withTimers { timers ->
                Behaviors.setup { context -> PersnalRoomActor(context, identifier, timers) }
            }
        }
    }

    init {
        val randomStartDuration = Duration.ofSeconds(ThreadLocalRandom.current().nextLong(3, 6))
        //timers.startSingleTimer(AutoOnceProcess, randomDuration)
        timers.startTimerAtFixedRate(AutoOnceProcess, randomStartDuration, Duration.ofSeconds(5))
    }

    private val logger = LoggerFactory.getLogger(PersnalRoomActor::class.java)

    private lateinit var testProbe: ActorRef<PersnalRoomResponse>

    private var isRunTimer: Boolean = true

    // TODO : StandAlone 에서 작동가능객체로 ~ 클러스터로 확장시 EventBus 개념적용필요
    private var socketSession: WebSocketSession? = null

    override fun createReceive(): Receive<PersnalRoomCommand> {
        return newReceiveBuilder()
            .onMessage(SetTestProbe::class.java, this::onSetTestProbe)
            .onMessage(SendMessage::class.java, this::onSendMessage)
            .onMessage(SendTextMessage::class.java, this::onSendTextMessage)
            .onMessage(AutoOnceProcess::class.java, this::onAutoOnceProcess)
            .onMessage(SetSocketSession::class.java, this::onSetSocketSession)
            .onMessage(ClearSocketSession::class.java, this::onClearSocketSession)
            .build()
    }

    private fun onSendTextMessage(sendTextMessage: SendTextMessage): Behavior<PersnalRoomCommand> {
        logger.info("OnSendTextMessage received in PrivacyRoomActor ${sendTextMessage.message}")

        if (socketSession != null) {
            try {
                socketSession!!.sendMessage(TextMessage("Echo : ${sendTextMessage.message}"))
            } catch (e: Exception) {
                logger.error("Error sending message: ${e.message}")
                socketSession = null
            }
        } else {
            logger.warn("socketSession is not initialized")
        }

        return this
    }

    private fun onClearSocketSession(clearSocketSession: ClearSocketSession): Behavior<PersnalRoomCommand> {
        logger.info("ClearSocketSession received in PrivacyRoomActor $identifier")
        socketSession = null
        return this
    }

    private fun onSetSocketSession(command: SetSocketSession): Behavior<PersnalRoomCommand> {
        logger.info("OnSetSocketSession received in PrivacyRoomActor $identifier")
        socketSession = command.socketSession

        if(!isRunTimer){
            val randomStartDuration = Duration.ofSeconds(ThreadLocalRandom.current().nextLong(3, 6))
            timers.startTimerAtFixedRate(AutoOnceProcess, randomStartDuration, Duration.ofSeconds(5))
            isRunTimer = true
        }

        return this
    }

    private fun onSetTestProbe(command: SetTestProbe): Behavior<PersnalRoomCommand> {
        logger.info("OnSetTestProbe received in PrivacyRoomActor $identifier")
        testProbe = command.testProbe

        return this
    }

    private fun onAutoOnceProcess(command: AutoOnceProcess): Behavior<PersnalRoomCommand> {
        logger.info("AutoOnceProcess received in PrivacyRoomActor $identifier")
        if (::testProbe.isInitialized) {
            testProbe.tell(PrivacyHelloResponse("Hello World"))
        } else {
            logger.warn("testProbe is not initialized")
        }

        if (socketSession != null) {
            try {
                socketSession!!.sendMessage(TextMessage("Hello World by PrivacyRoomActor $identifier"))
            } catch (e: Exception) {
                logger.error("Error sending message: ${e.message}")
                socketSession = null
                timers.cancel(AutoOnceProcess)
                isRunTimer = false
            }
        } else {
            logger.warn("socketSession is not initialized")
            timers.cancel(AutoOnceProcess)
            isRunTimer = false
        }
        return this
    }

    private fun onSendMessage(command: SendMessage): Behavior<PersnalRoomCommand> {
        logger.info("Message received in PrivacyRoomActor $identifier: ${command.message}")

        command.replyTo.tell(HelloResponse("Kotlin"))
        return this
    }
}