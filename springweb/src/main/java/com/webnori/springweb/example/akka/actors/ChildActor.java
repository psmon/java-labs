package com.webnori.springweb.example.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ChildActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props Props() {
        return Props.create(ChildActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    // 분배가 되는지 액터의 이름을 로깅합니다.
                    log.info("[{}] ChildActor InMessage : {}", self().path().name(), s);

                    // 부모어게 완료 통지를 합니다.
                    context().parent().tell(ParentActor.CMD_SOME_WORK_COMPLETED , ActorRef.noSender());
                })
                .matchAny(o -> log.warning("received unknown message")).build();
    }

}
