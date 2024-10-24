package com.example.kotlinbootlabs.controller

import com.example.kotlinbootlabs.actor.MainStageActorCommand
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.javadsl.AskPattern
import org.apache.pekko.util.Timeout
import org.springframework.web.bind.annotation.*
import com.example.kotlinbootlabs.ws.actor.*
import org.apache.pekko.actor.typed.ActorSystem
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@RestController
@RequestMapping("/api/admin/counselor")
class AdminCounselorController(private val actorSystem: ActorSystem<MainStageActorCommand>,
                               private val supervisorChannelActor: ActorRef<SupervisorChannelCommand>) {

    private val timeout: Duration = Duration.ofSeconds(5)

    @PostMapping("/add-counselor")
    fun addCounselor(@RequestParam channel: String, @RequestParam name: String): CompletionStage<String>? {
        return AskPattern.ask(
            supervisorChannelActor,
            { replyTo: ActorRef<SupervisorChannelResponse> -> GetCounselorManager(channel, replyTo) },
            timeout,
            actorSystem.scheduler()
        ).thenCompose { response ->
            when (response) {
                is CounselorManagerFound -> {
                    AskPattern.ask(
                        response.actorRef,
                        { replyTo: ActorRef<CounselorManagerResponse> -> CreateCounselor(name, replyTo) },
                        timeout,
                        actorSystem.scheduler()
                    ).thenApply { counselorResponse ->
                        when (counselorResponse) {
                            is CounselorCreated -> "Counselor $name created successfully."
                            else -> "Unknown error occurred."
                        }
                    }
                }
                is SupervisorErrorStringResponse -> CompletableFuture.completedFuture(response.message)
                else -> CompletableFuture.completedFuture("Unknown error occurred.")
            }
        }
    }
}