package com.webnori.springweb.akka;

import akka.actor.ActorRef;
import akka.routing.RoundRobinGroup;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.akka.actors.GreetingActor;
import com.webnori.springweb.akka.actors.SafeBatchActor;
import com.webnori.springweb.akka.actors.TestTimerActor;
import com.webnori.springweb.example.akka.AkkaManager;
import com.webnori.springweb.example.akka.TimerActor;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleActorTest extends AbstractJavaTest {

    @Test
    @DisplayName("Actor - HelloWorld Test")
    public void HelloWorld(){
        new TestKit(system) {
            {
                ActorRef greetActor = system.actorOf(GreetingActor.Props(), "greetActor");
                greetActor.tell("Hello World!",ActorRef.noSender());
                expectNoMessage();
            }
        };
    }

    @Test
    @DisplayName("Actor - ThrottlerTest Test")
    public  void ThrottlerTest(){
        new TestKit(system) {
            {
                final Materializer materializer = ActorMaterializer.create(system);

                ActorRef greetActor = system.actorOf(GreetingActor.Props(), "greetActor");
                int processCouuntPerSec = 3;
                final ActorRef throttler1 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                final ActorRef throttler2 =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                for(int i=0 ; i<100 ; i++){
                    throttler1.tell("#### Hello World!",ActorRef.noSender());
                    throttler2.tell("$$$$ Hello World!",ActorRef.noSender());
                }

                expectNoMessage(Duration.ofSeconds(10));
            }
        };
    }

    @Test
    @DisplayName("Actor - RoundRobinTest Test")
    public void RoundRobinTest(){
        new TestKit(system) {
            {
                // https://doc.akka.io/docs/akka/current/routing.html
                system.actorOf(GreetingActor.Props(),"w1");
                system.actorOf(GreetingActor.Props(),"w2");
                system.actorOf(GreetingActor.Props(),"w3");

                List<String> paths = Arrays.asList("/user/w1", "/user/w2", "/user/w3");
                ActorRef router = system.actorOf(new RoundRobinGroup(paths).props(), "router");

                for(int i=0 ; i<100 ; i++){
                    router.tell("#### Hello World!" + i ,ActorRef.noSender());
                }
                expectNoMessage(Duration.ofSeconds(3));
            }
        };
    }

    @Test
    @DisplayName("Actor - TimerActor Test")
    public void TimerActor(){
        new TestKit(system) {
            {
                ActorRef greetActor = system.actorOf(TestTimerActor.Props(), "timerActor");
                expectNoMessage(Duration.ofSeconds(10));
            }
        };
    }

    @Test
    @DisplayName("Actor - SafeBatchActor Test")
    public void SafeBatchActor(){
        new TestKit(system) {
            {
                final Materializer materializer = ActorMaterializer.create(system);

                ActorRef batchActor = system.actorOf(SafeBatchActor.Props(), "batchActor");

                int processCouuntPerSec = 100;
                final ActorRef throttler =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(batchActor, akka.NotUsed.getInstance()))
                                .run(materializer);


                for(int i=0 ; i<10000 ; i++){
                    throttler.tell("#### Hello World!",ActorRef.noSender());
                }

                expectNoMessage(Duration.ofSeconds(10));

            }
        };
    }

}
