package com.example.kotlinbootlabs.actor


import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

/** HelloActor 처리할 수 있는 명령들 */
sealed class HelloActorCommand
data class Hello(val message: String, val replyTo: ActorRef<HelloActorResponse>) : HelloActorCommand()
data class GetHelloCount(val replyTo: ActorRef<HelloActorResponse>) : HelloActorCommand()
/** HelloActor 반환할 수 있는 응답들 */
sealed class HelloActorResponse
data class HelloResponse(val message: String) : HelloActorResponse()
data class HelloCountResponse(val count: Int) : HelloActorResponse()


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
            .onMessage(GetHelloCount::class.java, this::onGetHelloCount)
            .build()
    }

    private var helloCount: Int = 0

    private fun onHello(command: Hello): Behavior<HelloActorCommand> {
        if (command.message == "Hello") {
            helloCount++
            context.log.info("[${context.self.path()}] Received valid Hello message. Count incremented to $helloCount")
            command.replyTo.tell(HelloResponse("Kotlin"))
        }
        else if (command.message == "InvalidMessage") {
            throw RuntimeException("Invalid message received!")
        }

        return this
    }

    private fun onGetHelloCount(command: GetHelloCount): Behavior<HelloActorCommand> {
        command.replyTo.tell(HelloCountResponse(helloCount))
        return this
    }
}