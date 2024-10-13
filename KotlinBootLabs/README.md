# KotlinBootLabs Reactive Stream

Spring Boot 환경에서 Kotlin을 학습하는 저장공간 입니다.

CRUD가 대부분의 도메인 문제를 해결나겠지만 여기서는 CRUD보다는 단일지점 병목을 분산처리하는 이벤트 드리븐 방식을 활용하는 액터모델과 함께 Reactive Stream을 연구합니다.

## Unit Test


``` kotlin
val probe = testKit.createTestProbe<HelloActorResponse>()

val helloActor = testKit.spawn(HelloActor.create())

helloActor.tell(Hello("Hello", probe.ref()))

probe.expectMessage(HelloResponse("Kotlin"))

helloActor.tell(GetHelloCount(probe.ref()))

probe.expectMessage(HelloCountResponse(1))
```

여기서 작성되는 모든 코드는 , 작성코드를 스스로 설명하고 로직을 증명하는 유닛테스트를 기본으로 작성하합니다.

작동중인 코드의 흐름을 블락킹해 중단테스트를 하는것이 아닌 관찰자(Observer,Probe)를 통해 완료 이벤트를 검증방식을 이용합니다.



## Actor Model

```
/** HelloActor 클래스 */
class HelloActor private constructor(
    private val context: ActorContext<HelloActorCommand>,
) : AbstractBehavior<HelloActorCommand>(context) {
 
    companion object {
        fun create(): Behavior<HelloActorCommand> {
            return Behaviors.setup { context -> HelloActor(context) }
        }
    }
 
    override fun createReceive(): Receive<HelloActorCommand> {
        return newReceiveBuilder()
            .onMessage(Hello::class.java, this::onHello)
            .build()
    }
 
    private fun onHello(command: Hello): Behavior<HelloActorCommand> {
 
        if (command.message == "Hello") {
            command.replyTo.tell(HelloResponse("Kotlin"))
        }
        return this
    }
}
```
액터 모델은 병렬 및 분산 시스템에서 상태와 동작을 독립적인 액터(Actor)로 캡슐화하여 처리하는 모델입니다. 각 액터는 메시지 기반으로 통신하며, 동시성 문제를 해결하고 병목을 최소화합니다. 액터는 상태를 공유하지 않고, 비동기적으로 메시지를 주고받으며, 독립적인 실행 단위를 구성해 확장성과 유연성이 뛰어납니다.


## Websocket

``` kotlin
class MyWebSocketHandler : TextWebSocketHandler() {
    private val logger = LoggerFactory.getLogger(MyWebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        logger.info("Connected: ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: org.springframework.web.socket.CloseStatus) {
        sessions.remove(session.id)
        logger.info("Disconnected: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        logger.info("Received message: $payload from session: ${session.id}")

        // Example: Handle subscription to a topic
        if (payload.startsWith("subscribe:")) {
            val topic = payload.substringAfter("subscribe:")
            topicSubscriptions.computeIfAbsent(topic) { mutableSetOf() }.add(session.id)
            logger.info("Session ${session.id} subscribed to topic $topic")
        } else {
            session.sendMessage(TextMessage("Echo: $payload"))
        }
    }

    fun sendMessageToSession(sessionId: String, message: String) {
        sessions[sessionId]?.sendMessage(TextMessage(message))
    }

    fun sendMessageToTopic(topic: String, message: String) {
        topicSubscriptions[topic]?.forEach { sessionId ->
            sessions[sessionId]?.sendMessage(TextMessage(message))
        }
    }
}
```

웹소켓(WebSocket)은 클라이언트와 서버 간에 지속적인 연결을 유지하여 양방향 통신을 실시간으로 제공하는 프로토콜입니다. HTTP 요청 후 연결이 유지되며, 클라이언트와 서버가 자유롭게 데이터를 주고받을 수 있습니다. 낮은 지연 시간과 효율적인 네트워크 사용이 장점이며, 채팅, 게임, 실시간 업데이트 등에서 많이 사용됩니다. HTTP보다 더 가벼운 통신을 제공해 빠른 반응성이 요구되는 애플리케이션에 적합합니다.

## WebSocket with Actor Model

웹소켓과 액터 모델을 연동하면 실시간 양방향 통신과 고도로 병렬화된 비동기 처리의 장점을 결합할 수 있습니다. 웹소켓은 클라이언트와 서버 간 실시간 데이터 전송을 가능하게 하고, 액터 모델은 각 연결을 독립적인 액터로 처리하여 확장성과 동시성을 극대화합니다. 이를 통해 실시간 시스템에서 클라이언트 요청을 병목 없이 처리하고, 여러 사용자와의 상호작용을 효율적으로 관리할 수 있습니다. 또한, 액터 모델의 상태 격리 덕분에 충돌 위험 없이 여러 연결을 안전하게 처리할 수 있습니다.

## Akka Cluster


액터 모델은 병렬 처리와 분산 시스템을 다루기 위한 개념적 프레임워크로, 각 액터가 독립적으로 상태를 관리하고 메시지를 주고받으며 상호작용합니다. 로컬에서 작성된 액터 모델을 클러스터화할 때, 주요 컨셉은 코드의 큰 변경 없이 확장성과 분산성을 유지하는 것입니다. 다음은 이 개념을 요약한 몇 가지 핵심 요소입니다:

독립적인 상태 관리: 액터는 각자의 상태를 독립적으로 관리하며, 다른 액터들과 공유되지 않습니다. 이를 통해 로컬 환경에서 동작하던 액터들이 클러스터 환경으로 확장될 때, 상태 관리의 일관성을 유지할 수 있습니다.

메시지 기반 통신: 액터 간의 상호작용은 메시지를 통해 이루어지며, 메시지는 비동기적으로 전달됩니다. 로컬에서 작성된 메시지 전달 로직은 클러스터화할 때 네트워크를 통해 전달되도록 자연스럽게 확장될 수 있습니다. 즉, 액터 간 통신 방식은 로컬이나 클러스터 환경에서 동일하게 유지됩니다.

위치 투명성(Location Transparency): 액터 모델에서는 액터의 실제 위치(로컬이든 클러스터든)가 중요하지 않습니다. 액터는 서로의 위치를 알 필요 없이 메시지를 주고받습니다. 클러스터로 확장될 때에도 액터의 위치는 추상화되며, 이를 통해 코드 수정 없이 클러스터에 배포 가능합니다.

분산 가능한 스케줄링: 액터의 실행은 스케줄러에 의해 관리되며, 클러스터화된 환경에서는 이 스케줄링이 분산 시스템 전체로 확장될 수 있습니다. 즉, 액터를 로컬 환경에서 스케줄링하듯이 클러스터 환경에서도 분산된 자원 위에서 스케줄링할 수 있습니다.

장애 내성(Fault Tolerance): 클러스터 환경에서는 액터가 장애가 발생한 경우에도 이를 복구할 수 있는 메커니즘(예: 액터 복제, 상태 저장)을 제공하여 분산 시스템에서의 안정성을 확보합니다. 이는 로컬에서 구현된 액터 모델이 클러스터로 확장될 때, 별도의 코드 수정 없이 장애를 처리할 수 있게 해줍니다.

이러한 개념들을 통해, 로컬에서 작성된 액터 모델 코드는 큰 변경 없이 클러스터화할 수 있으며, 분산 시스템에서 자연스럽게 동작하도록 확장됩니다. 대표적인 프레임워크로는 Akka, Microsoft Orleans 등이 있으며, 이들은 액터 모델의 클러스터링 기능을 지원합니다.


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



### Reference Documentation

- [Base Concepts](https://wiki.webnori.com/display/AKKA/Concepts)
- [Actor With Kotlin](https://wiki.webnori.com/display/AKKA/AKKA.Kotlin)
- [ReactiveSummit](https://www.youtube.com/@ReactiveSummit/videos)
- [Line Chatting Architecture](https://engineering.linecorp.com/ko/blog/the-architecture-behind-chatting-on-line-live)
- [Asynchronous testing](https://doc.akka.io/docs/akka/current/typed/testing-async.html)
- [Akka Cluster](https://www.youtube.com/watch?v=mUTKvGyxbOA)
