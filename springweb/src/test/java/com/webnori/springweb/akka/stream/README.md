# Streams

Akka Streams 알아봅니다.

```
이 테스트 코드는 Akka 프레임워크를 사용한 단위 테스트입니다. 주요 기능과 목적을 요약하면 다음과 같습니다:

테스트 설계: BackPressureTest라는 이름의 유닛 테스트입니다. 이 테스트는 Akka의 TestKit 클래스를 사용하여 작성되었습니다. TestKit은 Akka 액터 시스템을 테스트하기 위한 유틸리티입니다.

액터 시스템 및 자료 흐름 설정:

Materializer와 TestKit 인스턴스를 생성합니다. 이들은 스트림 처리와 테스트 실행에 사용됩니다.
TpsMeasurementActor 액터를 생성하고, 이 액터에게 메시지를 보냅니다. 이는 아마도 스트림 처리의 성능 측정을 위한 것으로 보입니다.
Akka Streams 사용:

소스 생성: Source.range(1, 10000)을 사용하여 1부터 10,000까지의 숫자를 생성합니다.
Flow 정의: 비동기 API 호출을 시뮬레이션하기 위해 mapAsync와 buffer를 사용한 Flow를 정의합니다. mapAsync를 사용하여 병렬 처리를 구현하고, buffer를 통해 백프레셔(Back Pressure) 전략을 적용합니다.
싱크(Sink) 정의: 결과를 출력하기 위한 Sink를 정의합니다.
백프레셔 전략: 스트림 처리 중에 백프레셔를 관리하기 위해 bufferSize를 설정하고, OverflowStrategy.backpressure()를 사용합니다. 이는 시스템이 과부하 상태가 되지 않도록 하기 위함입니다.

RunnableGraph 실행: 정의된 소스, 플로우, 싱크를 연결하여 스트림 처리 파이프라인을 구축하고 실행합니다.

비동기 API 호출 시뮬레이션: callApiAsync 메서드는 CompletableFuture를 사용하여 비동기적으로 API 호출을 시뮬레이션합니다. 각 호출은 100ms의 지연 후에 완료되며, tpsActor에 완료 이벤트를 보냅니다.

테스트 검증: within 메서드를 사용하여 15초 동안 아무 메시지도 수신되지 않는 것을 확인함으로써, 스트림 처리가 예상대로 작동하는지 검증합니다.

이 테스트는 Akka Streams의 백프레셔 메커니즘과 비동기 처리 기능을 검증하는 데 중점을 두고 있습니다.
```

참고링크 :
- https://blog.rockthejvm.com/akka-streams-backpressure/
