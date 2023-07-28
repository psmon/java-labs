# AKKA(JAVA) Unit TEST

## Akka TestToolKit 컨셉

![AkkaUnitTest](../../../../../../../doc/akkatest.png)

전통적 유닛테스트에서는 함수호출의 결과값을 기다려야하는 동기적 검사 위주로 작성되지만
이벤트 메시징 큐기반으로 작성된 모듈기반에서는 이러한 테스트 방식을 채택한다고 하면
작동중인 코드를 중단한후 검사해야하지만

액터의 특성을 이용 관찰자를 연결하여 관찰자의 메시지 검사를 통해 서비스 액터의 블락킹없이 유닛 테스트를 수행할수 있습니다.

    # hello 이벤트를 받으면, world를 반환하는 액터의 유닛검사 방법
    greetActor.tell("hello", getRef());
    probe.expectMsg(Duration.ofSeconds(100), "world");

## Dispacher

![dispacher](../../../../../../../doc/dispacher.png)

하나의 액터는 순차성을 보장합니다. 순차성이 아닌 병렬동시처리가 필요할시  멀티스레드 프로그래밍을 할 필요는 없지만
풀을 구성하고 스레드 옵션을 줄수가 있습니다. 이러한 액터의 실행계획은 Dispatcher가 관리하게됩니다.


코드로도 정의 가능하지만 이러한 튜닝옵션을 AKKA작동 환경파일을 통해 코딩없이 실행계획 작동방식을 조정할수 있습니다.


    #test.conf 파일에 정의 되었습니다.
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

    # 코드 이용부분에서 실행
    final ActorRef greetActor = system.actorOf(new RoundRobinPool(poolCount).props(HelloWorld.Props()
            .withDispatcher("my-dispatcher-test1")), "router2");

## Throttler Actor

![stream](../../../../../../../doc/stream.png)
-그림 : 속도제어기를 이해하기위한 실세계 존재하는 유체흐름 제어장치

메시지 처리속도를 제어할 필요가 있을때 AkkaStream에서 제공하는 기능중 일부인 throttle장치를 이용할수 있습니다.
속도 제어기를 기존 액터와 연결하여 액터에게 보낼 메시지 처리량을 조절할수가 있으며 서비스 코드는 성능관심사를 분리할수 있습니다.

> :warning: 서비스작동 코드 내에서 **Sleep** 을 사용하여 속도를 조절하는 방법은 분산환경포함 단일구동 환경에서도 전체성능을 떨어트릴수 있습니다.  

물의 흐름(strem)은 데이터의 흐름과도 유사하며 실시간성 이벤트를 처리에서 표현하는 IT용어도 Stream이라고 표현하며
안정적인 흐름 수압조절 장치에 사용하는 감압밸브와 같은 배압장치 설계를 할수도 있습니다.


Reactive Stream에서의 Backpressure도 스트림처리에서 생산과 소비의 속도가 다르기때문에 이러한 장치가 실세계에 존재하는 유체(물,기름)의 흐름을 제어하는 실장치와 닮아있으며
그림에서 표현되는 압력측정기의 경우 시스템에서는 트래픽모니터링이 있어야함을 의미하고 조절기(throttle)를 이용하여 흐름의 속도를 컨트롤할수가 있습니다.

    # 초당하나를 처리하고, 1000개의 버퍼가 쌓였을때는 드롭을 하는 조절기 샘플
    int processCouuntPerSec = 3; 

    # throttler1 -> greetActor : 조절기를 앞단에 달아서 처리량을 조절할수 있습니다.
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

## Alpakka with Kafka

![stream](../../../../../../../doc/alpakka.png)

Alpakka는 Kafka를 포함 리액티브 스트림의 인터페이스를 준수하여 다양한 외부 스택과 AkkaStream에 연결할수 있는
Akka에 파생된 서브툴킷으로 Reactive Stream을 준수합니다.

Alpakka-Kafka는 Kafka가 제공하는 다양한 메시지 고성능 전송성전략을 Akka에서 이용할수 있게 다양한 Producer/Consumer유틸을 제공합니다.


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

유닛테스트를 통해  Kafka의 생상-소비를 포함 Kafka가 제공하는 파티션 분산메시징을 이용하고 검사할수 있습니다.

KAFKA와 같은 외부 스트림장치를 이용할때 메시지 전송이 보장이 될것이다란 믿음이 아닌, 메시지 전송보장에 가까운 설계를 위해 Kafka를 이용하는 다양한 전략을 가지는 테스트 코드를 작성할수 있습니다.  


```
# 카프카 UnitTest

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

## 작성된 코드 샘플

유닛테스트를 통해 코드작동을 이해할수 있으며 실제 작동되는 유넷테스트 코드 수행을통해 AKKA에서 제공되는 기능을 학습하고 실험할수 있습니다.

- [AbstractJavaTest.java](AbstractJavaTest.java]) : 우하한 종료(GraceFulDown)를 지원하는 Base추상Test객체 
- [AkkaBasicTests.java](BasicTest.java) : Akka 기본 메시지 전송
- [AkkaDisPatcherTests.java](DisPatcherTest.java) : Dispatcher를 이용한 동시성처리 ( 멀티스레드 대응 )
- [AkkaThrottleTests.java](ThrottleTest.java) : 메시지 처리 속도제어 ( API호출제약및 생산속도 조절시 사용 )
- [AkkaKafkaTests.java](KafkaTest.java) : Akka(+Alpakka)를 이용한 Kafka 활용 유닛테스트기
- [Reliable Message Delivery](https://getakka.net/articles/actors/reliable-delivery.html) : 메시지 전송을 보장하기위한 전략( at motst once)

## 참고링크

JAVA/.NET 동일한 컨셉으로 이용할수 있습니다.  

- [AkkaUnitTest-JAVA](https://doc.akka.io/docs/akka/current/testing.html)
- [AkkaUnitTest-NET](https://getakka.net/articles/actors/testing-actor-systems.html)
- [AkkaStream-Backpressure](https://blog.rockthejvm.com/akka-streams-backpressure/)
- [Alpakka](https://doc.akka.io/docs/alpakka/current/index.html)
- [AKKA 소식을 다루는 FaceBook(Kr) 채널](https://www.facebook.com/groups/akkalabs)
