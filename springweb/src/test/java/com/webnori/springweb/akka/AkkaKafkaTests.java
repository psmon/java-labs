package com.webnori.springweb.akka;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.kafka.*;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.webnori.springweb.example.akka.AkkaManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;

// https://doc.akka.io/docs/alpakka-kafka/current/producer.html
// http://localhost:8989/

public class AkkaKafkaTests extends AbstractJavaTest {


    @Test
    @DisplayName("KafkaProduce - 100개의 메시지생산이 처리완료")
    public void TestKafkaProduce() {
        new TestKit(system) {
            {
                final TestKit probe = new TestKit(system);
                final ActorRef greetActor = AkkaManager.getInstance().getGreetActor();

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                final Config config = system.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(config, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers("localhost:9092");

                String topic = "test-1";

                CompletionStage<Done> done =
                        Source.range(1, 100)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(topic, value))
                                .runWith(Producer.plainSink(producerSettings), system);

                Source<Done, NotUsed> source = Source.completionStage(done);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            // 작업이 완료되면 GreetActor에 Hello전송
                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    greetActor.tell("hello", getRef())
                            );

                            // Kafka 생산시작
                            source.runWith(sink, system);

                            // Kafka 생산 완료감지
                            probe.expectMsg(Duration.ofSeconds(5), "world");

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();

                            return null;
                        });
            }
        };
    }

    CompletionStage<String> business(String key, String value)
    {
        System.out.println( String.format("business with Key-Value : %s-%s",key,value ));
        return null;
    }

    @Test
    @DisplayName("KafkaProduce - 100개의 메시지생산을하고 100개의 메시지소비테스트 확인")
    public void TestKafkaProduceAndConsume() {
        new TestKit(system) {
            {
                final TestKit probe = new TestKit(system);
                final ActorRef greetActor = AkkaManager.getInstance().getGreetActor();

                greetActor.tell(probe.getRef(), getRef());
                expectMsg(Duration.ofSeconds(1), "done");

                final Config producerConfig = system.settings().config().getConfig("akka.kafka.producer");
                final ProducerSettings<String, String> producerSettings =
                        ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                                .withBootstrapServers("localhost:9092");


                final Config conSumeConfig = system.settings().config().getConfig("akka.kafka.consumer");
                final ConsumerSettings<String, String> consumerSettings =
                        ConsumerSettings.create(conSumeConfig, new StringDeserializer(), new StringDeserializer())
                                .withBootstrapServers("localhost:9092")
                                .withGroupId("group1")
                                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                String topic = "test-1";


                //Consumer
                final Config commitConfig = system.settings().config().getConfig("akka.kafka.committer");
                CommitterSettings committerSettings = CommitterSettings.create(commitConfig);

                Consumer.DrainingControl<Done> control =
                        Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
                                .mapAsync(
                                        1,
                                        msg ->
                                                business(msg.record().key(), msg.record().value())
                                                        .<ConsumerMessage.Committable>thenApply(done -> msg.committableOffset()))
                                .toMat(Committer.sink(committerSettings), Consumer::createDrainingControl)
                                .run(system);


                //Producer
                CompletionStage<Done> done =
                        Source.range(1, 100)
                                .map(number -> number.toString())
                                .map(value -> new ProducerRecord<String, String>(topic,"test", value))
                                .runWith(Producer.plainSink(producerSettings), system);

                Source<Done, NotUsed> source = Source.completionStage(done);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage(Duration.ofSeconds(3));

                            // 작업이 완료되면 GreetActor에 Hello전송
                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    greetActor.tell("hello", getRef())
                            );

                            // Kafka 생산시작
                            source.runWith(sink, system);

                            // Kafka 생산 완료감지
                            probe.expectMsg(Duration.ofSeconds(5), "world");

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage(Duration.ofSeconds(5));

                            return null;
                        });
            }
        };
    }

}
