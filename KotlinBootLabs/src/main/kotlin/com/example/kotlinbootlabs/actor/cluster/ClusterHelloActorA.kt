package com.example.kotlinbootlabs.actor.cluster


import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import com.example.kotlinbootlabs.actor.PersitenceSerializable

/** HelloActor 처리할 수 있는 명령들 */
sealed class HelloActorACommand : PersitenceSerializable
data class HelloA(val message: String, val replyTo: ActorRef<HelloActorAResponse>) : HelloActorACommand()
data class GetHelloCountA(val replyTo: ActorRef<HelloActorBResponse>) : HelloActorACommand()

/** HelloActor 반환할 수 있는 응답들 */
sealed class HelloActorAResponse : PersitenceSerializable
data class HelloAResponse(val message: String) : HelloActorAResponse()
data class HelloCountAResponse(val count: Int) : HelloActorAResponse()


/** HelloActor 클래스 */
class ClusterHelloActorA private constructor(
    private val context: ActorContext<HelloActorACommand>,
) : AbstractBehavior<HelloActorACommand>(context) {

    companion object {

        fun create(): Behavior<HelloActorACommand> {
            return Behaviors.setup { context -> ClusterHelloActorA(context) }
        }
    }

    init {
        var mediator = DistributedPubSub.get(context.system).mediator()
        mediator.tell(DistributedPubSubMediator.Subscribe("roleA", Adapter.toClassic(context.self)),null)
    }

    override fun createReceive(): Receive<HelloActorACommand> {
        return newReceiveBuilder()
            .onMessage(HelloA::class.java, this::onHello)
            .onMessage(GetHelloCountA::class.java, this::onGetHelloCount)
            .build()
    }

    private var helloCount: Int = 0

    private fun onHello(command: HelloA): Behavior<HelloActorACommand> {
        if (command.message == "Hello") {
            helloCount++
            context.log.info("[${context.self.path()}] Received valid Hello message. Count incremented to $helloCount")
            command.replyTo.tell(HelloAResponse("Kotlin"))
        }
        else if (command.message == "InvalidMessage") {
            throw RuntimeException("Invalid message received!")
        }

        return this
    }

    private fun onGetHelloCount(command: GetHelloCountA): Behavior<HelloActorACommand> {
        command.replyTo.tell(HelloCountBResponse(helloCount))
        return this
    }
}