package com.webnori.springweb.akka;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.webnori.springweb.example.akka.AkkaManager;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

// https://doc.akka.io/docs/alpakka-kafka/current/producer.html
public class AkkaKafkaTests extends AbstractJavaTest {


    @Test
    @DisplayName("KafkaProduce - 100개의 메시지생산이 처리완료")
    public void TestIt() {
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

                            Sink<Done, CompletionStage<Done>> sink = Sink.foreach(i ->
                                    greetActor.tell("hello", getRef())
                            );
                            source.runWith(sink, system); // 10

                            // check that the probe we injected earlier got the msg
                            probe.expectMsg(Duration.ofSeconds(5), "world");

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }

}
