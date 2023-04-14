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

        // https://doc.akka.io/docs/akka/current/actors.html#creating-actors
        // 기능 : 액터모델을 생성하고 메시지를 전송합니다.

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

        //https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html
        // 기능 : 속도제어장치를 액터 앞단에 달아서, TPS를 제어할수 있습니다.

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

        // https://doc.akka.io/docs/akka/current/routing.html
        // 실시간으로 발생하는 이벤트를 분배기를 통해 동시에 실행할수 있습니다.
        // 멀티스레드에 대응하며 분산처리된 액터단위로 순차성은 보장되지 않습니다.

        new TestKit(system) {
            {
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

        // https://doc.akka.io/docs/akka/current/actors.html#timers-scheduled-messages
        // 기능 : Actor에 반복작동하는 스케줄러기능을 탑재할수 있습니다.
        // 스케줄러를 탑재하면서 메시지 전송도 가능합니다.

        new TestKit(system) {
            {
                ActorRef greetActor = system.actorOf(TestTimerActor.Props(), "timerActor");

                greetActor.tell("Hello~ World........", ActorRef.noSender());

                expectNoMessage(Duration.ofSeconds(10));
            }
        };
    }

    @Test
    @DisplayName("Actor - SafeBatchActor Test")
    public void SafeBatchActor(){

        // ThrottlerTest(초당제한) + TimerActor(초당처리)를 조합하여 스마트한 고성능 준실시간성 벌크처리를 작성할수 있습니다.
        // 심플한 코드로 FSMBatch 처리기와 같은 기능을 만들수 있습니다.

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
