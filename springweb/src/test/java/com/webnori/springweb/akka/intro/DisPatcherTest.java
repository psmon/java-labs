package com.webnori.springweb.akka.intro;

import akka.actor.ActorRef;
import akka.routing.RoundRobinPool;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.akka.AbstractJavaTest;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;


/**
 * TestClass : DisPatcherTest
 * 목표 : 액터의 스케쥴을 관장하는 Dispatcher를 테스트하고 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

public class DisPatcherTest extends AbstractJavaTest {

    @Test
    @DisplayName("Actor - HelloWorld Dispacher Test")
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
