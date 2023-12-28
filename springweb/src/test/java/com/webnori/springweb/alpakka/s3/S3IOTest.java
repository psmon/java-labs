package com.webnori.springweb.alpakka.s3;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.stream.actor.SlowConsumerActor;
import com.webnori.springweb.akka.stream.actor.TpsMeasurementActor;
import com.webnori.springweb.akka.stream.actor.model.TPSInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;


/**
 * TestClass : BasicTest
 * 목표 : 액터의 기본메시지 전송을 확인하고, 이벤트를 유닛테스트화 하는방법을 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

public class S3IOTest {

    private static final Logger logger = LoggerFactory.getLogger(S3IOTest.class);
    private static final String hello = "not another hello world";
    private static final Executor executor = Executors.newFixedThreadPool(450);
    private static ActorSystem actorSystem;
    private static ActorRef tpsActor;

    private static ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(String.format("akka.cluster.roles = [%s]", role)).withFallback(ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);
        return serverSystem;
    }

    @BeforeClass
    public static void setup() {
        // Seed
        actorSystem = serverStart("ClusterSystem", "test", "seed");
        logger.info("========= sever loaded =========");
    }

    private static CompletionStage<String> callApiAsync(Integer param) {
        // CompletableFuture를 사용하여 비동기 처리 구현
        return CompletableFuture.supplyAsync(() -> {
            try {

                double dValue = Math.random();
                int iValue = (int) (dValue * 1000);
                Thread.sleep(iValue); // API 응답 시간을 시뮬레이션하기 위한 지연
                tpsActor.tell("CompletedEvent", ActorRef.noSender());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Response for " + param;
        }, executor);
    }

    @Test
    @DisplayName("ThrottleTest 100")
    public void ThrottleTest100() {
        UploadS3ThrottleTest(100, 5000);
    }

    public void UploadS3ThrottleTest(int tps, int testCount) {
        new TestKit(actorSystem) {
            {
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem).withDispatcher("my-dispatcher-streamtest");

                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);
                final TestKit probe = new TestKit(actorSystem);

                tpsActor = actorSystem.actorOf(TpsMeasurementActor.Props(), "TpsActor");
                tpsActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                final Source<ByteString, NotUsed> file = Source.single(ByteString.fromString(body()));

                final Sink<ByteString, CompletionStage<MultipartUploadResult>> s3Sink =
                        S3.multipartUpload(bucket(), bucketKey());

                // Source 생성
                Source<Integer, NotUsed> source = Source.range(1, testCount);

                // 병렬 처리를 위한 Flow 정의
                final int parallelism = 450;
                Flow<Integer, String, NotUsed> parallelFlow = Flow.<Integer>create().mapAsync(parallelism, S3IOTest::callApiAsync);

                // Buffer 설정 및 OverflowStrategy.backpressure 적용
                int bufferSize = 100000;
                Flow<Integer, Integer, NotUsed> backpressureFlow = Flow.<Integer>create().buffer(bufferSize, OverflowStrategy.backpressure());

                AtomicInteger processedCount = new AtomicInteger();

                // Sink 정의
                Sink<String, CompletionStage<Done>> sink = Sink.foreach(s -> {
                    //처리완료
                    processedCount.getAndIncrement();
                    if (processedCount.getAcquire() % 10 == 0) {
                        //System.out.println("Processed 10");
                    }
                });

                System.out.println("Run backpressureFlow bufferSize:" + bufferSize);

                // RunnableGraph 생성 및 실행
                source.via(backpressureFlow).throttle(tps, FiniteDuration.create(1, TimeUnit.SECONDS), tps, (ThrottleMode) ThrottleMode.shaping()).via(parallelFlow).to(sink).run(materializer);

                within(Duration.ofSeconds(15), () -> {
                    // Will wait for the rest of the 10 seconds
                    expectNoMessage(Duration.ofSeconds(10));
                    return null;
                });
            }
        };
    }
}

/*


 */
