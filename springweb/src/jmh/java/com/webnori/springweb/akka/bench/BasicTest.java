package com.webnori.springweb.akka.bench;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * TestClass : BasicTest
 * 목표 : 액터의 기본메시지 전송을 성능테스트화 확인
 * 참고 링크 : https://ysjee141.github.io/blog/quality/java-benchmark/
 */

@State(Scope.Thread)
public class BasicTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicTest.class);
    int testEventCount = 1000;
    private Blackhole blackhole;
    private ActorSystem actorSystem;

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
    @BenchmarkMode(Mode.Throughput)
    @Measurement(iterations = 1)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int HelloWorldTest(MyState state) {

        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props());

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                within(
                        Duration.ofSeconds(3),
                        () -> {

                            for (int i = 0; i < testEventCount; i++) {
                                greetActor.tell("hello", getRef());
                            }

                            for (int i = 0; i < testEventCount; i++) {
                                // check that the probe we injected earlier got the msg
                                probe.expectMsg(Duration.ofSeconds(1), "world");
                                state.count++;
                            }

                            blackhole.consume(testEventCount);
                            return null;
                        });
            }
        };

        return testEventCount;
    }

    @Test
    public void runBenchmarks() throws Exception {
        Options options = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(6)
                .threads(1)
                .measurementIterations(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        new Runner(options).run();
    }

    public interface Counter {
        int inc();
    }

    @State(Scope.Thread)
    public static class MyState {
        public int count = 0;
    }


}

/* Report Sample :
env : wm BULK
Result "com.webnori.springweb.akka.bench.BasicTest.HelloWorldTest":
  32.205 ops/s


# Run complete. Total time: 00:00:23

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                  Mode  Cnt   Score   Error  Units
BasicTest.HelloWorldTest  thrpt       32.205          ops/s
 */
