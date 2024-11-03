package com.example.kotlinbootlabs.module

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import java.time.Duration
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

object AkkaUtils {
    suspend fun <T, R> askActor(
        actor: ActorRef<T>,
        message: (ActorRef<R>) -> T,
        timeout: Duration,
        actorSystem: ActorSystem<*>
    ): R {
        return AskPattern.ask(
            actor,
            message,
            timeout,
            actorSystem.scheduler()
        ).await()
    }

    fun <T, R> runBlockingAsk(
        actor: ActorRef<T>,
        message: (ActorRef<R>) -> T,
        timeout: Duration,
        actorSystem: ActorSystem<*>
    ): R = runBlocking {
        askActor(actor, message, timeout, actorSystem)
    }
}