package actor.hellostate

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

import akka.stream.javadsl.Source
import akka.stream.javadsl.Sink
import akka.stream.OverflowStrategy
import akka.stream.Materializer
import java.time.Duration

import akka.actor.typed.javadsl.TimerScheduler
import java.io.Console

/** HelloStateActor 처리할 수 있는 명령들 */
sealed class HelloStateActorCommand
data class Hello(val message: String, val replyTo: ActorRef<Any>) : HelloStateActorCommand()
data class GetHelloCount(val replyTo: ActorRef<Any>) : HelloStateActorCommand()
data class GetHelloTotalCount(val replyTo: ActorRef<Any>) : HelloStateActorCommand()
data class ChangeState(val newState: State) : HelloStateActorCommand()
data class HelloLimit(val message: String, val replyTo: ActorRef<Any>) : HelloStateActorCommand()
object ResetHelloCount : HelloStateActorCommand()

/** HelloStateActor 반환할 수 있는 응답들 */
sealed class HelloStateActorResponse
data class HelloResponse(val message: String) : HelloStateActorResponse()
data class HelloCountResponse(val count: Int) : HelloStateActorResponse()

/** 상태 정의 */
enum class State {
    HAPPY, ANGRY
}

/** HelloStateActor 클래스 */
class HelloStateActor private constructor(
    private val context: ActorContext<HelloStateActorCommand>,
    private val timers: TimerScheduler<HelloStateActorCommand>,
    private var state: State
) : AbstractBehavior<HelloStateActorCommand>(context) {

    companion object {
        fun create(initialState: State): Behavior<HelloStateActorCommand> {
            return Behaviors.withTimers { timers ->
                Behaviors.setup { context -> HelloStateActor(context, timers, initialState) }
            }
        }
    }

    init {
        timers.startTimerAtFixedRate(ResetHelloCount, Duration.ofSeconds(10))
    }

    override fun createReceive(): Receive<HelloStateActorCommand> {
        return newReceiveBuilder()
            .onMessage(Hello::class.java, this::onHello)
            .onMessage(HelloLimit::class.java, this::onHelloLimit)
            .onMessage(GetHelloCount::class.java, this::onGetHelloCount)
            .onMessage(GetHelloTotalCount::class.java, this::onGetHelloTotalCount)
            .onMessage(ChangeState::class.java, this::onChangeState)
            .onMessage(ResetHelloCount::class.java, this::onResetHelloCount)
            .build()
    }

    private var helloCount: Int = 0

    private var helloTotalCount: Int = 0

    private val materializer = Materializer.createMaterializer(context.system)

    private val helloLimitSource = Source.queue<HelloLimit>(100, OverflowStrategy.backpressure())
        .throttle(3, Duration.ofSeconds(1))
        .to(Sink.foreach { cmd ->
            when (state) {
                State.HAPPY -> {
                    if (cmd.message == "Hello") {
                        helloCount++
                        helloTotalCount++
                        cmd.replyTo.tell(HelloResponse("Kotlin"))
                    }
                }
                State.ANGRY -> {
                    cmd.replyTo.tell(HelloResponse("Don't talk to me!"))
                }
            }
        })
        .run(materializer)

    private fun onHello(command: Hello): Behavior<HelloStateActorCommand> {
        when (state) {
            State.HAPPY -> {
                if (command.message == "Hello") {
                    helloCount++
                    helloTotalCount++
                    command.replyTo.tell(HelloResponse("Kotlin"))
                    println("onHello-Kotlin")
                }
            }
            State.ANGRY -> {
                command.replyTo.tell(HelloResponse("Don't talk to me!"))
            }
        }
        return this
    }

    private fun onHelloLimit(command: HelloLimit): Behavior<HelloStateActorCommand> {
        helloLimitSource.offer(command)
        return this
    }

    private fun onGetHelloCount(command: GetHelloCount): Behavior<HelloStateActorCommand> {
        command.replyTo.tell(HelloCountResponse(helloCount))
        println("onGetHelloCount-helloCount: $helloCount")
        return this
    }

    private fun onGetHelloTotalCount(command: GetHelloTotalCount): Behavior<HelloStateActorCommand> {
        command.replyTo.tell(HelloCountResponse(helloTotalCount))
        return this
    }

    private fun onChangeState(command: ChangeState): Behavior<HelloStateActorCommand> {
        state = command.newState
        return this
    }

    private fun onResetHelloCount(command: ResetHelloCount): Behavior<HelloStateActorCommand> {
        println("Resetting hello count")
        helloCount = 0
        return this
    }
}