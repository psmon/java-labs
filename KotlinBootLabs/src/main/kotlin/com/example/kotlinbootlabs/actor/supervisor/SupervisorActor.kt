package com.example.kotlinbootlabs.actor.supervisor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

import com.example.kotlinbootlabs.actor.Hello
import com.example.kotlinbootlabs.actor.HelloActor
import com.example.kotlinbootlabs.actor.HelloActorCommand
import com.example.kotlinbootlabs.actor.HelloActorResponse
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.Terminated

/** SupervisorActor가 처리할 수 있는 명령들 */
sealed class SupervisorCommand

data class CreateChild(val name: String) : SupervisorCommand()
data class SendHello(
    val childName: String,
    val message: String,
    val replyTo: ActorRef<HelloActorResponse>
) : SupervisorCommand()
data class GetChildCount(val replyTo: ActorRef<Int>) : SupervisorCommand()
data class TerminateChild(val name: String) : SupervisorCommand()

class SupervisorActor private constructor(
    context: ActorContext<SupervisorCommand>
) : AbstractBehavior<SupervisorCommand>(context) {

    companion object {
        fun create(): Behavior<SupervisorCommand> {
            return Behaviors.setup { context -> SupervisorActor(context) }
        }
    }

    private val children = mutableMapOf<String, ActorRef<HelloActorCommand>>()

    override fun createReceive(): Receive<SupervisorCommand> {
        return newReceiveBuilder()
            .onMessage(CreateChild::class.java, this::onCreateChild)
            .onMessage(SendHello::class.java, this::onSendHello)
            .onMessage(GetChildCount::class.java, this::onGetChildCount)
            .onMessage(TerminateChild::class.java, this::onTerminateChild)
            .onSignal(Terminated::class.java, this::onChildTerminated)
            .build()
    }

    private fun onCreateChild(command: CreateChild): Behavior<SupervisorCommand> {
        val childName = command.name
        val childActor = context.spawn(
            Behaviors.supervise(HelloActor.create())
                .onFailure(SupervisorStrategy.restart()),
            childName
        )
        context.watch(childActor)

        children[childName] = childActor
        context.log.info("Created child actor with name: $childName")
        return this
    }

    private fun onSendHello(command: SendHello): Behavior<SupervisorCommand> {
        val child = children[command.childName]
        if (child != null) {
            child.tell(Hello(command.message, command.replyTo))
        } else {
            context.log.warn("Child actor [${command.childName}] does not exist.")
        }
        return this
    }

    private fun onGetChildCount(command: GetChildCount): Behavior<SupervisorCommand> {
        command.replyTo.tell(children.size)
        return this
    }

    private fun onChildTerminated(terminated: Terminated): Behavior<SupervisorCommand> {
        val childActor = terminated.ref
        val childName = childActor.path().name()
        children.remove(childName)
        context.log.info("Child actor terminated: $childName")
        return this
    }

    private fun onTerminateChild(command: TerminateChild): Behavior<SupervisorCommand> {
        val child = children[command.name]
        if (child != null) {
            context.stop(child)
            context.log.info("Terminated child actor: ${command.name}")
        } else {
            context.log.warn("Attempted to terminate non-existent child actor: ${command.name}")
        }
        return this
    }

}