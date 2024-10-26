package com.example.kotlinbootlabs.controller

import com.example.kotlinbootlabs.actor.MainStageActorCommand
import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.AskPattern
import akka.util.Timeout
import org.springframework.web.bind.annotation.*
import com.example.kotlinbootlabs.ws.actor.*
import akka.actor.typed.ActorSystem
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@RestController
@RequestMapping("/api/admin/channel")
class AdminChannelController(private val actorSystem: ActorSystem<MainStageActorCommand>,
                             private val supervisorChannelActor: ActorRef<SupervisorChannelCommand>) {

    private val timeout: Duration = Duration.ofSeconds(5)

    @PostMapping("/add-counselor-manager")
    fun addCounselorManager(@RequestParam channel: String): CompletionStage<String>? {
        return AskPattern.ask(
            supervisorChannelActor,
            { replyTo: ActorRef<SupervisorChannelResponse> -> CreateCounselorManager(channel, replyTo) },
            timeout,
            actorSystem.scheduler()
        ).thenApply { response ->
            when (response) {
                is CounselorManagerCreated -> "Counselor Manager for channel $channel created successfully."
                is SupervisorErrorStringResponse -> response.message
                else -> "Unknown error occurred."
            }
        }
    }

    @GetMapping("/list-counselor-managers")
    fun listCounselorManagers(): CompletionStage<List<String>>? {
        return AskPattern.ask(
            supervisorChannelActor,
            { replyTo: ActorRef<SupervisorChannelResponse> -> GetAllCounselorManagers(replyTo) },
            timeout,
            actorSystem.scheduler()
        ).thenApply { response ->
            when (response) {
                is AllCounselorManagers -> response.channels
                else -> emptyList()
            }
        }
    }
}