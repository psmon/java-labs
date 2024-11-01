package com.example.kotlinbootlabs.actor.cluster


import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/** HelloActor 처리할 수 있는 명령들 */
sealed class HelloActorBCommand
data class HelloB(val message: String, val replyTo: ActorRef<HelloActorBResponse>) : HelloActorBCommand()
data class GetHelloCountB(val replyTo: ActorRef<HelloActorBResponse>) : HelloActorBCommand()
/** HelloActor 반환할 수 있는 응답들 */
sealed class HelloActorBResponse
data class HelloBResponse(val message: String) : HelloActorBResponse()
data class HelloCountBResponse(val count: Int) : HelloActorBResponse()


/** HelloActor 클래스 */
class ClusterHelloActorB private constructor(
    private val context: ActorContext<HelloActorBCommand>,
) : AbstractBehavior<HelloActorBCommand>(context) {

    companion object {
        fun create(): Behavior<HelloActorBCommand> {
            return Behaviors.setup { context -> ClusterHelloActorB(context) }
        }
    }

    override fun createReceive(): Receive<HelloActorBCommand> {
        return newReceiveBuilder()
            .onMessage(HelloB::class.java, this::onHello)
            .onMessage(GetHelloCountB::class.java, this::onGetHelloCount)
            .build()
    }

    private var helloCount: Int = 0

    private fun onHello(command: HelloB): Behavior<HelloActorBCommand> {
        if (command.message == "Hello") {
            helloCount++
            context.log.info("[${context.self.path()}] Received valid Hello message. Count incremented to $helloCount")
            command.replyTo.tell(HelloBResponse("Kotlin"))
        }
        else if (command.message == "InvalidMessage") {
            throw RuntimeException("Invalid message received!")
        }

        return this
    }

    private fun onGetHelloCount(command: GetHelloCountB): Behavior<HelloActorBCommand> {
        command.replyTo.tell(HelloCountBResponse(helloCount))
        return this
    }
}