package com.webnori.springweb.alpakka.s3;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.alpakka.s3.S3Attributes;
import akka.stream.alpakka.s3.S3Ext;
import akka.stream.alpakka.s3.S3Settings;
import akka.stream.alpakka.s3.javadsl.S3;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * TestClass : S3IOTest
 * 목표 : S3 IO Test
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
    @DisplayName("UploadS3")
    public void UploadS3() {
        new TestKit(actorSystem) {
            {
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem).withDispatcher("my-dispatcher-streamtest");

                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);
                final TestKit probe = new TestKit(actorSystem);

                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");
                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                final Config config = actorSystem.settings().config().getConfig("alpakka.s3");

                S3Settings s3Settings = S3Settings.create(config);
                S3Ext s3Ext = S3Ext.get(actorSystem);

                // 업로드할 데이터
                final Source<ByteString, NotUsed> fileSource = Source.single(ByteString.fromString("Hello, S3!"));

                // S3 버킷 및 파일 이름 정의
                final String bucketName = "my-bucket";
                final String key = "testKey";
                final String fileName = "test/test.txt";

                // S3에 파일 업로드
                fileSource.runWith(S3.multipartUpload(bucketName, fileName)
                                .withAttributes(S3Attributes.settings(s3Settings)), materializer)
                        .thenAccept(result -> {
                            System.out.println("Upload complete: " + result);
                            greetActor.tell("hello", null);
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Upload failed: " + throwable.getMessage());
                            return null;
                        });

                // S3로부터 파일 다운로드
                S3.download(bucketName, fileName)
                        .withAttributes(S3Attributes.settings(s3Settings))
                        .runWith(Sink.head(), materializer)
                        .thenAccept(result -> {
                            if (result.isPresent()) {
                                Source<ByteString, ?> fileReadSource = result.get().first();
                                fileReadSource.runForeach(data -> System.out.println(data.utf8String()), materializer)
                                        .whenComplete((done, exc) -> {
                                            if (exc != null) {
                                                System.err.println("Download failed: " + exc.getMessage());
                                            } else {
                                                System.out.println("Download complete");
                                                greetActor.tell("hello", null);
                                            }
                                        });
                            } else {
                                System.err.println("File not found");
                            }
                        });

                within(
                        Duration.ofSeconds(5),
                        () -> {

                            probe.expectMsg(Duration.ofSeconds(3), "world");

                            expectNoMessage(Duration.ofSeconds(1));

                            return null;
                        });
            }
        };
    }
}
