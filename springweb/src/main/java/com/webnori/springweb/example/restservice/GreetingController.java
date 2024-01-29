package com.webnori.springweb.example.restservice;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.webnori.springweb.example.akka.AkkaManager;
import com.webnori.springweb.example.akka.actors.cluster.TestClusterMessages;
import com.webnori.springweb.example.akka.models.FakeSlowMode;
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

//TEST : http://localhost:8080/swagger-ui/index.html

@RestController
@Tag(name = "greeting", description = "test API")
@RequestMapping("/api/greeting")
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    ActorSelection helloWorldActor;

    ActorSelection helloWordChildActor;

    ActorRef helloWordActor2;

    ActorRef roundRobbinActor;

    ActorRef clusterActor;

    ActorRef clusterManagerActor;


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

        // ActorRef
        helloWordActor2 = AkkaManager.getInstance().getGreetActor();

        // Router
        roundRobbinActor = AkkaManager.getInstance().getRouterActor();

        // ClusterActor
        clusterActor = AkkaManager.getInstance().getClusterActor();

        clusterManagerActor = AkkaManager.getInstance().getClusterManagerActor();

    }

    @Operation(summary = "greeting",
            description = "테스트 : 액터 주소/참조를 통해 이벤트를 전송")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Greeting.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")})
    @Parameters({@Parameter(name = "name", description = "name", example = "hello")})
    @ResponseBody
    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {

        // 액터는 참조자를통해 메시지 전송도 가능하지만 알려진 주소체계를 통해 전송도 가능합니다.
        // Remote/Cluster로 확장될수 있으며 위치투명성을 가질수 있어 분산처리환경에 유연합니다.
        // 로컬에서만 작성된 스레드객체는 일반적으로 리모트로 확장을포함 분산처리 확장이 어려울수 있습니다.

        String testMessage = String.format(template, name);

        helloWorldActor.tell(testMessage + " by ActorAddress", ActorRef.noSender());

        helloWordActor2.tell(testMessage + " by ActorRef", ActorRef.noSender());

        return new Greeting(counter.incrementAndGet(), testMessage);
    }

    @Operation(summary = "greetingWithBlock",
            description = "테스트 : 기다림이 필요없는 불락킹기능을 수행하지만~ API의 지연은 발생하지 않습니다. - ForgetAndFire")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Greeting.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")})
    @Parameters({@Parameter(name = "name", description = "name", example = "hello")})
    @ResponseBody
    @GetMapping("/greeting-block")
    public Greeting greetingWithBlock(@RequestParam(value = "name", defaultValue = "World") String name) {

        String testMessage = String.format(template, name);

        // Command를 통해 슬로우모드로 전환합니다.
        // 생성자를 통해 액터의 작동을 결정할수도 있지만, 커멘드를 통해 작동방식을 조정할수 있으며 유닛테스트때 유연할수 있습니다.
        // 생성자에의해 의존성이 복잡한 객체를 유닛테스트하는것은 어려울수 있습니다.

        helloWordActor2.tell(new FakeSlowMode(), ActorRef.noSender());

        helloWordActor2.tell(testMessage + " blcok test", ActorRef.noSender());

        return new Greeting(counter.incrementAndGet(), testMessage);
    }

    @Operation(summary = "greetingWithRouter",
            description = "테스트 : greetingWithBlock의 확장버전으로 블락킹이 존재하는 기능을 동시 실행할수있습니다. 멀티스레드 프로그래밍" +
                    "필요없이 Dispacher로 스레드활용 계획을 정의할수 있습니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Greeting.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")})
    @Parameters({
            @Parameter(name = "name", description = "name", example = "hello"),
            @Parameter(name = "testCount", description = "testCount", example = "1")
    })
    @ResponseBody
    @GetMapping("/greeting-router")
    public Greeting greetingWithRouter(
            @RequestParam(value = "name", defaultValue = "World") String name,
            @RequestParam(value = "testCount") int testCount)
    {

        String testMessage = String.format(template, name);

        for(int i=0;i<testCount;i++){
            roundRobbinActor.tell(new FakeSlowMode(), ActorRef.noSender());
        }

        for(int i=0;i<testCount;i++){
            roundRobbinActor.tell(testMessage + " blcok test", ActorRef.noSender());
        }

        return new Greeting(counter.incrementAndGet(), testMessage);
    }

    @Operation(summary = "greetingClusterRouterActor",
            description = "테스트 : 분산어플리케이션에 RoundRobin")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Greeting.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")})
    @Parameters({
            @Parameter(name = "name", description = "name", example = "hello"),
            @Parameter(name = "testCount", description = "testCount", example = "1")
    })
    @ResponseBody
    @GetMapping("/greeting-cluster-router")
    public Greeting greetingClusterRouterActor(
            @RequestParam(value = "name", defaultValue = "World") String name,
            @RequestParam(value = "testCount") int testCount)
    {

        String testMessage = String.format(template, name);

        for(int i=0;i<testCount;i++){
            clusterActor.tell(testMessage + i , ActorRef.noSender());
        }

        return new Greeting(counter.incrementAndGet(), testMessage);
    }

    @Operation(summary = "greetingClusterManagerActor",
            description = "테스트 : 특정 Role에게만 전송")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = Greeting.class))),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"),
            @ApiResponse(responseCode = "404", description = "NOT FOUND"),
            @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")})
    @Parameters({
            @Parameter(name = "testCount", description = "testCount", example = "1")
    })
    @ResponseBody
    @GetMapping("/greeting-cluster-manager")
    public Greeting greetingClusterManagerActor(
            @RequestParam(value = "testCount") int testCount)
    {

        String testMessage = String.format(template, "test");

        for(int i=0;i<testCount;i++){
            clusterManagerActor.tell( TestClusterMessages.ping() , ActorRef.noSender());
        }

        return new Greeting(counter.incrementAndGet(), testMessage);
    }

}