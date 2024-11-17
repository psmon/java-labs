package com.example.kotlinbootlabs.kactor

import com.example.kotlinbootlabs.kafka.getStateStoreWithRetries
import com.example.kotlinbootlabs.service.RedisService
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
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
    @JsonProperty("state") var state: HelloKState,
    @JsonProperty("helloCount") var helloCount: Long,
    @JsonProperty("helloTotalCount") var helloTotalCount: Long
)

class HelloKTableActor(
        private val persistenceId:String ,
        private val producer: KafkaProducer<String, HelloKTableState>,
        private val redisService: RedisService
    ) {

    private val channel = Channel<HelloKTableActorCommand>()

    private val scope = CoroutineScope(Dispatchers.Default)

    private val objectMapper = ObjectMapper()

    private var curState: HelloKTableState

    init {

        // Read initial state from Redis
        curState = redisService.getValue("hello-state-store", persistenceId)
            .map { stateJson ->
                // Deserialize stateJson to HelloKTableState
                // Assuming you have a method to deserialize JSON to HelloKTableState
                stateJson?.let { deserializeState(it) }
            }
            .block() ?: HelloKTableState(HelloKState.HAPPY, 0, 0) // Default state if not found

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

            curState = newState

            // Save state to Redis
            redisService.setValue("hello-state-store", persistenceId, serializeState(curState)).subscribe()

            // Update KTable with new state
            //stateStore.put(persistenceId, newState)
            producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", persistenceId, curState))

            command.replyTo.complete(HelloKStateResponse("Kotlin"))

        } else if (curState.state == HelloKState.ANGRY) {
            command.replyTo.complete(HelloKStateResponse("Don't talk to me!"))
        }
    }

    private fun handleGetHelloCount(command: GetHelloKtableCount) {
        command.replyTo.complete(HelloKStateCountResponse(curState.helloCount))
    }

    private fun handleChangeState(command: ChangeStateKtable) {

        curState.state = command.state.state

        // Save state to Redis
        redisService.setValue("hello-state-store", persistenceId, serializeState(curState)).subscribe()

        // Update KTable with new state
        //stateStore.put(persistenceId, newState.state)
        producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", persistenceId, curState))
    }

    private fun handleResetHelloCount() {

        val newState = curState.copy(helloCount = 0)
        curState = newState

        // Save state to Redis
        redisService.setValue("hello-state-store", persistenceId, serializeState(curState)).subscribe()

        // Update KTable with new state
        //stateStore.put(persistenceId, newState)
        producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", persistenceId, curState))
    }

    suspend fun send(command: HelloKTableActorCommand) {
        channel.send(command)
    }

    fun stop() {
        scope.cancel()
    }

    private fun serializeState(state: HelloKTableState): String {
        return objectMapper.writeValueAsString(state)
    }

    private fun deserializeState(stateJson: String): HelloKTableState {
        return objectMapper.readValue(stateJson, HelloKTableState::class.java)
    }
}