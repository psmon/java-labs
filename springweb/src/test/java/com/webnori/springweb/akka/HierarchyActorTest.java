package com.webnori.springweb.akka;

/*#######################################################
    TestClass : HierarchyActorTest
    목표 : 액터를 계층(부모-자식)구조를 테스트화하고 학습합니다.
#########################################################*/


import akka.actor.ActorRef;
import akka.routing.RoundRobinPool;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.example.akka.AkkaManager;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import com.webnori.springweb.example.akka.actors.ParentActor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

public class HierarchyActorTest extends AbstractJavaTest {

    private TestKit probe;

    public ActorRef parentActor;

    public HierarchyActorTest(){
        setupDI();
    }

    public  void setupDI() {

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
    public void EventFlowParentTOChildTest(){
        new TestKit(system){{
            // 이벤트검사를 위한 관찰자를 셋업합니다.
            parentActor.tell(probe.getRef(), getRef());

            // 자식 액터에게 일을 자동분배합니다. ( RoundRobbin )
            parentActor.tell(ParentActor.CMD_SOME_WORK, ActorRef.noSender());
            parentActor.tell(ParentActor.CMD_SOME_WORK, ActorRef.noSender());
            parentActor.tell(ParentActor.CMD_SOME_WORK, ActorRef.noSender());

            // 자식액터의 Job이 완료됨을 관찰자를 통해 검사합니다.
            probe.expectMsg(ParentActor.CMD_SOME_WORK_COMPLETED);
            probe.expectMsg(ParentActor.CMD_SOME_WORK_COMPLETED);
            probe.expectMsg(ParentActor.CMD_SOME_WORK_COMPLETED);

        }};
    }

}
