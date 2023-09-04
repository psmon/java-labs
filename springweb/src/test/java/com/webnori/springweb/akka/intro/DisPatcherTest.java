package com.webnori.springweb.akka.intro;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.router.routing.BasicRoutingTest;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;


/**
 * TestClass : DisPatcherTest
 * 목표 : 액터의 스케쥴을 관장하는 Dispatcher를 테스트하고 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

public class DisPatcherTest {

    private static final Logger logger = LoggerFactory.getLogger(DisPatcherTest.class);

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
    @DisplayName("Actor - HelloWorld Dispacher Test")
    public void TestItManyByRouter() {
        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);
                int poolCount = 100;
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props()
                        .withDispatcher("my-dispatcher-test1"), "HelloWorld");

                for (int i = 0; i < poolCount; i++) {
                    greetActor.tell(probe.getRef(), getRef());
                    expectMsg(Duration.ofSeconds(1), "done");
                }

                for (int i = 0; i < poolCount; i++) {
                    greetActor.tell("command:tobeslow", getRef());
                }

                within(
                        Duration.ofSeconds(100),
                        () -> {

                            int testCount = 1000;

                            for (int i = 0; i < testCount; i++) {
                                greetActor.tell("hello", getRef());
                            }

                            for (int i = 0; i < testCount; i++) {
                                // check that the probe we injected earlier got the msg
                                probe.expectMsg(Duration.ofSeconds(1), "world");
                            }

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}
