package com.webnori.springweb;

import akka.actor.ActorRef;
import akka.routing.RoundRobinPool;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.example.akka.HelloWorld;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

// https://doc.akka.io/docs/akka/current/testing.html
public class AkkaDisPatcherTests extends AbstractJavaTest {

    @Test
    @DisplayName("Actor - HelloWorld Test")
    public void TestItManyByRouter() {
        new TestKit(system) {
            {
                final TestKit probe = new TestKit(system);
                int poolCount = 100;
                final ActorRef greetActor = system.actorOf(new RoundRobinPool(poolCount).props(HelloWorld.Props()
                        .withDispatcher("my-dispatcher-test1")), "router2");

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
