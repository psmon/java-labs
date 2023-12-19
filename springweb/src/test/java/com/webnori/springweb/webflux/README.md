# Reactive Streams

Reactive Streams은 비동기 데이터 스트림 처리를 위한 표준이며, 주요 장점들을 다음과 같이 요약할 수 있습니다:

- 비동기 처리 및 병렬성: Reactive Streams는 비동기 처리를 통해 시스템의 병렬성과 성능을 향상시킵니다. 이는 데이터 처리와 이벤트 핸들링을 더 효율적으로 만들어 줍니다.
- 백프레셔(Backpressure) 관리: 이 기능은 데이터 소스가 너무 빠르게 데이터를 생산할 때, 데이터 소비자가 처리할 수 있는 속도를 초과하지 않도록 관리합니다. 이로 인해 메모리 오버플로우나 데이터 손실의 위험을 줄일 수 있습니다.
- 확장성: Reactive Streams는 확장 가능한 아키텍처를 제공합니다. 대규모 데이터 스트림과 고성능 애플리케이션에 적합하며, 시스템 리소스를 효율적으로 활용합니다.
- 결합성(Loose Coupling): 데이터 생산자와 소비자 간의 결합도를 낮추어, 유지보수와 시스템의 변경이 용이하게 만들어 줍니다.
- 오류 처리: Reactive Streams는 데이터 스트림 중 발생할 수 있는 오류를 효과적으로 처리할 수 있는 메커니즘을 제공합니다. 이를 통해 안정적인 시스템 운영이 가능해집니다.

이러한 장점들로 인해 Reactive Streams는 대용량 데이터 처리, 실시간 스트리밍, 고성능 웹 애플리케이션 개발 등 다양한 분야에서 활용되고 있습니다.

## AkkaStreams

여기서 AkkaStreams의 특징만 정리하면 다음과 같이 요약할 수 있습니다.

- Akka Streams는 리액티브 스트림스 표준을 준수합니다. 이는 데이터 스트림을 비동기적으로 처리하면서 백프레셔(backpressure)를 관리하여 시스템이 과부하되는 것을 방지합니다.
- 스트림 그래프 DSL: Akka Streams는 선언적인 DSL을 제공하여, 스트림 처리 로직을 간결하고 명확하게 표현할 수 있게 해줍니다.

Akka Streams는 이러한 특징을 바탕으로 실시간 데이터 처리, 복잡한 이벤트 처리, 고성능 백엔드 시스템 개발 등에 널리 사용됩니다.


### RunableGraph

AkkaStream에서 실행되는 Flow들은 DSL을 이용하여 레고를 조립하듯이 구성할수 있으며 실행가능한 완전한 요소를 RunableGraph라고 합니다.

```
                             RunableGraph                                                                                       
                                                                                                                                
      +--------+   +--------+  +--------+  +--------+  +--------+                                                               
      |        |+-+|        +-+|        +-+|        ++-+        |                                                               
      |        |+-+|        +-+|        +-+|        ++-+        |                                                               
      |        |   |        |  |        |  |        |  |        |                                                               
      +--------+   +--------+  +--------+  +--------+  +--------+                                                               
       Source -> BackPresure -> Throttle -> ParalleFlow -> Sink                
```

```
Source<Integer, NotUsed> source = Source.range(1, 100);
source
    .via(backpressureFlow)
    .throttle(processCouuntPerSec, Duration.ofSeconds(1))
    .via(parallelFlow)
    .to(sink)
    .run(materializer);
```

위 코드 샘플은, 입력되는 값이 배열의 요소이며 via를 통해 연결해 우리가 계획한 형태로 stream을 흘려보낼수 있습니다.

- BackPresure : 소비속도를 고려 생산속도를 제어할수 있습니다.
- Throttle : Stream 데이터를 TPS로 제어할수 있습니다.
- ParalleFlow : 멀티스레드를 이용해 병렬처리를 할수 있습니다.
- Sink : 최종 처리된 결과를 비동기적으로 Element단위로 흘려보냅니다. 

각각의 코드 구성이 어떻게 되었는지 살펴보겠습니다.

### BackPresure
```
// Buffer 설정 및 OverflowStrategy.backpressure 적용
int bufferSize = 100000;
Flow<Integer, Integer, NotUsed> backpressureFlow = 
 Flow.<Integer>create().buffer(bufferSize, OverflowStrategy.backpressure());
```

### Throttle
```
throttle(processCouuntPerSec, Duration.ofSeconds(1))
```

### ParalleFlow

Stream 처리과정중 Blocking코드가 존재하거나 응답이 느린경우 Stream의 전체 처리가 늦어질수 있습니다. 
동시성을 지원하는 일반적인 프레임워크를 이용하는경우 블락킹코드가 하나라도 있으면 전체가 멈출수 있음으로 
프레임이 제공하는 비동기처리만을 이용해야하는 경우도 있지만 AkkaStream은 JAVA의 비동기 병렬작동가능한 CompletionStage를 지원해서 병렬처리로 흘려보낼수 있습니다.

Executor에서 병렬처리 스레드수를 지정하고, Stream에서는 동시실행가능한 수를 지정할수 있습니다.
이러한 병렬처리 전략으로 지연이 높은 블락킹처리에 대해 멀티스레드 프로그래밍을 하지 않지만 그것을 활용할수 있습니다.

```
private static final Executor executor = Executors.newFixedThreadPool(30);
private static CompletionStage<String> callApiAsync(Integer param) {
    // CompletableFuture를 사용하여 비동기 처리 구현
    return CompletableFuture.supplyAsync(() -> {
        try {
            double dValue = Math.random();
            int iValue = (int) (dValue * 1000);
            Thread.sleep(iValue); // 블락킹을 시뮬레이션하기 위한 지연코드            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Response for " + param;
    }, executor);
}
    
final int parallelism = 15;
Flow<Integer, String, NotUsed> parallelFlow = 
  Flow.<Integer>create().mapAsync(parallelism, callApiAsync);
```

### Stream을 Actor에 연결

입력되는 값이, 배열과같이 고정된 값이 아닌 액터를 연결 시킴으로 
불특정하게 발생하는 이벤트에 대한 Flow제어를 할수 있으며 액터는 원격/클러스터로 확장가능할수 있으며 
이것은 분산환경에서 Stream처리를 다룰수 있게합니다.


```
final ActorRef throttler = Source.actorRef(bufferSize, OverflowStrategy.dropNew())
        .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
        .to(Sink.actorRef(slowConsumerActor, akka.NotUsed.getInstance())).run(materializer);

for (int i = 0; i < testCount; i++) {
    throttler.tell("hello", probe.getRef());
}
```

## Webflux

WebFlux는 Spring Framework 5에서 도입된 리액티브 프로그래밍을 위한 웹 프레임워크입니다. 
이는 전통적인 Spring MVC와 다르게 비동기 및 논블로킹 방식을 지원하여, 
리액티브 스트림스(Reactive Streams) 기반으로 높은 처리량과 효율적인 리소스 사용을 가능하게 합니다.


### Reactive Strems 표준활동

- 배경: Reactive Streams는 비동기 데이터 스트림 처리에 대한 수요가 증가함에 따라 개발되었습니다. 대용량 데이터 처리, 고성능 웹 애플리케이션, 실시간 데이터 스트리밍 등의 분야에서 필요성이 대두되었습니다.
- 개발: 이 표준은 2013년경 Lightbend (당시 Typesafe), Netflix, Pivotal 등 여러 회사와 개발자들이 협력하여 만들었습니다.
- 목적: Reactive Streams의 주된 목적은 대규모 분산 시스템에서의 백프레셔(back-pressure)를 관리하고, 데이터 스트림의 비동기 처리를 표준화하는 것입니다.

Webflux 역시 ReactiveStrems을 준수하고 있으며 AKKA Stream과 연결이 가능합니다. 
이러한 프레임워크가 단순하게 동시성처리의 목적만을 가진것이 아닌 ReactiveStreams을 준수하는 오픈스택들과 
그 목적과 구현체가 달라도 표준적인 방법으로 연결이 가능하다란것입니다.

AkkaStream과 Webplux가 어떻게 연결이 되고 작동되는지 살펴보겠습니다.

###

도메인영역을 다루는 메인스트림은 단일요소로 활용하는 것이 권장되지만 
아래와같이 데이터가 AkkaStream에서 출발하여 Webplux를 지나 Actor모델로 도달할수 있습니다.
이것은 Akka와 Webplux가 상호 연동하려고 준비가 안되어도 ReactiveStream을 준수하였기때문에 가능한 시나리오입니다.

```
                                                                                                                                
                 +--------+   +--------+   +--------+   +--------+                                                              
                 |        |+-+|        +-+-+        |+-+|        |                                                              
                 |        |+-+|        +-+-+        |+-+|        |                                                              
                 |        |   |        |   |        |   |        |                                                              
                 +--------+   +--------+   +--------+   +--------+                                                              
                                                                                                                                
          Sink(AkkaStream) -> Publisher(Reactive) -> Subscribe(Flux) -> Actor 
```

다음 코드는 

- AkkaStream의 Backpressure/Throttle를 통과하고 Reactive Stream의 표준인 Publisher로 흘려보냅니다.
- Webplux는 Publish를 수신받아 Subscribe를 처리합니다.
- SubScribe에서는 유닛테스트내에서 TPS측정과 수신검증이 가능한 Actor모델로 흘려보냅니다.

```
// AkkaStream
Publisher<ConfirmEvent> publisher = source
        .via(initialDelayFlow)
        .via(backpressureFlow)
        .throttle(processCouuntPerSec, Duration.ofSeconds(1))
        .via(convertFlow)
        .runWith(Sink.asPublisher(AsPublisher.WITH_FANOUT), materializer);

// Webplux
Flux<ConfirmEvent> flux = Flux.from(publisher);

flux.subscribe(event -> subScribeActor.tell(event, ActorRef.noSender()));

// StepVerifier Junit에서 Webflux검증을 지원합니다.
StepVerifier.create(flux)
        .expectNextCount(testCount)
        .verifyComplete(); // 스트림이 정상적으로 완료되는지 확인;

// 수신검증 by AkkaTest 관찰자
for (int i = 0; i < testCount; i++) {
    probe.expectMsgClass(ConfirmEvent.class);
}
```

여기서 설명된 내용은 작동가능한 코드로 커밋이 되었으며 추가로 유닛테스트를 통해 TPS측정및 유닛테스트화가 가능합니다.

StreamAPI와 같이 동시성/병렬처리를 다루는 선언형 프로그래밍을 다루는경우~ 유닛테스트를 작성하는것은

올바른 작동방식과 다양한 응용방법을 발굴할수 있는 학습방식으로 권장됩니다.

link : https://github.com/psmon/java-labs/blob/master/springweb/src/test/java/com/webnori/springweb/webflux/BasicGuideTest.java



## 참고링크
- https://doc.akka.io/docs/alpakka/current/index.html - AkkaStream을 ReactiveStream을 준수하는 모든 Stack에 연결하는 Akka 서브프로젝트
- https://wiki.webnori.com/pages/viewpage.action?pageId=94240901 - 분산처리를 다루는 개발자가 AKKA를 학습하면 도움되는 이유 (도입을 하지 않더라도~)
