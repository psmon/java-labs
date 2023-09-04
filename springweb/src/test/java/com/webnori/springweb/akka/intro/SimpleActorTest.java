package com.webnori.springweb.akka.intro;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.RoundRobinGroup;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.GreetingActor;
import com.webnori.springweb.example.akka.actors.SafeBatchActor;
import com.webnori.springweb.example.akka.actors.TestTimerActor;
import com.webnori.springweb.example.akka.models.FakeSlowMode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TestClass : SimpleActorTest
 * 목표 : 몇가지 유용한 액터샘플을 통해 액터의 기능을 살펴봅니다.
 * 참고 링크 : https://wiki.webnori.com/display/AKKA
 */

public class SimpleActorTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleActorTest.class);
    private static final String hello = "not another hello world";
    private static ActorSystem actorSystem;

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
    @DisplayName("Actor - HelloWorld Test")
    public void HelloWorld() {

        // https://doc.akka.io/docs/akka/current/actors.html#creating-actors
        // 기능 : 액터모델을 생성하고 메시지를 전송합니다.

        new TestKit(actorSystem) {
            {
                ActorRef greetActor = actorSystem.actorOf(GreetingActor.Props(), "greetActor");
                greetActor.tell("Hello World!", ActorRef.noSender());
                expectNoMessage();
            }
        };
    }

    @Test
    @DisplayName("Actor - ThrottlerTest Test")
    public void ThrottlerTest() {

        //https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html
        // 기능 : 속도제어장치를 액터 앞단에 달아서, TPS를 제어할수 있습니다.

        new TestKit(actorSystem) {
            {
                final Materializer materializer = ActorMaterializer.create(actorSystem);

                ActorRef greetActor = actorSystem.actorOf(GreetingActor.Props(), "greetActor");
                int processCouuntPerSec = 3;
                final ActorRef throttler1 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                final ActorRef throttler2 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                for (int i = 0; i < 100; i++) {
                    throttler1.tell("#### Hello World!", ActorRef.noSender());
                    throttler2.tell("$$$$ Hello World!", ActorRef.noSender());
                }

                expectNoMessage(Duration.ofSeconds(10));
            }
        };
    }

    @Test
    @DisplayName("Actor - RoundRobinTest Test")
    public void RoundRobinTest() {

        // https://doc.akka.io/docs/akka/current/routing.html
        // 실시간으로 발생하는 이벤트를 분배기를 통해 동시에 실행할수 있습니다.
        // 멀티스레드에 대응하며 분산처리된 액터단위로 순차성은 보장되지 않습니다.

        new TestKit(actorSystem) {
            {
                ActorRef w1 = actorSystem.actorOf(GreetingActor.Props(), "w1");
                ActorRef w2 = actorSystem.actorOf(GreetingActor.Props(), "w2");
                ActorRef w3 = actorSystem.actorOf(GreetingActor.Props(), "w3");

                List<String> paths = Arrays.asList("akka:1.1.1.1/user/w1", "/user/w2", "/user/w3");
                ActorRef router = actorSystem.actorOf(new RoundRobinGroup(paths).props(), "router");

                for (int i = 0; i < 100; i++) {
                    router.tell("#### Hello World!" + i, ActorRef.noSender());
                }
                expectNoMessage(Duration.ofSeconds(3));
            }
        };
    }

    @Test
    @DisplayName("Actor - RoundRobinThrottleTest Test")
    public void RoundRobinThrottleTest() {

        // 동시성 병렬처리 문제
        // 스레드 3개 작동된다고 TPS3으로 작동되는것이 아니며 각 스레드는 대기가 있을수 있기때문입니다.
        // 생산명령을 3으로 조절하고, 처리 스레드는 N개가 있어야 3TPS처리에 가깝게 처리할수 있습니다.

        new TestKit(actorSystem) {
            {
                //Given
                int concurrencyCount = 10;      //동시처리능력
                int processCouuntPerSec = 3;    //초당 처리 밸브
                int maxBufferSize = 3000;       //최대버퍼수(넘을시 Drop전략)
                int testCallCount = 100;

                final Materializer materializer = ActorMaterializer.create(actorSystem);
                List<String> paths = new ArrayList<>();

                for (int i = 0; i < concurrencyCount; i++) {
                    String pathName = "w" + (i + 1);
                    ActorRef work = actorSystem.actorOf(GreetingActor.Props("my-dispatcher-test1"), pathName);
                    work.tell(new FakeSlowMode(), ActorRef.noSender());
                    paths.add("/user/" + pathName);
                }

                ActorRef router = actorSystem.actorOf(new RoundRobinGroup(paths).props(), "router");

                final ActorRef throttler =
                        Source.actorRef(maxBufferSize, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, ThrottleMode.shaping())
                                .to(Sink.actorRef(router, akka.NotUsed.getInstance()))
                                .run(materializer);

                for (int i = 0; i < testCallCount; i++) {
                    throttler.tell("#### Hello World!" + i, ActorRef.noSender());
                }

                int expectedCompleteTime = testCallCount / 3;

                expectNoMessage(Duration.ofSeconds(expectedCompleteTime));
            }
        };
    }

    @Test
    @DisplayName("Actor - TimerActor Test")
    public void TimerActor() {

        // https://doc.akka.io/docs/akka/current/actors.html#timers-scheduled-messages
        // 기능 : Actor에 반복작동하는 스케줄러기능을 탑재할수 있습니다.
        // 스케줄러를 탑재하면서 메시지 전송도 가능합니다.

        new TestKit(actorSystem) {
            {
                ActorRef greetActor = actorSystem.actorOf(TestTimerActor.Props(), "timerActor");

                greetActor.tell("Hello~ World........", ActorRef.noSender());

                expectNoMessage(Duration.ofSeconds(10));
            }
        };
    }

    @Test
    @DisplayName("Actor - SafeBatchActor Test")
    public void SafeBatchActor() {

        // ThrottlerTest(초당제한) + TimerActor(초당처리)를 조합하여 스마트한 고성능 준실시간성 벌크처리를 작성할수 있습니다.
        // 심플한 코드로 FSMBatch 처리기와 같은 기능을 만들수 있습니다.

        new TestKit(actorSystem) {
            {
                final Materializer materializer = ActorMaterializer.create(actorSystem);

                ActorRef batchActor = actorSystem.actorOf(SafeBatchActor.Props(), "batchActor");

                int processCouuntPerSec = 100;
                final ActorRef throttler =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, ThrottleMode.shaping())
                                .to(Sink.actorRef(batchActor, akka.NotUsed.getInstance()))
                                .run(materializer);


                for (int i = 0; i < 10000; i++) {
                    throttler.tell("#### Hello World!", ActorRef.noSender());
                }

                expectNoMessage(Duration.ofSeconds(10));

            }
        };
    }

}
