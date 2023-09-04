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
 * 목표 : 액터의 기본메시지 전송을 확인하고, 이벤트를 유닛테스트화 하는방법을 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
public class BasicTest {

    @State(Scope.Thread)
    public static class MyState {
        public int count = 0;
    }

    private static final Logger logger = LoggerFactory.getLogger(BasicTest.class);
    private static final String hello = "not another hello world";
    private ActorSystem actorSystem;

    private ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]", role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);
        return serverSystem;
    }

    @Setup(Level.Trial)
    public void init() {
        logger.info("========= sever loaded =========");
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
    public void HelloWorldTest(Blackhole blackhole, MyState state) {

        new TestKit(actorSystem) {
            {
                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props());

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
                                state.count++;
                            }

                            blackhole.consume(testCount);

                            return null;
                        });
            }
        };

        logger.info("count : {}", state.count);

    }

    @Test
    public void runBenchmarks() throws Exception {
        Options options = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.AverageTime)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(6)
                .threads(1)
                .measurementIterations(3)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        new Runner(options).run();
    }
}