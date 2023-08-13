package com.webnori.springweb.akka.intro;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.akka.AbstractJavaTest;
import com.webnori.springweb.example.akka.AkkaManager;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;


/**
 * TestClass : BasicTest
 * 목표 : 액터의 기본메시지 전송을 확인하고, 이벤트를 유닛테스트화 하는방법을 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
*/

public class BasicTest extends AbstractJavaTest {


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
    @DisplayName("Actor - HelloWorld Tests")
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
