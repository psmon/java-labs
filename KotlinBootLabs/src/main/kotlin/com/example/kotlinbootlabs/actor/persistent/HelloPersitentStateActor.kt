package com.example.kotlinbootlabs.actor.persistent


import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.state.javadsl.CommandHandler
import org.apache.pekko.persistence.typed.state.javadsl.DurableStateBehavior
import org.apache.pekko.persistence.typed.state.javadsl.Effect

sealed class HelloPersistentStateActorCommand
data class Hello(val message: String, val replyTo: ActorRef<Any>) : HelloPersistentStateActorCommand()
data class GetHelloCount(val replyTo: ActorRef<Any>) : HelloPersistentStateActorCommand()
data class GetHelloTotalCount(val replyTo: ActorRef<Any>) : HelloPersistentStateActorCommand()
data class ChangeState(val newState: State) : HelloPersistentStateActorCommand()
data class HelloLimit(val message: String, val replyTo: ActorRef<Any>) : HelloPersistentStateActorCommand()
object ResetHelloCount : HelloPersistentStateActorCommand()
object StopResetTimer : HelloPersistentStateActorCommand()

sealed class HelloPersistentStateActorResponse
data class HelloResponse(val message: String) : HelloPersistentStateActorResponse()
data class HelloCountResponse(val count: Int) : HelloPersistentStateActorResponse()


class LongToIntDeserializer : JsonDeserializer<Int>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Int {
        return p.longValue.toInt()
    }
}

enum class State {
    HAPPY,
    ANGRY
}

@JsonSerialize
data class HelloState (
    val state: State,
    @field:JsonDeserialize(using = LongToIntDeserializer::class)
    val helloCount: Int,
    @field:JsonDeserialize(using = LongToIntDeserializer::class)
    val helloTotalCount: Int
)

class HelloPersistentStateActor private constructor(
    private val context: ActorContext<HelloPersistentStateActorCommand>,
    private val persistenceId: PersistenceId
) : DurableStateBehavior<HelloPersistentStateActorCommand, HelloState>(persistenceId) {

    companion object {
        fun create(persistenceId: PersistenceId): Behavior<HelloPersistentStateActorCommand> {
            return Behaviors.setup { context -> HelloPersistentStateActor(context, persistenceId) }
        }
    }

    override fun tag(): String {
        return "tag1"
    }

    override fun emptyState(): HelloState = HelloState(State.HAPPY, 0, 0)

    override fun commandHandler(): CommandHandler<HelloPersistentStateActorCommand, HelloState> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Hello::class.java) { state, command -> onHello(state, command) }
            .onCommand(GetHelloCount::class.java) { state, command -> onGetHelloCount(state, command) }
            .onCommand(GetHelloTotalCount::class.java) { state, command -> onGetHelloTotalCount(state, command) }
            .onCommand(ChangeState::class.java) { state, command -> onChangeState(state, command) }
            .onCommand(ResetHelloCount::class.java) { state, _ -> onResetHelloCount(state) }
            .build()
    }

    private fun onHello(state: HelloState, command: Hello): Effect<HelloState> {
        return when (state.state) {
            State.HAPPY -> {
                if (command.message == "Hello") {
                    val newState = state.copy(
                        helloCount = state.helloCount + 1,
                        helloTotalCount = state.helloTotalCount + 1
                    )
                    Effect().persist(newState).thenRun {
                        command.replyTo.tell(HelloResponse("Kotlin"))
                        context.log.info("onHello-Kotlin")
                    }
                } else {
                    Effect().none()
                }
            }
            State.ANGRY -> {
                command.replyTo.tell(HelloResponse("Don't talk to me!"))
                Effect().none()
            }
        }
    }

    private fun onGetHelloCount(state: HelloState, command: GetHelloCount): Effect<HelloState> {
        command.replyTo.tell(HelloCountResponse(state.helloCount))
        context.log.info("onGetHelloCount-helloCount: ${state.helloCount}")
        return Effect().none()
    }

    private fun onGetHelloTotalCount(state: HelloState, command: GetHelloTotalCount): Effect<HelloState> {
        command.replyTo.tell(HelloCountResponse(state.helloTotalCount))
        return Effect().none()
    }

    private fun onChangeState(state: HelloState, command: ChangeState): Effect<HelloState> {
        val newState = state.copy(state = command.newState)
        return Effect().persist(newState)
    }

    private fun onResetHelloCount(state: HelloState): Effect<HelloState> {
        context.log.info("Resetting hello count")
        val newState = state.copy(helloCount = 0)
        return Effect().persist(newState)
    }
}