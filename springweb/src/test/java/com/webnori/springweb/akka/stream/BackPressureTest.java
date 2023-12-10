package com.webnori.springweb.akka.stream;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.stream.actor.SlowConsumerActor;
import com.webnori.springweb.akka.stream.actor.TpsMeasurementActor;
import com.webnori.springweb.akka.stream.actor.model.TPSInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;


/**
 * TestClass : BasicTest
 * 목표 : 액터의 기본메시지 전송을 확인하고, 이벤트를 유닛테스트화 하는방법을 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

public class BackPressureTest {

    private static final Logger logger = LoggerFactory.getLogger(BackPressureTest.class);

    private static ActorSystem actorSystem;
    private static final String hello = "not another hello world";

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
    @DisplayName("TpsMeasurementActorTest")
    public void TpsMeasurementActorTest() {
        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);
                final ActorRef tpsActor = actorSystem.actorOf(TpsMeasurementActor.Props(), "TpsActor");

                tpsActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                within(
                        Duration.ofSeconds(20),
                        () -> {

                            try {
                                for(int i=0;i<1200;i++){
                                    tpsActor.tell("some Event", getRef());
                                }
                                sleep(1500);

                                for(int i=0;i<600;i++){
                                    tpsActor.tell("some Event", getRef());
                                }
                                sleep(1500);

                                for(int i=0;i<300;i++){
                                    tpsActor.tell("some Event", getRef());
                                }
                                sleep(1500);

                                for(int i=0;i<500;i++){
                                    tpsActor.tell("some Event", getRef());
                                }

                                sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

                            TPSInfo expectedTcp = new TPSInfo(0);
                            tpsActor.tell("tps", getRef());
                            expectMsg(expectedTcp);

                            return null;
                        });
            }
        };
    }

    @Test
    @DisplayName("SlowConsumerActorTest")
    public void SlowConsumerActorTest() {
        new TestKit(actorSystem) {
            {
                final Materializer materializer = ActorMaterializer.create(actorSystem);
                final TestKit probe = new TestKit(actorSystem);
                final ActorRef slowConsumerActor = actorSystem.actorOf(SlowConsumerActor.Props(), "SlowConsumerActor");
                slowConsumerActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                int testCount = 50000;
                int bufferSize = 100000;
                int processCouuntPerSec = 200;

                final ActorRef throttlerTPS100 =
                        Source.actorRef(bufferSize, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, ThrottleMode.shaping())
                                .to(Sink.actorRef(slowConsumerActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            for(int i=0;i<testCount;i++){
                                throttlerTPS100.tell("hello", probe.getRef());
                            }

                            for(int i=0;i<testCount;i++){
                                probe.expectMsg( Duration.ofSeconds(3), "world");
                            }

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}
