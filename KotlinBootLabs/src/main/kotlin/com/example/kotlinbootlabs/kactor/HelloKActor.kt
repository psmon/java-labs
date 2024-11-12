package com.example.kotlinbootlabs.kactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

sealed class HelloKActorCommand
data class Hello(val message: String, val replyTo: kotlinx.coroutines.CompletableDeferred<HelloKActorResponse>) : HelloKActorCommand()
data class GetHelloCount(val replyTo: kotlinx.coroutines.CompletableDeferred<HelloKActorResponse>) : HelloKActorCommand()

sealed class HelloKActorResponse
data class HelloResponse(val message: String) : HelloKActorResponse()
data class HelloCountResponse(val count: Int) : HelloKActorResponse()

class HelloKActor {
    private val channel = Channel<HelloKActorCommand>()
    private var helloCount = 0
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            for (command in channel) {
                when (command) {
                    is Hello -> handleHello(command)
                    is GetHelloCount -> handleGetHelloCount(command)
                }
            }
        }
    }

    private fun handleHello(command: Hello) {
        if (command.message == "Hello") {
            helloCount++
            command.replyTo.complete(HelloResponse("Kotlin"))
        } else if (command.message == "InvalidMessage") {
            throw RuntimeException("Invalid message received!")
        }
    }

    private fun handleGetHelloCount(command: GetHelloCount) {
        command.replyTo.complete(HelloCountResponse(helloCount))
    }

    suspend fun send(command: HelloKActorCommand) {
        channel.send(command)
    }

    fun stop() {
        scope.cancel()
    }
}