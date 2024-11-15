# Reactive Programming

Spring Boot의 Reactive 프로그래밍은 Spring WebFlux 모듈을 통해 지원됩니다. Spring WebFlux는 비동기 논블로킹(non-blocking) 방식으로 데이터를 처리하여 고성능 애플리케이션을 구현하는 데 도움을 줍니다. 이 방식은 특히 높은 동시성을 요구하는 애플리케이션이나 백프레셔(Backpressure)를 필요로 하는 상황에 적합합니다.

주요 특징은 다음과 같습니다:

비동기 및 논블로킹 I/O: 전통적인 스레드 기반 모델과 달리, WebFlux는 Reactor 라이브러리를 기반으로 리액티브 스트림(reactive streams)을 사용하여 I/O 작업을 비동기식으로 처리합니다. 이를 통해 자원을 효율적으로 사용하며 스레드 수를 줄일 수 있습니다.

Reactor 기반의 리액티브 스트림: Spring WebFlux는 리액티브 프로그래밍을 위한 Reactor 라이브러리를 사용합니다. Reactor는 Mono와 Flux라는 두 가지 주요 타입을 제공하며, 각각 단일 값과 다수의 값을 처리하는 리액티브 스트림을 나타냅니다.

백프레셔(Backpressure): WebFlux는 리액티브 스트림의 백프레셔를 지원하여 데이터 생성 속도가 소비 속도를 초과할 때의 문제를 완화합니다. 이는 시스템 리소스 관리와 안정성 유지에 도움이 됩니다.

함수형 API: WebFlux는 함수형 스타일로 작성된 API를 통해 코드를 간결하게 유지할 수 있도록 돕습니다. 예를 들어, 체이닝(chaining)이나 람다식 사용으로 가독성이 높아집니다.

서버와 클라이언트 모두 지원: WebFlux는 서버 측뿐 아니라 클라이언트 측에서도 리액티브 프로그래밍을 지원합니다. WebClient는 논블로킹 방식으로 외부 API 호출을 수행할 수 있는 리액티브 클라이언트로, 고성능 애플리케이션 개발에 유용합니다.

코루틴 및 Kotlin 지원: Kotlin과의 호환성을 고려하여, Spring WebFlux는 코루틴을 사용할 수 있도록 확장 기능을 제공합니다. 이를 통해 코루틴을 사용한 논블로킹 코드를 더욱 간결하고 직관적으로 작성할 수 있습니다.

Spring Boot에서 WebFlux를 사용하려면 spring-boot-starter-webflux 의존성을 추가하고, 이를 통해 컨트롤러, 서비스, 리포지토리 등을 리액티브 방식으로 구현할 수 있습니다.


Reactive Stream을 도전하기전 , 기본언어가 지원하는 병렬처리및 동시성처리의 차이점을 먼저 학습하는것이 권장됩니다. 

- [병렬처리 VS 동시성 처리](https://wiki.webnori.com/display/AKKA/Terminology)
- [동시프로그래밍](https://wiki.webnori.com/display/AKKA/Future+and+Promise)


Reactive는 구현체가 달라도 상호연동및 운영가능한 인터페이스로 

SpringBoot Reactive 요소와 Kotlin의 Reactive 요소를 상호연결 운영할수 있으며
Reactive를 지원하는 다양한 저장장치와 연동하여 활용할수 있습니다.

다음 구성요소를 활용하여 Reactive Stream을 구현할수 있습니다.

- Spring Boot Reactive Stream
  - Netty
  - Ractive Streams Adapter
  - Spring Security Reactive
  - Spring WebFlux 
  - Spring Data Reactive Repositories
    - MongoDB, Cassandra, Couchbase, Redis, Elasticsearch , R2DBC.......

- Kotlin Coroutine 
  - core: 기본 코루틴 기능을 제공하는 핵심 모듈입니다. 
  - reactive: 리액티브 스트림과의 상호 운용을 지원합니다. 
  - reactor: Reactor 라이브러리와의 통합을 지원합니다. 
  - rx2: RxJava 2와의 상호 운용성을 제공합니다. 
  - guava: Google Guava와의 호환을 위해 작성된 코루틴 모듈입니다.
  
## Spring Boot Reactive Stream

기대효과
고성능 처리: 논블로킹 I/O와 리액티브 스트림을 활용한 고성능 분산 처리를 구현할 수 있습니다.
확장성과 유연성: 다양한 비관계형 데이터베이스와의 리액티브 연동을 통해 대규모 확장성 있는 시스템을 구축할 수 있습니다.
리액티브 프로그램의 일관성: 코루틴과 리액티브 스트림의 통합으로 코드 일관성을 유지하면서, 효율적인 비동기 처리가 가능합니다.

추가로 활용할 수 있는 Stack

- Kotlin Flow: 단일 데이터 처리에서 스트림 데이터까지 확장된 비동기 처리를 지원합니다.
- gRPC 또는 GraphQL: 마이크로서비스 간의 고성능 통신을 위해, 비동기 통신을 지원하는 프로토콜을 사용할 수 있습니다.
- Kafka (Spring Cloud Stream): 이벤트 기반 아키텍처에서 데이터 스트림을 효율적으로 처리할 수 있으며, 확장된 비동기 처리를 위한 데이터 파이프라인에 활용할 수 있습니다.
- Spring Cloud Gateway: API 게이트웨이로서 리액티브 방식의 요청 라우팅과 필터링을 지원해, 마이크로서비스 아키텍처에서 유용합니다.
이 구성을 통해 코틀린과 스프링 부트 기반의 효율적이고 고성능의 분산 시스템을 구축할 수 있으며, 확장성을 갖춘 이벤트 기반 아키텍처로 발전시킬 수 있습니다.
    
## 코틀린 지원 순수 액터모델

위와 같은 장점은 코틀린의 core가 액터모델을 품고 있기때문에, 액터모델 채택시 기본으로 운영가능합니다.

- [코액터](https://github.com/psmon/java-labs/tree/master/KotlinBootLabs/src/test/kotlin/com/example/kotlinbootlabs/kactor)
- [연구위치](https://wiki.webnori.com/pages/viewpage.action?pageId=93946604)

## Stateful

상태프로그래밍을 이용한 Stream처리~

- https://velog.io/@ehdrms2034/%EC%B9%B4%ED%94%84%EC%B9%B4-%EC%8A%A4%ED%8A%B8%EB%A6%BC%EC%A6%88-DSL-%EA%B0%9C%EB%85%90
- https://breezymind.com/kafka-streams-basic/