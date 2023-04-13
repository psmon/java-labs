package com.webnori.springweb.example.restservice;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.webnori.springweb.example.akka.AkkaManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicLong;

//TEST : http://localhost:8099/swagger-ui/index.html

@RestController
@Tag(name = "greeting", description = "test API")
@RequestMapping("/api/greeting")
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    ActorSelection helloWorldActor;

    ActorSelection helloWordChildActor;

    ActorRef helloWordActor2;


    GreetingController() {

        ActorSystem actorSystem = AkkaManager.getInstance().getActorSystem();

        // Two Way, Get Actor Ref~
        // By Actor Address
        // in create AkkaManager
        //
        // AkkSystem
        //   -user
        //    -HelloWorld
        //    -TimerActor
        //      -helloActor
        helloWorldActor = actorSystem.actorSelection("/user/HelloWorld");

        helloWordChildActor = actorSystem.actorSelection("/user/TimerActor/helloActor");

        // By ActorRef
        helloWordActor2 = AkkaManager.getInstance().getGreetActor();

    }

    @Operation(summary = "Hello, Worold", description = "인사하기")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content =
            @Content(schema = @Schema(implementation = Greeting.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")
    })
    @Parameters({
            @Parameter(name = "name", description = "이름", example = "헬로우")
    })
    @ResponseBody
    @GetMapping("/hello")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {

        String testMessage = String.format(template, name);

        helloWorldActor.tell(testMessage + " by ActorAddress", ActorRef.noSender());

        helloWordActor2.tell(testMessage + " by ActorAddress", ActorRef.noSender());

        return new Greeting(counter.incrementAndGet(), testMessage);
    }
}