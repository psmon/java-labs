package com.webnori.springweb;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.example.akka.AkkaManager;
import com.webnori.springweb.example.akka.HelloWorld;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

// https://doc.akka.io/docs/akka/current/testing.html
public class AkkaBasicTests extends AbstractJavaTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = AkkaManager.getInstance().getActorSystem();
        //system.logConfiguration();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    @DisplayName("Actor - HelloWorld Test")
    public void TestIt() {
        new TestKit(system) {
            {
                final TestKit probe = new TestKit(system);
                final ActorRef greetActor = AkkaManager.getInstance().getGreetActor();

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                within(
                        Duration.ofSeconds(3),
                        () -> {
                            greetActor.tell("hello", getRef());

                            awaitCond(probe::msgAvailable);

                            // check that the probe we injected earlier got the msg
                            probe.expectMsg(Duration.ZERO, "world");

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }

    @Test
    @DisplayName("Actor - HelloWorld Test")
    public void TestItMany() {
        new TestKit(system) {
            {
                final TestKit probe = new TestKit(system);
                final ActorRef greetActor = AkkaManager.getInstance().getGreetActor();

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                within(
                        Duration.ofSeconds(3),
                        () -> {

                            int testCount = 1000;

                            for(int i=0; i<testCount; i++){
                                greetActor.tell("hello", getRef());
                            }

                            for(int i=0; i<testCount; i++){
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

    @Test
    @DisplayName("Actor - HelloWorld Test")
    public void TestItManyByRouter() {
        new TestKit(system) {
            {
                final TestKit probe = new TestKit(system);
                int poolCount = 100;
                final  ActorRef greetActor = system.actorOf(new RoundRobinPool(poolCount).props(HelloWorld.Props()
                        .withDispatcher("my-dispatcher-test1")), "router2");

                for(int i =0 ; i<poolCount ; i++) {
                    greetActor.tell(probe.getRef(), getRef());
                    expectMsg(Duration.ofSeconds(1), "done");
                }

                for(int i =0 ; i<poolCount ; i++) {
                    //greetActor.tell("command:tobeslow", getRef());
                }

                within(
                        Duration.ofSeconds(100),
                        () -> {

                            int testCount = 1000;

                            for(int i=0; i<testCount; i++){
                                greetActor.tell("hello", getRef());
                            }

                            for(int i=0; i<testCount; i++){
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
