package com.example.kotlinbootlabs.kactor

import com.example.kotlinbootlabs.kafka.getStateStoreWithRetries
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore


sealed class HelloKTableActorCommand
data class HelloKtable(val message: String, val replyTo: kotlinx.coroutines.CompletableDeferred<HelloKTableActorResponse>) : HelloKTableActorCommand()
data class GetHelloKtableCount(val replyTo: kotlinx.coroutines.CompletableDeferred<HelloKTableActorResponse>) : HelloKTableActorCommand()

data class ChangeStateKtable(val state: HelloKTableState) : HelloKTableActorCommand()
object ResetKTableHelloCount : HelloKTableActorCommand()

sealed class HelloKTableActorResponse
data class HelloKStateResponse(val message: String) : HelloKTableActorResponse()
data class HelloKStateCountResponse(val count: Long) : HelloKTableActorResponse()

enum class HelloKState {
    HAPPY,
    ANGRY
}

data class HelloKTableState @JsonCreator constructor(
    @JsonProperty("state") val state: HelloKState,
    @JsonProperty("helloCount") val helloCount: Long,
    @JsonProperty("helloTotalCount") val helloTotalCount: Long
)

class HelloKTableActor(
        private val persistenceId:String ,
        private val streams: KafkaStreams,
        private var inItState: HelloKTableState,
        private val producer: KafkaProducer<String, HelloKTableState>
    ) {
    private val channel = Channel<HelloKTableActorCommand>()
    private val scope = CoroutineScope(Dispatchers.Default)

    val readStateStore: ReadOnlyKeyValueStore<String, HelloKTableState> =
        getStateStoreWithRetries(streams, "hello-state-store")

    private val curState: HelloKTableState

    init {

        curState = inItState

        scope.launch {
            for (command in channel) {
                when (command) {
                    is HelloKtable -> handleHello(command)
                    is GetHelloKtableCount -> handleGetHelloCount(command)
                    is ChangeStateKtable -> handleChangeState(command)
                    is ResetKTableHelloCount -> handleResetHelloCount()
                }
            }
        }
    }

    private fun handleHello(command: HelloKtable) {
        if (curState.state == HelloKState.HAPPY && command.message == "Hello") {
            val newState = curState.copy(helloCount = curState.helloCount + 1, helloTotalCount = curState.helloTotalCount + 1)
            // Update KTable with new state
            //stateStore.put(persistenceId, newState)
            producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", persistenceId, newState))

            command.replyTo.complete(HelloKStateResponse("Kotlin"))

        } else if (curState.state == HelloKState.ANGRY) {
            command.replyTo.complete(HelloKStateResponse("Don't talk to me!"))
        }
    }

    private fun handleGetHelloCount(command: GetHelloKtableCount) {
        command.replyTo.complete(HelloKStateCountResponse(curState.helloCount))
    }

    private fun handleChangeState(command: ChangeStateKtable) {
        val newState = command.copy(state = command.state)
        // Update KTable with new state
        //stateStore.put(persistenceId, newState.state)
        producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", persistenceId, newState.state))
    }

    private fun handleResetHelloCount() {

        val newState = curState.copy(helloCount = 0)
        // Update KTable with new state
        //stateStore.put(persistenceId, newState)
        producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", persistenceId, newState))
    }

    suspend fun send(command: HelloKTableActorCommand) {
        channel.send(command)
    }

    fun stop() {
        scope.cancel()
    }
}