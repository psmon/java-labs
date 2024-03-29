# Alpakka

Alpakka를 통해 ReactiveStream을 실험하는 테스트코드입니다.

## 사전지식

![](https://miro.medium.com/max/601/1*ElG4Tm3oHN_9BtV0QnyCEg.gif)
![](https://miro.medium.com/max/601/1*7let_JEprOxIGh2MXhcOaA.gif)

동시성과 병렬성은 약간의 차이가 있지만, 다음과 같이 간략하게 설명될수 있습니다.

동시성 : 한사람이 커피주문 받고 만들기를 동시에한다. ( 커피머신을 이용하면서도 주문을 받음)
병렬성 : 커피주문을 받는사람과 만드는 사람이 각각 따로 있다.
순차성 : 한사람이 커피주문을 받고 커피를 만들지만~ 커피가 완성될때까지 다음손님 주문을 받지 못한다.

StreamAPI를 학습하기전 각언어가 제공하는 동시성/병렬처리 방식을 알아보면 이 내용을 이해하는데 도움됩니다.
자바/닷넷 버전으로 설명을 구성하였으며 서로의 언어에서 사용방식 차이를 익히는것은 자신의 언어스펙 숙련을 높이는것에도 도움이됩니다.


- https://wiki.webnori.com/pages/viewpage.action?pageId=94241023

## Reactive Stream 특징

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
- 스트림 그래프 DSL(Domain Specific Language): Akka Streams는 선언적인 DSL을 제공하여, 스트림 처리 로직을 간결하고 명확하게 표현할 수 있게 해줍니다.

Akka Streams는 이러한 특징을 바탕으로 실시간 데이터 처리, 복잡한 이벤트 처리, 고성능 백엔드 시스템 개발 등에 널리 사용됩니다.

```                                                                                                                                                                                                                                                                                                                              
                                                flow                                                                                                           
                                                                                                                                                               
                    +----------------+        +-----------+      +----------------+                                                                            
                    |                |        |           |      |                |                                                                            
                    |                |>>>>>>>>>>          >>>>>>>>>>              |                                                                            
 +-----------+      |                |        |           |      |                |      +-----------+                                                         
 |           |      |                |        +-----------+      |                |      |           |                                                         
 |          >>>>>>>>>>               |                           |                >>>>>>>>>>         |                                                         
 |           |      |                |                           |                |      |           |                                                         
 +-----------+      |                |        +-----------+      |                |      +-----------+                                                         
    source          |                |        |           |      |                |         sink                                                               
                    |                |>>>>>>>>>>          >>>>>>>>>>              |                                                                            
                    |                |        |           |      |                |                                                                            
                    +----------------+        +-----------+      +----------------+                                                                            
                                                flow               fan-in (zip)                                                                                
                    fan-out(broadcast)      
```


Graph의 구성요소는 몇가지가 더있지만, 대표적으로 다음요소를 가지고 있습니다.
- 소스(Source): 스트림의 데이터를 제공하는 시작점입니다. 예를 들어, 파일, 컬렉션, 외부 시스템 등에서 데이터를 읽을 수 있습니다.
- 플로우(Flow): 소스와 싱크 사이에서 데이터를 변환하는 중간 처리 단계입니다. 예를 들어, 데이터 필터링, 변환, 집계 등을 수행할 수 있습니다.
- 싱크(Sink): 스트림의 데이터를 소비하는 종점입니다. 데이터를 파일에 쓰거나, 데이터베이스에 저장하거나, 단순히 버리는 등의 작업을 수행할 수 있습니다.
- 분배기 요소
    - Fan-out은 단일 데이터 소스에서 여러 출력 스트림으로 데이터를 분배하는 패턴입니다. 이는 하나의 입력 스트림이 여러 병렬 처리 경로로 나뉘어지는 경우에 사용됩니다. 예를 들어, 하나의 소스 스트림이 여러 Flow 요소로 분산되어 각기 다른 연산을 수행할 수 있습니다.
    - 반대로, fan-in은 여러 입력 스트림을 하나의 출력 스트림으로 결합하는 패턴입니다. 이는 여러 병렬 처리 스트림의 결과를 하나의 통합된 스트림으로 합치는 데 사용됩니다.

### RunableGraph

요소를 구성하고 흐름을 DSL을 이용 레고 조립하듯이 우리가 원하는 플로우를 구성할수 있습니다.
실행가능한 완전한 요소를 RunableGraph라고 하며

이 샘플은 via를 통해 한쪽방향으로 흐르지만 TPS조절및 BackPresure등 유용한 기능을 이용할수 있습니다.

```
                             RunableGraph                                                                                       
                                                                                                                                
      +--------+   +--------+  +--------+  +--------+  +--------+                                                               
      |        |+-+|        +-+|        +-+|        ++-+        |                                                               
      | Source |+-+| Buffer +-+| TPS    +-+| Flow   ++-+ Sink   |                                                               
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

BackPresure의 아이디어는 간단합니다. Buffer가 밀리면 소비(Sink)가 느려지는것임으로 생산속도를 조절합니다.

```
// Buffer 설정 및 OverflowStrategy.backpressure 적용
int bufferSize = 100000;
Flow<Integer, Integer, NotUsed> backpressureFlow = 
 Flow.<Integer>create().buffer(bufferSize, OverflowStrategy.backpressure());
```

### Throttle

TPS를 제어하기 위해 처리값을 계산하고 블락함수(sleep)로 지연시키는 방법은 성능에 있어서 멀티스레드이던 아니던 성능 파멸의 시나리오로 향합니다.
특히 단일스레드만 이용해 동시성처리능력을 극대화하는 플랫폼들은 전체 중지가 될수 있습니다.

BackPresure를 이용하여 자동조절장치를 도입하는것도 유용하지만~ AKKAStream에서는 TPS속도를 제어하는 장치자체를 제공하여 Stream에 연결할수 있습니다.

```
throttle(processCouuntPerSec, Duration.ofSeconds(1))
```

### Flow

Flow에서 스트림과정중 필터처리및 데이터가공이 필요한부분을 구현할수 있는 실제 코드 작성부분으로
자바에서 제공하는 기본 비동기 처리방식을 이용하여 연결할수 있습니다.

StreamAPI를 이용하기전에 잠깐~ 항상 기본언어가 제공하는 동시성 비동기처리 함수를 먼저 학습하는것을 권장하며
이것을 건너띄고 학습하게되는경우 문제해결의 공간이 한정적이게 되는 부작용이 발생합니다.
기본 제공스펙에서 해결할수 있는일이 대부분이며 이경우 AKKA와 같은 추상화 객체가 필요없을수도 있습니다.
추가로 JAVA StreamAPI에서도 유사한 기능을 제공하기때문에 함께 학습하면서 비교하는것도 권장됩니다.

Flow를 단일/병렬 처리를 채택할수 있으며~ 유입된 스트림의 분기가 필요할시 FanIn/Out 달수도 있습니다.

단일흐름요소에서 병렬(멀티스레드이용)하는 방법을 알아보겠습니다.
Stream 처리과정중 Blocking코드가 존재하거나 응답이 느린경우 Stream의 전체 처리가 늦어질수 있습니다.
동시성을 지원하는 일반적인 프레임워크를 이용하는경우 블락킹코드가 하나라도 있으면 전체가 멈출수 있음으로
프레임이 제공하는 비동기처리만을 이용해야하는 경우도 있지만 AkkaStream은 JAVA의 비동기 병렬작동가능한 CompletionStage를 지원해서 병렬처리로 흘려보낼수 있습니다.

Executor에서 병렬처리 스레드수를 지정하고, Stream에서는 동시실행가능한 수를 지정할수 있습니다.
이러한 병렬처리 전략으로 지연이 높은 블락킹처리에 대해 멀티스레드 프로그래밍을 하지 않지만 그것을 활용할수 있습니다.

```
final int parallelism = 15;
Flow<Integer, String, NotUsed> parallelFlow = 
  Flow.<Integer>create().mapAsync(parallelism, callApiAsync);
  
Flow<Integer, String, NotUsed> sigleflow = 
  Flow.fromFunction(BackPressureTest::callApi);  
   
// 자바의 비동기처리방식 - CompletionStage
// 이 방식을 이용하는 경우 기본 스레드 이용값은 15입니다. 더 많은 스레드가 필요하면 이값을 높여주세요
// 스레드는 공짜가아니며 이 값이 단순하게 높다고 처리성능이 향상되는것은 아닙니다.  
private static final Executor executor = Executors.newFixedThreadPool(30); 



// 자바의 경우 비동기처리로 CompletionStage 를 채택했으며
// 여러진영에서 채택한 비동기를 조금더 심플하고 직관적으로 처리하는 async/await 아닌점은 조금 아쉽지만~
// 다음 함수는 CompletionStage를 이용해 async에 대응합니다.

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

// 자바의 일반적인 동기처리방식
private static String callApi(Integer param) {    
    try {
        Thread.sleep(100); // API 응답 시간을 시뮬레이션하기 위한 지연        
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "Response for " + param;
}
    
```

### Stream을 Actor에 연결

입력되는 값이, 배열과 같은 고정된 값이 아닌 액터를 연결 시킴으로
불특정하게 발생하는 이벤트에 대한 Flow제어를 할수 있으며 액터는 원격/클러스터로 확장가능할수 있으며
이것은 분산환경에서 실시간성 StreamData를 액터모델을 통해 다룰수 있게합니다.

```
final ActorRef throttler = Source.actorRef(bufferSize, OverflowStrategy.dropNew())
        .throttle(processCouuntPerSec, FiniteDuration.create(1, TimeUnit.SECONDS),
                processCouuntPerSec, (ThrottleMode) ThrottleMode.shaping())
        .to(Sink.actorRef(slowConsumerActor, akka.NotUsed.getInstance())).run(materializer);

for (int i = 0; i < testCount; i++) {
    throttler.tell("hello", probe.getRef());
}
```


## 참고링크
- https://blog.rockthejvm.com/akka-streams-graphs/ - Akka Streams Graphs
- https://doc.akka.io/docs/alpakka/current/index.html - AkkaStream을 ReactiveStream을 준수하는 모든 Stack에 연결하는 Akka 서브프로젝트
- https://doc.akka.io/docs/akka/current/testing.html - Akka Test Tool Kit
- https://wiki.webnori.com/display/AKKA/Terminology - 동시성 처리와 병렬처리의 차이점
- https://wiki.webnori.com/pages/viewpage.action?pageId=94240901 - 분산처리를 다루는 개발자가 AKKA를 학습하면 도움되는 이유 (도입을 하지 않더라도~)
- https://wiki.webnori.com/pages/viewpage.action?pageId=94240903 - 빠른 생산자와 느린소비자 - API호출편
- https://wiki.webnori.com/pages/viewpage.action?pageId=94241023 - 기본언어가 지원하는 동시성처리 기능 (기본기는 항상 중요합니다.)

## Alpakka 
- https://doc.akka.io/docs/alpakka/current/s3.html
- https://doc.akka.io/docs/alpakka-kafka/current/home.html