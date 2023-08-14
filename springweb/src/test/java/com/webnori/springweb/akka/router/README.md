# Router

Test코드를 통해 AKKA제공 다양한 라우팅 코드를 학습합니다.

## Pool VS GROUP

- Pool(풀) : 라우터는 라우트를 하위 액터로 생성하고 종료되면 라우터에서 제거합니다.
- Group(그룹) : 라우트 액터는 라우터 외부에서 생성되며 라우터는 종료를 감시하지 않고 액터 선택을 사용하여 지정된 경로로 메시지를 보냅니다.

## 지원되는 라우터

- round-robin
- random
- balancing
- smallest-mailbox
- broadcast
- scatter-gather
- tail-chopping
- consistent-hashing

## Test Doc

- https://wiki.webnori.com/display/AKKA/RoutingTest
- 
