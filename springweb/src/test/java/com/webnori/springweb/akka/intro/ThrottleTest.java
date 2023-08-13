package com.webnori.springweb.akka.intro;

import akka.actor.ActorRef;
import akka.routing.RoundRobinPool;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.akka.AbstractJavaTest;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * TestClass : ThrottleTest
 * 목표 : Throttle을 이용하는 TPS를 제어샘플
 * 참고 링크 :
 */
public class ThrottleTest extends AbstractJavaTest {

    @Test
    @DisplayName("Actor - HelloWorld Test")
    public void TestItManyThrottle() {
        new TestKit(system) {
            {
                final Materializer materializer = ActorMaterializer.create(system);

                final TestKit probe = new TestKit(system);
                int poolCount = 100;
                final ActorRef greetActor = system.actorOf(new RoundRobinPool(poolCount).props(HelloWorld.Props()
                        .withDispatcher("my-dispatcher-test1")), "router2");

                for (int i = 0; i < poolCount; i++) {
                    greetActor.tell(probe.getRef(), getRef());
                    expectMsg(Duration.ofSeconds(1), "done");
                }

                for (int i = 0; i < poolCount; i++) {
                    //greetActor.tell("command:tobeslow", getRef());
                }

                int processCouuntPerSec = 3;

                final ActorRef throttler1 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                final ActorRef throttler2 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                final ActorRef throttler3 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                within(
                        Duration.ofSeconds(100),
                        () -> {
                            int testCount = 50;
                            for (int i = 0; i < testCount; i++) {
                                throttler1.tell("hello1", getRef());
                                throttler2.tell("hello2", getRef());
                                throttler3.tell("hello3", getRef());
                            }

                            for (int i = 0; i < testCount * 3; i++) {
                                // check that the probe we injected earlier got the msg
                                probe.expectMsg(Duration.ofSeconds(100), "world");
                            }

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}
