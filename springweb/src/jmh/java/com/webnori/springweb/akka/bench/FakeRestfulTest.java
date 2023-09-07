package com.webnori.springweb.akka.bench;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import com.webnori.springweb.example.akka.models.FakeSlowMode;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * TestClass : FakeRestfulTest
 * 목표 : 공급자의 API성능 고려 + 자신의 로직처리 TPS추정을 할수 있습니다.
 * 여기서 측정된 기반으로 스레드및 분산처리 계획을 세울수도 있습니다.
 * 멀티스레드를 활용하는 경우 .threads(1) 값을 높일수 있습니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
public class FakeRestfulTest {

    private static final Logger logger = LoggerFactory.getLogger(FakeRestfulTest.class);
    private Blackhole blackhole;
    private ActorSystem actorSystem;

    private ActorRef throttler;

    private ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]", role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);

        return serverSystem;
    }

    @Setup(Level.Trial)
    public void init(final Blackhole _blackhole) {
        logger.info("========= sever loaded =========");

        blackhole = _blackhole;
        // Seed
        actorSystem = serverStart("ClusterSystem", "test", "seed");
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        TestKit.shutdownActorSystem(actorSystem);
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int HelloWorldTest(MyState state) {

        int testEventCount = 1000;

        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props().withDispatcher("my-blocking-dispatcher"));

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                FakeSlowMode fakeSlowMode = new FakeSlowMode();
                fakeSlowMode.bockTime = 500L;

                // Blocking 전략
                // Blocking전략을 위해 PlanA or PlanB 채택또는 함께 이용할수 있습니다.
                //
                // PlanA :  해당 기능(액터)이 Sleep를 사용하며 블락킹 모드로 이용한다.
                // API호출 네트워크를 지연시간이 있을때 고려할수 있습니다.
                //greetActor.tell(fakeSlowMode, getRef());

                // PlanB : 블락킹없이 Throlle을 이용 의존 API 초당처리능력 100로 제약한다.
                // 사용하는 API의 TPS스펙이 명시적일때 이용할수 있습니다.
                int givenAPiTPS = 100;
                final Materializer materializer = ActorMaterializer.create(actorSystem);
                throttler =
                        Source.actorRef(1000, OverflowStrategy.dropNew())
                                .throttle(givenAPiTPS, FiniteDuration.create(1, TimeUnit.SECONDS),
                                        givenAPiTPS, (ThrottleMode) ThrottleMode.shaping())
                                .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                                .run(materializer);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            for (int i = 0; i < testEventCount; i++) {
                                throttler.tell("hello", getRef());
                            }

                            for (int i = 0; i < testEventCount; i++) {
                                // check that the probe we injected earlier got the msg
                                probe.expectMsg(Duration.ofSeconds(1), "world");
                                state.count++;
                            }

                            return null;
                        });
            }
        };

        logger.info("count : {}", state.count);
        blackhole.consume(testEventCount);
        return testEventCount;
    }

    @Test
    public void runBenchmarks() throws Exception {
        Options options = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.Throughput)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(6)
                .threads(5)
                .measurementIterations(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        new Runner(options).run();
    }

    @State(Scope.Thread)
    public static class MyState {
        public int count = 0;
    }
}

/* Report Sample

Result "com.webnori.springweb.akka.bench.FakeRestfulTest.HelloWorldTest":
        0.553 ops/s

# Run complete. Total time: 00:03:18

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                        Mode  Cnt  Score   Error  Units
FakeRestfulTest.HelloWorldTest  thrpt       0.553          ops/s
*/
