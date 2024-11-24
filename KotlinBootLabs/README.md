# KotlinBootLabs Reactive Stream

Spring Boot 환경에서 Kotlin을 학습하는 저장공간 입니다. 

우리는 이것을 코프링이라고 부르며, 코틀린과 스프링부트를 학습하는 공간입니다.

CRUD가 대부분의 도메인 문제를 해결나겠지만 여기서는 CRUD보다는 단일지점 병목을 분산처리하는 이벤트 드리븐 방식을 활용하는 액터모델과 함께 Reactive Stream을 연구합니다.


## Reactive Stream

Reactive Streams는 비동기 스트림 처리를 표준화하기 위해 시작되었습니다.
이는 데이터 흐름의 반응성을 보장하고 백프레셔 문제를 해결하며,
다양한 라이브러리 간의 호환성을 목표로 합니다.
2013년 Reactive Manifesto 발표 이후, Netflix, Pivotal 등 주요 기술 기업들이 협력하여
2014년에 Reactive Streams를 표준화했습니다. 이를 통해 비동기 데이터 스트림 처리의 안정성과 확장성을 높이고,
자바 표준 라이브러리와 프레임워크에서 쉽게 사용할 수 있게 되었습니다.

Reactive Streams는 여러 기술에서 활용되고 있습니다.
대표적인 예로 Project Reactor(Spring 프레임워크에서 사용),
Akka Streams, RxJava 등이 있으며, 이들은 Reactive Streams 표준을 따르며
비동기 스트림 처리를 지원합니다. Java 9의 Flow API도 Reactive Streams의 개념을 도입하여
자바 표준 라이브러리에서 비동기 스트림 처리를 쉽게 구현할 수 있도록 하였습니다.
이러한 기술들은 마이크로서비스 아키텍처, 데이터 집약적 애플리케이션에서 높은 성능과 반응성을 유지하며,
확장 가능한 비동기 처리를 가능하게 합니다.


KotlinBootReactiveLabs 에서는 SpringBoot MVC모드가 아닌 WebFlux를 활용하여 Reactive Stream으로 작동합니다.
Reactive Stream에서는 Spring Boot2가 제공하는 Reactive Stack만 활용해 다양한 비동기처리 문제 해결을 시도합니다.

- [Reactive Streams With Webplux](https://github.com/psmon/java-labs/blob/master/KotlinBootLabs/REACTIVE.MD)
- [KotlinBootReactiveLabs](https://github.com/psmon/java-labs/blob/master/KotlinBootReactiveLabs/README.MD)

## Akka

Akka는 CQRS, Event Sourcing, Actor Model 등의 패턴을 지원하는 라이브러리로 병렬 및 분산 시스템에서 상태와 동작을 독립적인 액터(Actor)로 캡슐화하여 처리하는 모델입니다. 각 액터는 메시지 기반으로 통신하며, 동시성 문제를 해결하고 병목을 최소화합니다. 액터는 상태를 공유하지 않고, 비동기적으로 메시지를 주고받으며, 독립적인 실행 단위를 구성해 확장성과 유연성이 뛰어납니다.

이 라이브러리를 채택하지 않더라도 이러한 개념이 적용된 완성형 액터모델을 통해 이벤트드리븐 방식을 학습할수 있으며 

Spring Boot이 제공하는 Reactive Stack을 순수하게 사용했을시도 도움될수 있습니다. 

- [Reactive Streams With Akka](https://github.com/psmon/java-labs/blob/master/KotlinBootLabs/AKKA.MD)


### Reference Documentation

- [Base Concepts](https://wiki.webnori.com/display/AKKA/Concepts)
- [Actor With Kotlin](https://wiki.webnori.com/display/AKKA/AKKA.Kotlin)
- [ReactiveSummit](https://www.youtube.com/@ReactiveSummit/videos)
- [Line Chatting Architecture](https://engineering.linecorp.com/ko/blog/the-architecture-behind-chatting-on-line-live)
- [Asynchronous testing](https://doc.akka.io/docs/akka/current/typed/testing-async.html)
- [Akka Cluster](https://www.youtube.com/watch?v=mUTKvGyxbOA)
- https://doc.akka.io/libraries/akka-core/2.7/
