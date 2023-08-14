package com.webnori.springweb.akka.router.routing;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.FromConfig;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.router.routing.actor.WorkMessage;
import com.webnori.springweb.akka.router.routing.actor.WorkerActor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest
public class BasicRoutingTest {

    private static Logger logger = LoggerFactory.getLogger(BasicRoutingTest.class);

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
        actorSystem = serverStart("ClusterSystem", "router-test", "seed");
        logger.info("========= sever loaded =========");
    }

    @AfterClass
    public static void gracefulDown() {
        actorSystem.terminate();
        logger.info("========= sever down =========");
    }

    public void RouterByNameTest(String routerName) {

        new TestKit(actorSystem) {
            {
                int testCount = 50;

                final TestKit probe = new TestKit(actorSystem);

                ActorRef router =
                        actorSystem.actorOf(FromConfig.getInstance().props(WorkerActor.Props(probe.getRef())), routerName);

                within(
                        Duration.ofSeconds(30),
                        () -> {

                            for (int i = 0; i < testCount; i++) {
                                router.tell(new WorkMessage("message-" + i), ActorRef.noSender());
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

    @Test
    public void RoundRobinRoutingTest(){
        RouterByNameTest("router1");
    }

    @Test
    public void RandomRoutingTest(){
        RouterByNameTest("router2");
    }

    @Test
    public void BalancingRoutingTest(){
        RouterByNameTest("router3");
    }

    @Test
    public void BalancingRoutingWithThreadPoolTest(){
        RouterByNameTest("router4");
    }

    @Test
    public void SmallestMailBoxRoutingTest(){
        RouterByNameTest("router5");
    }
}
