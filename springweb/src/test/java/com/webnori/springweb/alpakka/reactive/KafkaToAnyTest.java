package com.webnori.springweb.alpakka.reactive;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.alpakka.s3.ObjectMetadata;
import akka.stream.alpakka.s3.S3Attributes;
import akka.stream.alpakka.s3.S3Settings;
import akka.stream.alpakka.s3.javadsl.S3;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.utils.actor.MessageCofirmActor;
import com.webnori.springweb.alpakka.reactive.models.S3TestJsonModel;
import com.webnori.springweb.alpakka.reactive.models.S3TestModel;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;

/**
 * TestClass : KafkaToAnyTest
 * 목표 : Kafka에서 이벤트가 시작하여 ReactiveStream을 준수하는 스펙의 종착지까지 흘려보내는 Test입니다.
 * 참고 링크 :
 * - https://www.reactive-streams.org/
 * - https://doc.akka.io/docs/alpakka/current/index.html
 */

public class KafkaToAnyTest {

    private static final Logger logger = LoggerFactory.getLogger(KafkaToAnyTest.class);
    private static final String hello = "not another hello world";
    private static ActorSystem actorSystem;

    // KAFKA-TOPIC 클린테스트로 테스트가 한번이라도 꼬이면~ TOPIC초기화를 해주세요!
    // test.conf - akka.kafka.committer 정책참고
    private String testTopicName = "s3test";
    private int consumeCnt1 = 0;
    private int consumeCnt2 = 0;

    private static ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]", role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);
        return serverSystem;
    }

    @BeforeClass
    public static void setup() {
        // Seed
        actorSystem = serverStart("ClusterSystem", "test", "seed");
        logger.info("========= sever loaded =========");
    }

    //  Paqrquet를 ByteString 으로 변환
    private ByteString serializeGenericRecordToByteString(GenericRecord record) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<GenericRecord> writer = new SpecificDatumWriter<>(record.getSchema());

        writer.write(record, encoder);
        encoder.flush();
        outputStream.close();

        byte[] serializedBytes = outputStream.toByteArray();
        return ByteString.fromArray(serializedBytes);
    }

    // Kafka 수신 메시지 Debug및, 수신검증
    private void debugKafkaMsg(String key, String value, ActorRef greet, String testKey, String consumerId) {
        if (consumerId.equals("consumer1")) {
            consumeCnt1++;
        } else if (consumerId.equals("consumer2")) {
            consumeCnt2++;
        }

        System.out.printf("[%s] Kafka with Key-Value : %s-%s Count[1:%d/2:%d] %n", consumerId, key, value, consumeCnt1, consumeCnt2);

        //테스트키 동일한것만 카운트 확인..(테스트마다 Kafka고유키 사용)
        if (testKey.equals(key)) greet.tell("kafkaOK", null);

    }

    /* Test - ProduceAndConsumeToS3AreOK




     */

    @Test
    @DisplayName("ProduceAndConsumeToS3AreOK")
    public void ProduceAndConsumeToS3AreOK() {
        new TestKit(actorSystem) {
            {
                // Test System
                final ActorMaterializerSettings settings = ActorMaterializerSettings.create(actorSystem).withDispatcher("my-dispatcher-streamtest");
                final Materializer materializer = ActorMaterializer.create(settings, actorSystem);

                final TestKit probe = new TestKit(actorSystem);
                final ActorRef confirmActor = actorSystem.actorOf(MessageCofirmActor.Props(), "confirmActor");

                final String testKey = java.util.UUID.randomUUID().toString();
                final String testKafkaServer = "localhost:9092";
                final String testGroup = "group1";

                confirmActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // Test Policy
                final int testCount = 1000;
                final int maxEntityPerFile = 50;
                final String fileName = "s3testfile-";
                final int[] curFileIdx = {0};
                int expectedFileCount = 1000 / maxEntityPerFile;

                // Given Configs....

                // Avro Schema
                String schemaString = "{\"namespace\": \"com.example\", " +
                        "\"type\": \"record\", " +
                        "\"name\": \"S3TestModel\", " +
                        "\"fields\": [" +
                        "{\"name\": \"name\", \"type\": \"string\"}," +
                        "{\"name\": \"jsonValue\", \"type\": \"string\"}" +
                        "]}";


                // s3 Config
                // AWS S3 Config - test.conf 참고
                final Config config = actorSystem.settings().config().getConfig("alpakka.s3");
                S3Settings s3Settings = S3Settings.create(config);
                final String bucketName = "my-bucket";

                // JSON변환 객체 - 멀티스레드(동시성)에 안전한 객체이지만, 구성변경시 주의
                ObjectMapper mapper = new ObjectMapper();

                // Producer Config
                final Config producerConfig = actorSystem.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers(testKafkaServer);

                // ConSumer Config
                final Config conSumeConfig = actorSystem.settings().config().getConfig("akka.kafka.consumer");
                final ConsumerSettings<String, String> consumerSettings =
                        ConsumerSettings.create(conSumeConfig, new StringDeserializer(), new StringDeserializer())
                                .withBootstrapServers(testKafkaServer)
                                .withGroupId(testGroup)
                                .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                                .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000")
                                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                // Flow - Consumer
                Consumer
                        .plainSource(
                                consumerSettings,
                                Subscriptions.topics(testTopicName))
                        .grouped(maxEntityPerFile)
                        .to(Sink.foreach(group -> {

                            S3TestModel model = new S3TestModel();
                            model.jsonValue = "example data";

                            Schema schema = new Schema.Parser().parse(schemaString);

                            // Avro GenericRecord 생성
                            GenericRecord record = new GenericData.Record(schema);

                            curFileIdx[0]++;

                            String dynamicFileKey = fileName + curFileIdx[0];

                            group.forEach(msg -> {
                                try {
                                    // AnyJson을 처리할수 있는~ S3TestModel
                                    S3TestModel obj = mapper.readValue(msg.value(), S3TestModel.class);
                                    record.put("name", obj.name);
                                    record.put("jsonValue", obj.jsonValue);

                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                                debugKafkaMsg(msg.key(), msg.value(), confirmActor, testKey, "consumer1");
                            });

                            ByteString byteString = serializeGenericRecordToByteString(record);

                            Source.single(byteString)
                                    .runWith(S3.multipartUpload(bucketName, dynamicFileKey)
                                            .withAttributes(S3Attributes.settings(s3Settings)), materializer)
                                    .thenAccept(result -> {
                                        LocalTime now = LocalTime.now();
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                                        String formatedNow = now.format(formatter);
                                        System.out.println(formatedNow + " Upload complete: " + result.location());
                                        confirmActor.tell("s3UploadOK", null);
                                    })
                                    .exceptionally(throwable -> {
                                        System.err.println("Upload failed: " + throwable.getMessage());
                                        return null;
                                    });
                        }))
                        .run(actorSystem);

                // Flow - Producer
                CompletionStage<Done> done =
                        Source.range(1, testCount)
                                .map(number -> {
                                    // JSON형태의 다양한 수십소스
                                    S3TestJsonModel s3TestJsonModel = new S3TestJsonModel();
                                    s3TestJsonModel.count = number;
                                    String jsonOriginData = mapper.writeValueAsString(s3TestJsonModel);

                                    // 원본유지를 위한 정의모델
                                    S3TestModel s3TestModel = new S3TestModel();
                                    s3TestModel.jsonValue = jsonOriginData;
                                    String jsonSendData = mapper.writeValueAsString(s3TestModel);

                                    return jsonSendData;
                                })
                                .map(value -> new ProducerRecord<String, String>(testTopicName, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                Source<Done, NotUsed> source = Source.completionStage(done);

                within(
                        Duration.ofSeconds(30),
                        () -> {

                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    System.out.println("생산완료")
                            );

                            //For Waitfor Safe Test - 소비자가 셋업되기 전에, 생산이 먼저시작하는 케이스 방지!
                            expectNoMessage(Duration.ofSeconds(3));

                            // Kafka 생산시작
                            source.runWith(sink, actorSystem);

                            // Kafka 소비 메시지 확인(100)
                            for (int i = 0; i < testCount; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "kafkaOK");
                            }

                            // Upload 확인
                            for (int i = 0; i < curFileIdx[0]; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "s3UploadOK");
                            }

                            System.out.println(" Upload complete - count:" + curFileIdx[0]);
                            assertEquals(expectedFileCount, curFileIdx[0]);

                            // Download 확인
                            Source<Integer, ?> downloadSource = Source.range(0, expectedFileCount );

                            downloadSource
                                    .runForeach(index -> {
                                        String dynamicFileKey = fileName + (index + 1);

                                        // S3에서 파일 다운로드
                                        CompletionStage<Optional<Pair<Source<ByteString, NotUsed>, ObjectMetadata>>> download =
                                                S3.download(bucketName, dynamicFileKey)
                                                        .withAttributes(S3Attributes.settings(s3Settings))
                                                        .runWith(Sink.head(), materializer);

                                        download.thenAccept(optionalSourcePair -> {
                                                    optionalSourcePair.ifPresent(sourcePair -> {
                                                        LocalTime now = LocalTime.now();
                                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                                                        String formatedNow = now.format(formatter);
                                                        Source<ByteString, ?> downloadbyteSource = sourcePair.first();
                                                        downloadbyteSource.runWith(Sink.foreach(byteString -> System.out.println(formatedNow + " Downloaded: " + dynamicFileKey)), materializer);
                                                        confirmActor.tell("s3DownloadOK", null);
                                                    });
                                                })
                                                .exceptionally(throwable -> {
                                                    System.err.println("Download failed for " + dynamicFileKey + ": " + throwable.getMessage());
                                                    return null;
                                                });
                                    }, materializer);

                            // Download 확인
                            for (int i = 0; i < curFileIdx[0]; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "s3DownloadOK");
                            }

                            // 추가 이벤트가 없는지 확인후 TEST 종료 for CleanTest.
                            expectNoMessage(Duration.ofSeconds(1));

                            return null;
                        });
            }
        };
    }
}
