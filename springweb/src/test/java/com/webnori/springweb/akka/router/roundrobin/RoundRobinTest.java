package com.webnori.springweb.akka.router.roundrobin;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.FromConfig;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest
public class RoundRobinTest {

    private static Logger logger = LoggerFactory.getLogger(RoundRobinTest.class);

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
        actorSystem = serverStart("ClusterSystem", "roundrobin", "seed");
        logger.info("========= sever loaded =========");
    }

    @AfterClass
    public static void gracefulDown() {
        actorSystem.terminate();
        logger.info("========= sever down =========");
    }

    @Test
    public void TestRoundRobinPool() {

        new TestKit(actorSystem) {
            {
                int testCount = 100;
                final TestKit probe = new TestKit(actorSystem);

                ActorRef router1 =
                        actorSystem.actorOf(FromConfig.getInstance().props(WorkerActor.Props(probe.getRef())), "router1");

                within(
                        Duration.ofSeconds(3),
                        () -> {
                            for (int i = 0; i < testCount; i++) {
                                router1.tell(new WorkMessage("message-" + i), ActorRef.noSender());
                            }

                            awaitCond(probe::msgAvailable);

                            for (int i = 0; i < testCount; i++) {
                                probe.expectMsg("completed");
                            }
                            probe.expectNoMessage();
                            return null;
                        });
            }
        };
    }

}
