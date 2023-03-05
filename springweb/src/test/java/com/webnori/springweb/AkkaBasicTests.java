package com.webnori.springweb;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.example.akka.AkkaManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;


public class AkkaBasicTests extends AbstractJavaTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = AkkaManager.getInstance().getActorSystem();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
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
}
