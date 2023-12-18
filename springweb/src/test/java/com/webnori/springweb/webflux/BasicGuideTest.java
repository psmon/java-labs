package com.webnori.springweb.webflux;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.stream.BackPressureTest;
import com.webnori.springweb.webflux.actor.SubScribeActor;
import com.webnori.springweb.webflux.actor.model.ConfirmEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class BasicGuideTest {

    private static final Logger logger = LoggerFactory.getLogger(BackPressureTest.class);

    private static ActorSystem actorSystem;
    private static final String hello = "not another hello world";

    private static ActorRef subScribeActor;

    private static ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]", role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);
        return serverSystem;
    }

    @BeforeClass
    public static void setup() {
        // Seed
        actorSystem = serverStart("ClusterSystem", "test", "seed");
        logger.info("========= sever loaded =========");
    }


    @Test
    @DisplayName("justMonoTest")
    public void justMonoTest() {

        new TestKit(actorSystem) {
            {
                // Test장치 셋업
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem)
                        .withDispatcher("my-dispatcher-streamtest");

                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);
                final TestKit probe = new TestKit(actorSystem);

                subScribeActor = actorSystem.actorOf(SubScribeActor.Props(), "SubScribeActor");
                subScribeActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // Test Rule
                int testTimeLimitSecond = 5;
                int testCount = 10000;

                Flux<Integer> numbersFlux = Flux.range(1, testCount);

                // 숫자를 ConfirmEvent 객체로 변환하고 수신액트에 전송
                numbersFlux.map(num -> new ConfirmEvent(num.toString()))
                    .subscribe(event -> {
                        subScribeActor.tell(event, ActorRef.noSender());
                });

                StepVerifier.create(numbersFlux)
                        .expectNextCount(testCount)
                        .verifyComplete(); // 스트림이 정상적으로 완료되는지 확인;

                //수신검증
                for(int i=0; i<testCount ; i++){
                    probe.expectMsgClass(ConfirmEvent.class);
                }

                within(Duration.ofSeconds(testTimeLimitSecond), () ->
                {
                    expectNoMessage(Duration.ofSeconds(testTimeLimitSecond));
                    return null;
                });
            }
        };
    }

    @Test
    @DisplayName("streamWithWebfluxTest")
    public void streamWithWebfluxTest() {
        new TestKit(actorSystem) {
            {
                // Test장치 셋업
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem)
                        .withDispatcher("my-dispatcher-streamtest");

                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);
                final TestKit probe = new TestKit(actorSystem);

                subScribeActor = actorSystem.actorOf(SubScribeActor.Props(), "SubScribeActor");
                subScribeActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // Test Rule
                int testTimeLimitSecond = 5;
                int testCount = 10000;
                int bufferSize = 100000;
                int processCouuntPerSec = 500;

                // Source 생성
                Source<Integer, NotUsed> source = Source.range(1, testCount);

                // Single Sync Flow
                Flow<Integer, ConfirmEvent, NotUsed> convertFlow = Flow.fromFunction(BasicGuideTest::integerToConfirmEvent);

                Flow<Integer, Integer, NotUsed> backpressureFlow = Flow.<Integer>create()
                        .buffer(bufferSize, OverflowStrategy.backpressure());

                // Sink 정의
                Sink<ConfirmEvent, CompletionStage<Done>> sink = Sink.foreach(s -> {
                    //처리완료
                });


                // RunnableGraph 생성 및 실행
                /*
                source.via(backpressureFlow)
                        .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS), processCouuntPerSec,
                                (ThrottleMode) ThrottleMode.shaping())
                        .via(convertFlow)
                        .to(sink)
                        .run(materializer);*/

                CompletionStage<Done> completionStage = source.via(backpressureFlow)
                        .throttle(processCouuntPerSec, Duration.ofSeconds(1))
                        .via(convertFlow)
                        .toMat(sink, Keep.right())
                        .run(materializer);

                Mono<Done> mono = Mono.fromCompletionStage(completionStage);


                Flux<Integer> numbersFlux = Flux.range(1, testCount);

                // 숫자를 ConfirmEvent 객체로 변환하고 수신액트에 전송
                numbersFlux.map(num -> new ConfirmEvent(num.toString()))
                        .subscribe(event -> {
                            subScribeActor.tell(event, ActorRef.noSender());
                        });

                StepVerifier.create(mono)
                        .expectNextCount(testCount)
                        .verifyComplete(); // 스트림이 정상적으로 완료되는지 확인;

                //수신검증
                for(int i=0; i<testCount ; i++){
                    probe.expectMsgClass(ConfirmEvent.class);
                }

                within(Duration.ofSeconds(testTimeLimitSecond), () ->
                {
                    expectNoMessage(Duration.ofSeconds(testTimeLimitSecond));
                    return null;
                });
            }
        };
    }

    private static ConfirmEvent integerToConfirmEvent(Integer param) {
        return new ConfirmEvent(param.toString());
    }

}
