package com.webnori.springweb.akka.intro;


import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.akka.AbstractJavaTest;
import com.webnori.springweb.example.akka.actors.ParentActor;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * TestClass : HierarchyActorTest
 * 목표 : 액터의 계층(Hierarchy) 구조로 학습합니다.
 * 참고 링크 : https://doc.akka.io/docs/akka/current/typed/guide/tutorial_1.html
 */
public class HierarchyActorTest extends AbstractJavaTest {

    public ActorRef parentActor;
    private TestKit probe;

    public HierarchyActorTest() {
        setupDI();
    }

    public void setupDI() {

        // 액터의 생성방법을 설명하며
        // 어플리케이션 초기화때 미리 생성할수 있습니다.
        probe = new TestKit(system);

        // 부모액터 생성
        parentActor = system.actorOf(ParentActor.Props());

        // 자식생성 COMMAND : 생성자 Params을 통해 생성도 가능하지만
        // 자식 Object 생성책임을 부모가 지며 의존없는 구조전략때 활용할수 있습니다.
        parentActor.tell(ParentActor.CMD_CREATE_CHILDS, ActorRef.noSender());

    }

    @Test
    @DisplayName("Round Robbin Test")
    public void EventFlowParentTOChildTest() {
        new TestKit(system) {{
            // 이벤트검사를 위한 관찰자를 셋업합니다.
            parentActor.tell(probe.getRef(), getRef());

            // 자식 액터에게 일을 자동분배합니다. ( RoundRobbin )
            parentActor.tell(ParentActor.CMD_SOME_WORK, ActorRef.noSender());
            parentActor.tell(ParentActor.CMD_SOME_WORK, ActorRef.noSender());
            parentActor.tell(ParentActor.CMD_SOME_WORK, ActorRef.noSender());


            /*
            작동하는 로깅으로도 작업이 분배되는지 확인할수 있습니다. ( 자식액터의 이름을 표시 )

[INFO ] [2023-07-21 15:48:39,050] [akka-spring-demo-akka.actor.default-dispatcher-8] [[w2] ChildActor InMessage : CMD_SOME_WORK]
[INFO ] [2023-07-21 15:48:39,053] [akka-spring-demo-akka.actor.default-dispatcher-8] [[w1] ChildActor InMessage : CMD_SOME_WORK]
[INFO ] [2023-07-21 15:48:39,053] [akka-spring-demo-akka.actor.default-dispatcher-8] [[w3] ChildActor InMessage : CMD_SOME_WORK]
             */

            // 자식액터의 Job이 완료됨을 관찰자를 통해 검사합니다.
            probe.expectMsg(ParentActor.CMD_SOME_WORK_COMPLETED);
            probe.expectMsg(ParentActor.CMD_SOME_WORK_COMPLETED);
            probe.expectMsg(ParentActor.CMD_SOME_WORK_COMPLETED);

        }};
    }

    @Test
    @DisplayName("EventForwardTest")
    public void EventForwardTest() {
        new TestKit(system) {{

            TestKit forwardActor = new TestKit(system);
            // ActorRef를 지정하여 전송합니다.
            // 전송자(sender)가 메시지를 받을수 있는 액터가 아닐시 활용될수 있습니다.
            parentActor.tell(ParentActor.CMD_MESSAGE_REPLY, forwardActor.getRef());

            // forawrd를 통해, 메시지를 돌려받을수 있는지 확인합니다.
            forwardActor.expectMsg(ParentActor.CMD_MESSAGE_REPLY);
        }};
    }

}
