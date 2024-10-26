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

sealed class PersonalRoomCommand
data class SendMessage(val message: String, val replyTo: ActorRef<HelloActorResponse>) : PersonalRoomCommand()
data class SendTextMessage(val message: String) : PersonalRoomCommand()
data object AutoOnceProcess : PersonalRoomCommand()
data class SetTestProbe(val testProbe: ActorRef<PersonalRoomResponse>) : PersonalRoomCommand()
data class SetSocketSession(val socketSession: WebSocketSession) : PersonalRoomCommand()
data object ClearSocketSession : PersonalRoomCommand()
data class SendToCounselorRoomForCounseling(val message: String) : PersonalRoomCommand()

sealed class PersonalRoomResponse
data class PrivacyHelloResponse(val message: String) : PersonalRoomResponse()
data class SetCounselorRoom(val counselorRoomActor: ActorRef<CounselorRoomCommand>) : PersonalRoomCommand()


class PersonalRoomActor private constructor(
    context: ActorContext<PersonalRoomCommand>,
    private val identifier: String,
    private val timers: TimerScheduler<PersonalRoomCommand>
) : AbstractBehavior<PersonalRoomCommand>(context) {

    companion object {
        fun create(identifier: String): Behavior<PersonalRoomCommand> {
            return Behaviors.withTimers { timers ->
                Behaviors.setup { context -> PersonalRoomActor(context, identifier, timers) }
            }
        }
    }

    init {
        val randomStartDuration = Duration.ofSeconds(ThreadLocalRandom.current().nextLong(3, 6))
        //timers.startSingleTimer(AutoOnceProcess, randomDuration)
        timers.startTimerAtFixedRate(AutoOnceProcess, randomStartDuration, Duration.ofSeconds(60))
    }

    private val logger = LoggerFactory.getLogger(PersonalRoomActor::class.java)

    private lateinit var testProbe: ActorRef<PersonalRoomResponse>

    private lateinit var counselorRoomActor: ActorRef<CounselorRoomCommand>

    private var isRunTimer: Boolean = true

    private var socketSession: WebSocketSession? = null

    override fun createReceive(): Receive<PersonalRoomCommand> {
        return newReceiveBuilder()
            .onMessage(SetTestProbe::class.java, this::onSetTestProbe)
            .onMessage(SendMessage::class.java, this::onSendMessage)
            .onMessage(SendTextMessage::class.java, this::onSendTextMessage)
            .onMessage(AutoOnceProcess::class.java, this::onAutoOnceProcess)
            .onMessage(SetSocketSession::class.java, this::onSetSocketSession)
            .onMessage(ClearSocketSession::class.java, this::onClearSocketSession)
            .onMessage(SetCounselorRoom::class.java, this::onSetCounselorRoom)
            .onMessage(SendToCounselorRoomForCounseling::class.java, this::onSendToCounselorRoomForCounseling)
            .build()
    }

    private fun onSendToCounselorRoomForCounseling(sendToCounselorRoomForCounseling: SendToCounselorRoomForCounseling): Behavior<PersonalRoomCommand> {
        if(::counselorRoomActor.isInitialized){
            counselorRoomActor.tell(SendToCounselor(sendToCounselorRoomForCounseling.message))
        }
        else {
            socketSession?.sendMessage(TextMessage("상담방이 없습니다."))
        }

        return this
    }

    private fun onSetCounselorRoom(setCounselorRoom: SetCounselorRoom): Behavior<PersonalRoomCommand> {
        counselorRoomActor = setCounselorRoom.counselorRoomActor

        if(socketSession!=null){
            socketSession!!.sendMessage(TextMessage("상담방이 시작됨 ${counselorRoomActor.path()}"))
        }
        return this
    }

    private fun onSendTextMessage(sendTextMessage: SendTextMessage): Behavior<PersonalRoomCommand> {
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

    private fun onClearSocketSession(clearSocketSession: ClearSocketSession): Behavior<PersonalRoomCommand> {
        logger.info("ClearSocketSession received in PrivacyRoomActor $identifier")
        socketSession = null
        return this
    }

    private fun onSetSocketSession(command: SetSocketSession): Behavior<PersonalRoomCommand> {
        logger.info("OnSetSocketSession received in PrivacyRoomActor $identifier")
        socketSession = command.socketSession

        if(!isRunTimer){
            val randomStartDuration = Duration.ofSeconds(ThreadLocalRandom.current().nextLong(3, 6))
            timers.startTimerAtFixedRate(AutoOnceProcess, randomStartDuration, Duration.ofSeconds(5))
            isRunTimer = true
        }

        return this
    }

    private fun onSetTestProbe(command: SetTestProbe): Behavior<PersonalRoomCommand> {
        logger.info("OnSetTestProbe received in PrivacyRoomActor $identifier")
        testProbe = command.testProbe

        return this
    }

    private fun onAutoOnceProcess(command: AutoOnceProcess): Behavior<PersonalRoomCommand> {
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

    private fun onSendMessage(command: SendMessage): Behavior<PersonalRoomCommand> {
        logger.info("Message received in PrivacyRoomActor $identifier: ${command.message}")

        command.replyTo.tell(HelloResponse("Kotlin"))
        return this
    }
}