package com.webnori.springweb.alpakka.s3;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.alpakka.s3.S3Attributes;
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
import software.amazon.awssdk.regions.Region;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * TestClass : S3IOTest
 * 목표 : S3 IO Test
 * 참고 링크 : https://doc.akka.io/docs/akka/current/testing.html
 */

public class S3IOTest {

    private static final Logger logger = LoggerFactory.getLogger(S3IOTest.class);
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

    @Test
    @DisplayName("UploadAndDownLoadS3")
    public void UploadAndDownLoadS3() {
        new TestKit(actorSystem) {
            {
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem).withDispatcher("my-dispatcher-streamtest");

                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);
                final TestKit probe = new TestKit(actorSystem);

                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");
                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // AWS S3 Config - test.conf 참고
                final Config config = actorSystem.settings().config().getConfig("alpakka.s3");

                // 기존 Config를 사용하여 S3Settings 인스턴스 생성
                S3Settings initialSettings = S3Settings.create(config);

                // 원하는 리전으로 S3Settings 수정
                Region desiredRegion = Region.US_EAST_1;  // 예시로 'us-west-2' 리전 사용
                S3Settings s3Settings = initialSettings.withS3RegionProvider(() -> desiredRegion);


                // S3 버킷 및 파일 이름 정의
                final String bucketName = "my-bucket";
                final String fileKey = "example.txt";

                System.out.println("Try Upload");

                Source<ByteString, ?> byteSource = Source.single(ByteString.fromString("file contents"));

                byteSource
                    .runWith(S3.multipartUpload(bucketName, fileKey)
                            .withAttributes(S3Attributes.settings(s3Settings)), materializer)
                    .thenAccept(result -> {
                        System.out.println("Upload complete: " + result.location());
                        greetActor.tell("hello", null);
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Upload failed: " + throwable.getMessage());
                        return  null;
                    });

                System.out.println("Wait for UploadCompleted");

                // 액터 수신확인을 통한 완료검증~
                probe.expectMsg(Duration.ofSeconds(3), "world");

                System.out.println("Try Download");

                // 파일 다운로드
                S3.download(bucketName, fileKey)
                        .withAttributes(S3Attributes.settings(s3Settings))
                        .runWith(Sink.head(), materializer)
                        .thenApply(opt -> opt.orElseThrow(() -> new RuntimeException("File not found")))
                        .thenAccept(bytes ->{
                            System.out.println("Download complete: " + bytes.toString());
                            greetActor.tell("hello", null);
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Upload failed: " + throwable.getMessage());
                            return  null;
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

    @Test
    @DisplayName("UploadAndDownLoadS3Throttle10")
    public void UploadAndDownLoadS3Throttle10() {
        new TestKit(actorSystem) {
            {
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem).withDispatcher("my-dispatcher-streamtest");

                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);
                final TestKit probe = new TestKit(actorSystem);

                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");
                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // AWS S3 Config - test.conf 참고
                final Config config = actorSystem.settings().config().getConfig("alpakka.s3");

                // 기존 Config를 사용하여 S3Settings 인스턴스 생성
                S3Settings initialSettings = S3Settings.create(config);

                // 원하는 리전으로 S3Settings 수정
                Region desiredRegion = Region.US_EAST_1;  // 예시로 'us-west-2' 리전 사용
                S3Settings s3Settings = initialSettings.withS3RegionProvider(() -> desiredRegion);

                int processCouuntPerSec = 1;
                int testCount = 10;

                // S3 버킷 및 파일 이름 정의
                final String bucketName = "my-bucket";
                final String fileKey = "example.txt";

                System.out.println("Try Upload");

                List<ByteString> byteStrings = new ArrayList<>();
                for (int i = 0; i < testCount; i++) {
                    byteStrings.add(ByteString.fromString("file contents " + i));
                }

                Source<ByteString, ?> byteSource = Source.from(byteStrings);

                byteSource
                        .throttle(processCouuntPerSec, Duration.ofSeconds(1))
                        .runWith(S3.multipartUpload(bucketName, fileKey)
                                .withAttributes(S3Attributes.settings(s3Settings)), materializer)
                        .thenAccept(result -> {
                            System.out.println("Upload complete: " + result.location());
                            greetActor.tell("hello", null);
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Upload failed: " + throwable.getMessage());
                            return  null;
                        });

                System.out.println("Wait for UploadCompleted");

                // 액터 수신검증을 이용 완료검증
                probe.expectMsg(Duration.ofSeconds(15), "world");

                System.out.println("Try Download");

                // 파일 다운로드
                S3.download(bucketName, fileKey)
                        .withAttributes(S3Attributes.settings(s3Settings))
                        .runWith(Sink.head(), materializer)
                        .thenApply(opt -> opt.orElseThrow(() -> new RuntimeException("File not found")))
                        .thenAccept(bytes ->{
                            System.out.println("Download complete: " + bytes.toString());
                            greetActor.tell("hello", null);
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Upload failed: " + throwable.getMessage());
                            return  null;
                        });

                within(
                        // 테스트 최대시간은 설정 : 이시간을 초과하면 유닛테스트 실패
                        Duration.ofSeconds(2 * testCount),
                        () -> {

                            probe.expectMsg(Duration.ofSeconds(3), "world");

                            expectNoMessage(Duration.ofSeconds(1));

                            return null;
                        });
            }
        };
    }

}
