# AKKA(JAVA) Unit TEST

## Actor UnitTest 컨셉

전통적 유닛테스트기에서는 함수호출의 결과값을 기다려야하는 동기적 검사만 가능하지만
결과처리를 특정 테스트 관찰자(Probe)에게 전달하고 큐를 검사함으로
비동기로 작동되는 액터의 값을 검사하기위해 멈출(블락킹)필요가 없습니다.
    
    greetActor.tell("hello", getRef());
    probe.expectMsg(Duration.ofSeconds(100), "world");

## Dispacher

하나의 액터는 순차성을 보장합니다. 순차성이 아닌 병렬동시처리가 필요할시 
멀티스레드 프로그래밍을 할 필요는 없지만
풀을 구성하고 스레드 옵션을 줄수가 있습니다.

    my-dispatcher-test1 { 
        type = Dispatcher 
        executor = "fork-join-executor" 
        fork-join-executor { 
            parallelism-min = 2 
            parallelism-factor = 2.0 
            parallelism-max = 50
        }
        throughput = 5
    }

    final ActorRef greetActor = system.actorOf(new RoundRobinPool(poolCount).props(HelloWorld.Props()
            .withDispatcher("my-dispatcher-test1")), "router2");

## Throttler Actor

메시지 처리속도를 늦출필요가 있을때 AkkaStream API의 throttle 을 이용할수 있습니다.
속제제어기를 기존 액터와 연결하여 액터에게 보낼 메시지 처리량을 조절합니다.

    int processCouuntPerSec = 3; 

    final ActorRef throttler1 =
            Source.actorRef(1000, OverflowStrategy.dropNew())
                    .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                            processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
                    .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
                    .run(materializer);

    int testCount = 50;
    for (int i = 0; i < testCount; i++) {
        throttler1.tell("hello1", getRef()); 
    }

## Kafka

Alpakka를 이용하여 ReactiveStream(AkkaStream)하게 Kafka에 발생하는 데이터의 흐름을 제어할수 있으며

Kafka가 제공하는 기능을 이용하여 다양한 전략에 맞게 사용할수 있는 Consumer 유틸을 제공합니다.

- plainSource
- plainExternalSource
- committableSource
- committableExternalSource
- commitWithMetadataSource
- sourceWithOffsetContext
- atMostOnceSource
- plainPartitionedSource
- plainPartitionedManualOffsetSource
- committablePartitionedSource
- committablePartitionedManualOffsetSource
- commitWithMetadataPartitionedSource

Trasaction이 지원되는 Consumers
- Transactional.source
- Transactional.sourceWithOffsetContext


[MoreInfo](https://doc.akka.io/docs/alpakka-kafka/current/consumer.html)


유닛테스트를 통해  Kafka의 생상-소비 이벤트스트림에 발생한 데이터를 검사할수있는것은 보너스입니다. 

```
#AkkaKaftaTest.java

final ConsumerSettings<String, String> consumerSettings =
        ConsumerSettings.create(conSumeConfig, new StringDeserializer(), new StringDeserializer())
                .withBootstrapServers(testKafkaServer)
                .withGroupId(testGroup)
                .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000")
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");                

var consumer1 = Consumer.plainSource(
                consumerSettings,
                Subscriptions.assignment(new TopicPartition(topic, 0)))
        .to(Sink.foreach(msg ->
                debugKafkaMsg(msg.key(), msg.value(), greetActor, testKey, "consumer1"))
        )
        .run(system);

.............        
        
// Kafka 생산시작
source.runWith(sink, system);
source2.runWith(sink, system);

// Kafka 소비 메시지 확인 -
for (int i = 0; i < testCount * partitionCount; i++) {
    probe.expectMsg(Duration.ofSeconds(5), "world");
}        
```

## 테스트 코드

실제 동작하는 테스트코드를 참고하여, AKKA의 기능을 이해할수 있습니다.

- [AkkaBasicTests.java](BasicTests.java) : Akka 기본 메시지 전송
- [AkkaDisPatcherTests.java](DisPatcherTests.java) : Dispatcher를 이용한 동시성처리 ( 멀티스레드 대응 )
- [AkkaThrottleTests.java](ThrottleTests.java) : 메시지 처리 속도제어 ( API호출제약및 생산속도 조절시 사용 )
- [AkkaKafkaTests.java](KafkaTests.java) : Akka(+Alpakka)를 이용한 Kafka 활용 유닛테스트기