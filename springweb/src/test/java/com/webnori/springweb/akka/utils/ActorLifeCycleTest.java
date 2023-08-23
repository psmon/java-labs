package com.webnori.springweb.akka.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Kill;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.utils.actor.LifeCycleTestActor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

/**
 * TestClass : ActorLifeCycleTest
 * 목표 : 액터의 라이플사이클에대해 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/actors.html#stopping-actors
 */
@SpringBootTest
public class ActorLifeCycleTest {

    private static Logger logger = LoggerFactory.getLogger(CoordinatedShutdownTest.class);

    private static ActorSystem actorSystem;

    private static ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]", role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);
        return serverSystem;
    }

    @BeforeClass
    public static void bootUp() {
        actorSystem = serverStart("ClusterSystem", "router-test", "seed");
        logger.info("========= sever loaded =========");

    }

    @AfterClass
    public static void bootDown() {
        logger.info("========= try graceful down =========");
        actorSystem.terminate();
        logger.info("========= graceful down =========");
    }

    @Test
    public void ActorPoisonPillTest() {

        // Poison메시지는 현재까지 적재된 메시지만 처리하고~ 액터를 중단시킵니다.
        var testActor = actorSystem.actorOf(LifeCycleTestActor.Props(), "testActor");

        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);

                testActor.tell(probe.getRef(), getRef());

                for (int i = 0; i < 10; i++) {
                    testActor.tell("someWork", ActorRef.noSender());
                }

                // 테스트 주요 포인트 : PoisonPill은 이 메시지까지 받고 중지됩니다.
                testActor.tell(PoisonPill.getInstance(), ActorRef.noSender());

                within(Duration.ofSeconds(10), () -> {

                    // try 10 and more 1
                    testActor.tell("someWork", ActorRef.noSender());

                    // check that the probe we injected earlier got the msg
                    probe.expectMsg(Duration.ofSeconds(10), "done");

                    // Will wait for the rest of the 3 seconds
                    probe.expectNoMessage();

                    assertEquals(10, LifeCycleTestActor.workCountForTest);

                    return null;
                });
            }
        };
    }

    @Test
    public void ActorStopTest() {

        // stop은 대기중인 메시지를 고려하지 않고 즉각 중단시킵니다.
        var testActor = actorSystem.actorOf(LifeCycleTestActor.Props(), "testActor");

        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);

                testActor.tell(probe.getRef(), getRef());

                for (int i = 0; i < 10; i++) {
                    testActor.tell("someWork", ActorRef.noSender());
                }

                actorSystem.stop(testActor);

                within(Duration.ofSeconds(10), () -> {

                    // more 1
                    testActor.tell("someWork", ActorRef.noSender());

                    // check that the probe we injected earlier got the msg
                    probe.expectMsg(Duration.ofSeconds(10), "cancel");

                    // Will wait for the rest of the 3 seconds
                    probe.expectNoMessage();

                    assertEquals(true, LifeCycleTestActor.workCountForTest < 10);

                    return null;
                });

            }
        };
    }

}
