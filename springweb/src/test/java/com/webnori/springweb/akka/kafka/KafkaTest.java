package com.webnori.springweb.akka.kafka;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

// 
// http://localhost:8989/

/**
 * TestClass : KafkaTest
 * 목표 : Akka의 Stream을 이용해 카프카를 리액티스 스트림하게 이용
 * 참고 링크 : https://doc.akka.io/docs/alpakka-kafka/current/producer.html
 */

public class KafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTest.class);
    private static final String hello = "not another hello world";
    private static ActorSystem actorSystem;
    String testTopicName = "test-1";
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

    @Test
    @DisplayName("TestKafkaProduce")
    public void TestKafkaProduce() {
        new TestKit(actorSystem) {
            {
                //100개의 메시지생산 처리완료

                // initialization

                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // config

                final Config config = actorSystem.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(config, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers("localhost:9092");

                int topicCount = 100;

                // Producer Flow (데이터 생상)

                CompletionStage<Done> done =
                        Source.range(1, topicCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                Source<Done, NotUsed> source = Source.completionStage(done);


                // Test

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            // 생산작업이 완료되면 GreetActor에 Hello전송
                            // Hello수신시 관찰자(작업완료)에게는 world 전송
                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    greetActor.tell("hello", getRef())
                            );

                            // Kafka 생산시작
                            source.runWith(sink, actorSystem);

                            // Kafka 생산완료검사
                            probe.expectMsg(Duration.ofSeconds(5), "world");

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();

                            return null;
                        });
            }
        };
    }

    CompletionStage<String> business(String key, String value) {
        System.out.printf("business with Key-Value : %s-%s%n", key, value);
        return null;
    }

    void debugKafkaMsg(String key, String value, ActorRef greet, String testKey, String consumerId) {
        if (consumerId.equals("consumer1")) {
            consumeCnt1++;
        } else if (consumerId.equals("consumer2")) {
            consumeCnt2++;
        }

        System.out.printf("[%s] Kafka with Key-Value : %s-%s Count[1:%d/2:%d] %n", consumerId, key, value, consumeCnt1, consumeCnt2);

        //테스트키 동일한것만 카운트 확인..(테스트마다 Kafka고유키 사용)
        if (testKey.equals(key)) greet.tell("hello", null);

    }

    @Test
    @DisplayName("TestKafkaProduceAndConsume")
    public void TestKafkaProduceAndConsume() {
        new TestKit(actorSystem) {
            {
                //100개의 메시지생산을하고 100개의 메시지소비테스트 확인 - 파티션지정X

                // initialization

                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");
                final int testCount = 100;
                final String testKey = java.util.UUID.randomUUID().toString();
                final String testKafkaServer = "localhost:9092";
                final String testGroup = "group1";

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                // producerConfig

                final Config producerConfig = actorSystem.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers(testKafkaServer);

                // conSumeConfig

                final Config conSumeConfig = actorSystem.settings().config().getConfig("akka.kafka.consumer");
                final ConsumerSettings<String, String> consumerSettings =
                        ConsumerSettings.create(conSumeConfig, new StringDeserializer(), new StringDeserializer())
                                .withBootstrapServers(testKafkaServer)
                                .withGroupId(testGroup)
                                .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                                .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000")
                                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                //Consumer Flow
                Consumer
                        .plainSource(
                                consumerSettings,
                                Subscriptions.topics(testTopicName))
                        .to(Sink.foreach(msg ->
                                debugKafkaMsg(msg.key(), msg.value(), greetActor, testKey, "consumer1"))
                        )
                        .run(actorSystem);

                //Producer Flow
                CompletionStage<Done> done =
                        Source.range(1, testCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                Source<Done, NotUsed> source = Source.completionStage(done);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    System.out.println("생산완료")
                            );

                            //For Clean Test - 3초후 메시지생성
                            expectNoMessage(Duration.ofSeconds(3));

                            // Kafka 생산시작
                            source.runWith(sink, actorSystem);

                            // Kafka 소비 메시지 확인(100)
                            for (int i = 0; i < testCount; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "world");
                            }

                            return null;
                        });
            }
        };
    }

    @Test
    @DisplayName("TestKafkaProduceAndMultiConsume")
    public void TestKafkaProduceAndMultiConsume() {
        new TestKit(actorSystem) {
            {
                //고정파티션전략 : 파티정지정 생산*2/소비*2 2배생산/2배소비

                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");
                final int testCount = 100;
                final int partitionCount = 2;

                final String testKey = java.util.UUID.randomUUID().toString();
                final String testKafkaServer = "localhost:9092";
                final String testGroup = "group1";

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                final Config producerConfig = actorSystem.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers(testKafkaServer);


                final Config conSumeConfig = actorSystem.settings().config().getConfig("akka.kafka.consumer");
                final ConsumerSettings<String, String> consumerSettings =
                        ConsumerSettings.create(conSumeConfig, new StringDeserializer(), new StringDeserializer())
                                .withBootstrapServers(testKafkaServer)
                                .withGroupId(testGroup)
                                .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                                .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000")
                                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


                //Consumer Setup
                var control =
                        Consumer.plainSource(
                                        consumerSettings,
                                        Subscriptions.assignment(new TopicPartition(testTopicName, 0)))
                                .to(Sink.foreach(msg ->
                                        debugKafkaMsg(msg.key(), msg.value(), greetActor, testKey, "consumer1"))
                                )
                                .run(actorSystem);

                var control2 =
                        Consumer.plainSource(
                                        consumerSettings,
                                        Subscriptions.assignment(new TopicPartition(testTopicName, 1)))
                                .to(Sink.foreach(msg ->
                                        debugKafkaMsg(msg.key(), msg.value(), greetActor, testKey, "consumer2"))
                                )
                                .run(actorSystem);

                //Producer Setup
                CompletionStage<Done> done =
                        Source.range(1, testCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, 0, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                CompletionStage<Done> done2 =
                        Source.range(1, testCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, 1, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                //Producer Task Setup
                Source<Done, NotUsed> source = Source.completionStage(done);
                Source<Done, NotUsed> source2 = Source.completionStage(done2);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    System.out.println("생산완료")
                            );

                            //For Clean Test - 3초후 메시지생성
                            expectNoMessage(Duration.ofSeconds(3));

                            // Kafka 생산시작
                            source.runWith(sink, actorSystem);
                            source2.runWith(sink, actorSystem);

                            // Kafka 소비 메시지 확인 -
                            for (int i = 0; i < testCount * partitionCount; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "world");
                            }

                            return null;
                        });
            }
        };
    }

    @Test
    @DisplayName("TestKafkaProduceAndDynamicConsume")
    public void TestKafkaProduceAndDynamicConsume() {
        new TestKit(actorSystem) {
            {
                //소비자2시작 -> 생산(200개) -> 소비카운팅확인 -> 생산(100) -> 소비자1중지 -> 소비카운팅확인

                final TestKit probe = new TestKit(actorSystem);
                final ActorRef greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");
                final int testCount = 100;
                final int partitionCount = 2;

                final String testKey = java.util.UUID.randomUUID().toString();
                final String testKafkaServer = "localhost:9092";
                final String testGroup = "group1";

                consumeCnt1 = 0;
                consumeCnt2 = 0;

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                final Config producerConfig = actorSystem.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers(testKafkaServer);


                final Config conSumeConfig = actorSystem.settings().config().getConfig("akka.kafka.consumer");
                final ConsumerSettings<String, String> consumerSettings =
                        ConsumerSettings.create(conSumeConfig, new StringDeserializer(), new StringDeserializer())
                                .withBootstrapServers(testKafkaServer)
                                .withGroupId(testGroup)
                                .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                                .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000")
                                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                //Consumer Setup
                var consumer1 = Consumer
                        .plainSource(
                                consumerSettings,
                                Subscriptions.topics(testTopicName))
                        .to(Sink.foreach(msg ->
                                debugKafkaMsg(msg.key(), msg.value(), greetActor, testKey, "consumer1"))
                        )
                        .run(actorSystem);

                var consumer2 = Consumer
                        .plainSource(
                                consumerSettings,
                                Subscriptions.topics(testTopicName))
                        .to(Sink.foreach(msg ->
                                debugKafkaMsg(msg.key(), msg.value(), greetActor, testKey, "consumer2"))
                        )
                        .run(actorSystem);

                //Producer Setup
                CompletionStage<Done> done =
                        Source.range(1, testCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, 0, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                CompletionStage<Done> done2 =
                        Source.range(1, testCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, 1, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                CompletionStage<Done> done3 =
                        Source.range(1, testCount)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(testTopicName, 1, testKey, value))
                                .runWith(Producer.plainSink(producerSettings), actorSystem);

                //Producer Task Setup
                Source<Done, NotUsed> source = Source.completionStage(done);
                Source<Done, NotUsed> source2 = Source.completionStage(done2);
                Source<Done, NotUsed> source3 = Source.completionStage(done3);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    System.out.println("생산완료")
                            );

                            //For Clean Test - 3초후 메시지생성
                            expectNoMessage(Duration.ofSeconds(3));

                            // Kafka 생산시작
                            source.runWith(sink, actorSystem);
                            source2.runWith(sink, actorSystem);

                            // Kafka 소비 메시지 확인 -
                            for (int i = 0; i < testCount * partitionCount; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "world");
                            }

                            // 컨슈머1 중지~
                            consumer1.stop();

                            expectNoMessage(Duration.ofSeconds(3));

                            source3.runWith(sink, actorSystem);

                            // 추가 생산 소비확인
                            for (int i = 0; i < testCount; i++) {
                                probe.expectMsg(Duration.ofSeconds(5), "world");
                            }

                            return null;

                        });
            }
        };

    }

}
