package com.webnori.springweb.webflux;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.AsPublisher;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.webflux.actor.SubScribeActor;
import com.webnori.springweb.webflux.actor.model.ConfirmEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * TestClass : BasicGuideTest
 * 목표 : ReactiveStream의 구현체인 AkkaStream과 Webflux를 Stream으로 연결해봅니다.
 * 참고 링크 : TODO
 */
public class BasicGuideTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicGuideTest.class);
    private static ActorSystem actorSystem;
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

    private static ConfirmEvent integerToConfirmEvent(Integer param) {
        return new ConfirmEvent(param.toString());
    }

    private static CompletionStage<ConfirmEvent> integerToConfirmEventAsync(Integer param) {
        return CompletableFuture.supplyAsync(() -> {
            // Todo Something...
            return new ConfirmEvent(param.toString());
        });
    }

    @Test
    @DisplayName("fluxConfirmTestWithTPS")
    public void fluxConfirmTestWithTPS() {

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
                for (int i = 0; i < testCount; i++) {
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
    @DisplayName("akkaStreamToFluxConfirmTestWithTps")
    public void akkaStreamToFluxConfirmTestWithTps() {
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
                int testTimeLimitSecond = 10;
                int testCount = 1000;
                int backpresureBufferSize = 100000;
                int processCouuntPerSec = 200;

                // Source 생성
                Source<Integer, NotUsed> source = Source.range(1, testCount);

                // ConvertFlow
                final int parallelism = 1;
                Flow<Integer, ConfirmEvent, NotUsed> convertFlow = Flow.<Integer>create()
                        .mapAsync(parallelism, BasicGuideTest::integerToConfirmEventAsync);

                Flow<Integer, Integer, NotUsed> backpressureFlow = Flow.<Integer>create()
                        .buffer(backpresureBufferSize, OverflowStrategy.backpressure());

                // Pub,Sub이 동시 시작되면 유실발생할수 있기때문에, Pub 시작시간이 1초지연~
                Flow<Integer, Integer, NotUsed> initialDelayFlow = Flow.<Integer>create()
                        .initialDelay(Duration.ofSeconds(1));

                // ReactiveStream의 인터페이스를 준수하면
                // 서로 다른목적으로 구현되어도(AkkaStream,WebFlux)
                // Stream간 상호연결이 가능합니다.  ( publisher -> subscribe )
                Publisher<ConfirmEvent> publisher = source
                        .via(initialDelayFlow)
                        .via(backpressureFlow)
                        .throttle(processCouuntPerSec, Duration.ofSeconds(1))
                        .via(convertFlow)
                        .runWith(Sink.asPublisher(AsPublisher.WITH_FANOUT), materializer);

                Flux<ConfirmEvent> flux = Flux.from(publisher);

                flux.subscribe(event -> subScribeActor.tell(event, ActorRef.noSender()));

                // 수신검증 by StepVerifier(Webflux Test 객체)
                StepVerifier.create(flux)
                        .expectNextCount(testCount)
                        .verifyComplete(); // 스트림이 정상적으로 완료되는지 확인;

                // 수신검증 by AkkaTest 관찰자
                for (int i = 0; i < testCount; i++) {
                    probe.expectMsgClass(ConfirmEvent.class);
                }

                within(Duration.ofSeconds(testTimeLimitSecond), () ->
                {
                    expectNoMessage();  // 중복발송 확인을 위해~ 수신이 없나를 추가검증
                    return null;
                });
            }
        };
    }
}
