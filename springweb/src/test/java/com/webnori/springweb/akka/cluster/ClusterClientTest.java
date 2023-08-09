package com.webnori.springweb.akka.cluster;

import akka.actor.ActorRef;
import com.webnori.springweb.example.akka.actors.cluster.ClusterListener;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.io.IOException;
import akka.testkit.javadsl.TestKit;


@SpringBootTest
public class ClusterClientTest {

    private static Logger logger = LoggerFactory.getLogger(ClusterClientTest.class);
    private static ActorSystem clusterSystem1;
    private static ActorSystem clusterSystem2;
    private static ActorSystem clusterSystem3;

    private int maxServerUptime = 20;

    private static ActorSystem serverStart(String sysName,String config,String role) throws IOException {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]",role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig );
        serverSystem.actorOf(Props.create(ClusterListener.class), "clusterListener");
        return serverSystem;
    }

    @BeforeClass
    public static void setup() throws IOException {
        // Seed
        clusterSystem1 = serverStart("ClusterSystem","server","seed");

        // Works Nodes
        clusterSystem2 = serverStart("ClusterSystem","factorial","backend");
        clusterSystem2.actorOf(Props.create(FactorialBackend.class), "factorialBackend");

        clusterSystem3 = serverStart("ClusterSystem","factorial","backend");
        clusterSystem3.actorOf(Props.create(FactorialBackend.class), "factorialBackend");

        logger.info("========= sever loaded =========");
    }

    @AfterClass
    public static void gracefulDown() {
        clusterSystem3.terminate();
        clusterSystem2.terminate();
        clusterSystem1.terminate();
        logger.info("========= sever down =========");
    }

    @Test
    public void clientTest() {
        logger.info("========= client start =========");
        final int upToN = 200;

        final Config config = ConfigFactory.parseString(
                "akka.cluster.roles = [frontend]").withFallback(
                ConfigFactory.load("factorial"));

        final ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.log().info("Factorials will start when 2 backend members in the cluster.");

        new TestKit(system) {
            {
                ActorRef probe = getRef();
                Cluster.get(system).registerOnMemberUp(new Runnable() {
                    @Override
                    public void run() {
                        ActorRef frontActor = system.actorOf(Props.create(FactorialFrontend.class, upToN, false),
                                "factorialFrontend");
                        frontActor.tell(new FactorialRequest(upToN),probe);
                    }
                });
                expectMsgClass(Duration.ofSeconds(maxServerUptime), FactorialResult.class);
            }};
    }
}