package com.example.kotlinbootlabs.actor.router
import com.example.kotlinbootlabs.actor.Hello
import com.example.kotlinbootlabs.actor.HelloActor
import com.example.kotlinbootlabs.actor.HelloActorCommand
import com.example.kotlinbootlabs.actor.HelloActorResponse
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.SupervisorStrategy
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import org.apache.pekko.actor.typed.javadsl.PoolRouter
import org.apache.pekko.actor.typed.javadsl.Routers

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