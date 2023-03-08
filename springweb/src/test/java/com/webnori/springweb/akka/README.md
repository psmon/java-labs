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

## 테스트 코드

실제 동작하는 테스트코드를 참고하여, AKKA의 기능을 이해할수 있습니다.

- [AkkaBasicTests.java](AkkaBasicTests.java) : Akka 기본 메시지 전송
- [AkkaDisPatcherTests.java](AkkaDisPatcherTests.java) : Dispatcher를 이용한 동시성처리 ( 멀티스레드 대응 )
- [AkkaThrottleTests.java](AkkaThrottleTests.java) : 메시지 처리 속도제어 ( API호출제약및 생산속도 조절시 사용 )

