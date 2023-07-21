package com.webnori.springweb.example.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.RoundRobinGroup;
import akka.routing.RoundRobinPool;

import javax.naming.Context;
import java.util.Arrays;
import java.util.List;

public class ParentActor extends AbstractActor {

    // 이벤트 정의
    // Test를 위해 간소하게 String으로 정의되었으며
    // 이벤트에 다양한 메타정보를 담는경우Class(Object) 로 관리되는것이 권장됩니다.
    public static String CMD_CREATE_CHILDS = "CMD_CREATE_CHILDS";
    public static String CMD_SOME_WORK = "CMD_SOME_WORK";

    public static String CMD_MESSAGE_REPLY = "CMD_MESSAGE_REPLY";
    public static String CMD_SOME_WORK_COMPLETED = "CMD_SOME_WORK_COMPLETED";

    private boolean firstInit;

    private ActorRef routerActor;

    private ActorRef testProbeActor;    //유닛테스트를 위한 관찰자 액터

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props Props() {
        return Props.create(ParentActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> s.equals(CMD_CREATE_CHILDS) , s -> {
                    if(!firstInit) {
                        // 동시처리를 위한 worker actor, 균등수를 옵션화하여 유연한 코드 작성가능~
                        var w1 = context().actorOf(ChildActor.Props(),"w1");
                        var w2 = context().actorOf(ChildActor.Props(),"w2");
                        var w3 = context().actorOf(ChildActor.Props(),"w3");

                        // Path함수의 Name을통해
                        List<String> paths = Arrays.asList(
                                "/user/"+self().path().name()+"/"+w1.path().name(),
                                "/user/"+self().path().name()+"/"+w2.path().name(),
                                "/user/"+self().path().name()+"/"+w3.path().name());

                        routerActor = getContext().actorOf(new RoundRobinGroup(paths).props(), "router16");
                    }
                    firstInit = true;
                })
                .match(String.class, s -> s.equals(CMD_SOME_WORK) , s -> {
                    // 부모 액터를 통해 부하를 균등합니다.
                    routerActor.tell(CMD_SOME_WORK, ActorRef.noSender());
                })
                .match(String.class, s -> s.equals(CMD_SOME_WORK_COMPLETED) , s -> {
                    // 자식의 작업완료 통지를 받습니다.
                    // 작업완료도 부하균등이 필요한경우 router를 생성하여 완료처리를 위임할수 있습니다.
                    log.info("CMD_SOME_WORK_COMPLETED");

                    //For Unit Test ( 완료작업 검증)
                    if(testProbeActor != null){
                        testProbeActor.tell(CMD_SOME_WORK_COMPLETED, ActorRef.noSender());
                    }

                })
                .match(String.class, s -> s.equals(CMD_MESSAGE_REPLY) , s -> {
                    // 전송자가 아닌 지저장(forwad)에게 메시지를 전송합니다.
                    sender().forward(CMD_MESSAGE_REPLY, getContext());
                })
                .match(ActorRef.class, actorRef -> {
                    // UnitTest를 위한 참조자 액터 셋팅
                    testProbeActor = actorRef;
                })
                .matchAny(o -> log.warning("received unknown message")).build();
    }
}
