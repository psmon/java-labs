package actor.router
import actor.Hello
import actor.HelloActor
import actor.HelloActorCommand
import actor.HelloActorResponse
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.PoolRouter
import akka.actor.typed.javadsl.Routers

sealed class HelloManagerCommand
data class DistributedHelloMessage(val message: String, val replyTo: ActorRef<HelloActorResponse>) : HelloManagerCommand()

class HelloManagerActor private constructor(
    context: ActorContext<HelloManagerCommand>,
    private var router: PoolRouter<HelloActorCommand>,
    private val routerRef: ActorRef<HelloActorCommand>
) : AbstractBehavior<HelloManagerCommand>(context) {

    companion object {
        fun create(): Behavior<HelloManagerCommand> {
            return Behaviors.setup { context ->

                val router = Routers.pool(5, Behaviors.supervise(HelloActor.create())
                    .onFailure(SupervisorStrategy.restart()))
                    .withRoundRobinRouting()

                val routerRef = context.spawn(router, "hello-actor-pool")

                HelloManagerActor(context, router, routerRef)
            }
        }
    }

    override fun createReceive(): Receive<HelloManagerCommand> {
        return newReceiveBuilder()
            .onMessage(DistributedHelloMessage::class.java, this::onSendHelloMessage)
            .build()
    }

    private fun onSendHelloMessage(command: DistributedHelloMessage): Behavior<HelloManagerCommand> {
        routerRef.tell(Hello(command.message, command.replyTo))
        return this
    }
}