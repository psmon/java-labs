package actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/** HelloActor 처리할 수 있는 명령들 */
sealed class HelloActorCommand
data class Hello(val message: String, val replyTo: ActorRef<Any>) : HelloActorCommand()

/** HelloActor 반환할 수 있는 응답들 */
sealed class HelloActorResponse
data class HelloResponse(val message: String) : HelloActorResponse()


/** HelloActor 클래스 */
class HelloActor private constructor(
    private val context: ActorContext<HelloActorCommand>,
) : AbstractBehavior<HelloActorCommand>(context) {

    companion object {
        fun create(): Behavior<HelloActorCommand> {
            return Behaviors.setup { context -> HelloActor(context) }
        }
    }

    override fun createReceive(): Receive<HelloActorCommand> {
        return newReceiveBuilder()
            .onMessage(Hello::class.java, this::onHello)
            .build()
    }

    private fun onHello(command: Hello): Behavior<HelloActorCommand> {

        if (command.message == "Hello") {
            command.replyTo.tell(HelloResponse("Kotlin"))
        }
        return this
    }
}