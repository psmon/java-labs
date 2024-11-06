package com.example.kotlinbootlabs.actor.eventbus

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.pubsub.Topic
import akka.stream.typed.javadsl.PubSub

sealed class PubSubCommand
data class PublishMessage(val channel: String, val message: String) : PubSubCommand()
data class Subscribe(val channel: String, val subscriber: ActorRef<String>) : PubSubCommand()


class PubSubActor(context: ActorContext<PubSubCommand>) : AbstractBehavior<PubSubCommand>(context) {

    companion object {
        fun create(): Behavior<PubSubCommand> {
            return Behaviors.setup { context -> PubSubActor(context) }
        }
    }

    private val topics = mutableMapOf<String, ActorRef<Topic.Command<String>>>()

    override fun createReceive(): Receive<PubSubCommand> {
        return newReceiveBuilder()
            .onMessage(PublishMessage::class.java, this::onPublishMessage)
            .onMessage(Subscribe::class.java, this::onSubscribe)
            .build()
    }

    private fun onPublishMessage(command: PublishMessage): Behavior<PubSubCommand> {
        val topic = topics.getOrPut(command.channel) {
            context.spawn(Topic.create(String::class.java, command.channel), command.channel)
        }
        topic.tell(Topic.publish(command.message))
        return this
    }

    private fun onSubscribe(command: Subscribe): Behavior<PubSubCommand> {
        val topic = topics.getOrPut(command.channel) {
            context.spawn(Topic.create(String::class.java, command.channel), command.channel)
        }
        topic.tell(Topic.subscribe(command.subscriber))
        return this
    }
}