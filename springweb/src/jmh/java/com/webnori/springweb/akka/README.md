# AKKA(JAVA) Microbenchmark
 
유닛테스트의 경우 로직을 검증하고 의미있는 검증 커버리지를 높이는것에 의미가 있다고하면
성능테스트의 경우 작성한 로직또는 이용하고 있는 로직의 성능이 충분한가? 가장 작은단위의 성능을 측정하고 개선시도할수 있습니다.


## 프로젝트 구성

유닛테스트의 경우 의미 있는 코드검증의 커버리지를 높이는것에 의미가 있다고하면
성능(BenchMark)측정의 경우 우리가 작성한 코드 또는 이용하는코드의 로직이 성능에 문제 없는가? 의 관점에서 작성이됩니다.

다음과 같이 성능측정을 위한 코드관리 레이어가 분리됩니다.

- src
  - main : 서비스 작동코드 
  - test : 유닛테스트 코드
  - jmh : 성능측정코드

## Akka TestKit

Akka의 기본 테스트 툴킷에서는 아래와같이  10초이내에 1000개의 메시지가 유실없이 모두 수신되어야한다란 
동기테스트 방식이 아닌 비동기적 메시지 수신테스트를 작성하고 이용할수 있습니다.

```
new TestKit(actorSystem) {
{
    
    within(
    Duration.ofSeconds(10),
    () -> {

        int testCount = 1000;

        for (int i = 0; i < testCount; i++) {
            greetActor.tell("hello", getRef());
        }

        for (int i = 0; i < testCount; i++) {            
            probe.expectMsg(Duration.ofSeconds(1), "world");
        }        
        // Will wait for the rest of the 3 seconds
        expectNoMessage();
        return null;
    });
}};
```

AKKA의 액터모델을 전면채택하는경우 APM시스템과 연동되어 디테일한 성능측정을 할수 있습니다.
여기서는 액터모델을 전면채택하지 않더라도 이벤트처리를 채택한 로직에서
로컬에서 마이크로한 벤치마크를 시도할수 있는 방법을 정리하고 소개합니다. 

기본 검증 유닛테스트및 통합적인 APM기반 측정
- https://github.com/psmon/java-labs/blob/master/springweb/src/test/java/com/webnori/springweb/akka/README.md
- https://www.datadoghq.com/blog/engineering/how-we-optimized-our-akka-application-using-datadogs-continuous-profiler/

## BenchMark

벤치마크의 개념은 약간 다릅니다. 이것이 수십회 또는 수백회 작동했을때 최소/평균/최대를 측정하고
초당처리능력이 아무리 높아도 1분만에 메모리가 풀이나는 로직이면 성능이 좋다라고 할수 없습니다.
지속적으로 작동할수 있는가? 도 중요한 성능측정의 요소이며~ GC측정툴도 포함되어 있습니다.
개선을 시도했을때 측정이되어야지 이것이 개선되었는지 아닌지를 알수 있으며

유닛테스트 탐색기와 결합함으로 지속적으로 로컬에서 작동시켜볼수 있습니다.
시스템이 통합되고 나서 측정하는방식이 아닌, 코드작성중 성능튜닝을 지속적으로 할수있는것에 의미가 있습니다.

다음은 유닛테스트 기반으로 성능측정 리포팅된 예시입니다. 

1088000번의 메시지 수신검사가 수행되었으며
처리단위를 1000개씩하였기때문에  1000개 수신검사에 평균 0.032초내에 수행되었음을 의미합니다.
측정이되는 기준 Time은 초/분/시간등 리포팅에 의미있는 시간으로 조정할수 있습니다.

```
[INFO ] [2023-09-06 15:56:20,458] [com.webnori.springweb.akka.bench.BasicTest.HelloWorldTest-jmh-worker-1] [count : 1087000]
[INFO ] [2023-09-06 15:56:20,489] [com.webnori.springweb.akka.bench.BasicTest.HelloWorldTest-jmh-worker-1] [count : 1088000]
[INFO ] [2023-09-06 15:56:20,518] [ClusterSystem-akka.actor.default-dispatcher-8] [Running CoordinatedShutdown with reason [ActorSystemTerminateReason]]
[DEBUG] [09/06/2023 15:56:20.593] [ClusterSystem-akka.actor.internal-dispatcher-13] [EventStream] shutting down: StandardOutLogger
[DEBUG] [09/06/2023 15:56:20.595] [ClusterSystem-akka.actor.internal-dispatcher-13] [EventStream] all default loggers stopped
0.001 min/op

Result "com.webnori.springweb.akka.bench.BasicTest.HelloWorldTest":
  0.032 ±(99.9%) 0.015 s/op [Average]
  (min, avg, max) = (0.031, 0.032, 0.033), stdev = 0.001
  CI (99.9%): [0.016, 0.047] (assumes normal distribution)


# Run complete. Total time: 00:00:45

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                 Mode  Cnt  Score   Error  Units
BasicTest.HelloWorldTest  avgt    3  0.032 ± 0.015   s/op
```


기존 유닛테스트함수를 만드는방식과 유사하게
다양한 성능측정 전략을 어노테이션을 통해 이용할수 있습니다.
```
@Benchmark
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.SECONDS)
public void HelloWorldTest(Blackhole blackhole, MyState state) 
......

@Test
public void runBenchmarks() throws Exception {
    Options options = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .warmupTime(TimeValue.seconds(1))
            .warmupIterations(6)
            .threads(1)
            .measurementIterations(3)
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

    new Runner(options).run();
    }

// Mode.AverageTime : 1회평균 작동시간으로 표시됩니다. 1회 응답시간의 표시가 중요한경우 이용할수 있습니다.
// Mode.Throughput : 측정초기준 몇회가 작동되었냐가 표시됩니다. 초당처리(TPS)능력 표현이 중요할때 이용할수 있습니다.
    
```

함수당 호출수가 아닌 작동하는 도메인내에서 의미있가 있는  처리량인경우  테스트 라이프사이클을 고려 ( 전체테스트별 / 하위테스트별 ) 
커스텀측정을 설계할수도 있습니다.  
JMH는 호출량기반 성능측정을 시도하지만 , 시스템 성능시스템에 일반적으로 사용하는  매트릭스 기반측정이 필요한경우 활용할수 있습니다.

성능에 이용되는 값이 호출이아닌 특정값인경우 Blackhole또는 State수치를 관리할수 있습니다.
```
// 테스트에 특정한 상태값 변화를 측정할때 이용
@State(Scope.Thread)
public static class MyState {
    public int count = 0;
}

@Benchmark
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MINUTES)
public void HelloWorldTest(Blackhole blackhole, MyState state) {

  int testEventCount = 1000;
  .........................
// 한번작동할시 반복되는 특정수치기반으로 측정할시
// TODO : 해당수치가 작동되지 않아 업데이트중에 있습니다. (호출수기반으로만 리포팅됨)   
  blackhole.consume(testEventCount);
  logger.info("count : {}", state.count);

}

//테스트중 수행결과
[INFO ] [2023-09-06 15:56:20,489] [com.webnori.springweb.akka.bench.BasicTest.HelloWorldTest-jmh-worker-1] [count : 1088000]
```

다양한 측정전략을 사용할때 활용할수 있는 샘플코드 모음입니다.
- https://github.com/Valloric/jmh-playground/tree/master/src/jmh/java/org/openjdk/jmh/samples

여기서 작성된 샘플은 AKKATestToolkit과 함께 성능측정이 시도되었으며
AKKA를 채택하지 않더라도 AkkaStream API를 TestToolkit에 이용할수 있어서
다음과같은 조절기를 테스트 코드작성시 전략적으로 이용할수 있습니다.
(Thread.Sllep 이 아닌 조절기를 통한 흐름제어가가능)
```
throttler =
    Source.actorRef(1000, OverflowStrategy.dropNew())
            .throttle(givenAPiTPS, FiniteDuration.create(1, TimeUnit.SECONDS),
                    givenAPiTPS, (ThrottleMode) ThrottleMode.shaping())
            .to(Sink.actorRef(greetActor, akka.NotUsed.getInstance()))
            .run(materializer);
```

AKKA와 상관없이 JMH만으로 마이크로한 측정을 할수 있으며
더 자세한 기술자료는  다음링크를 통해서 확인할수 있습니다.

- https://www.baeldung.com/java-microbenchmark-harness
- https://medium.com/@truongbui95/jmh-java-microbenchmark-harness-tests-in-java-applications-f607f00f536d
- https://ysjee141.github.io/blog/quality/java-benchmark/
- https://wiki.webnori.com/display/AKKA/UnitTest+with+NBench - 닷넷버전도 준비가 되어있습니다.
