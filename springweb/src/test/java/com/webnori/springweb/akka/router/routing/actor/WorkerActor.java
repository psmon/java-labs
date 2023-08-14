package com.webnori.springweb.akka.router.routing.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class WorkerActor extends AbstractActor {

    private ActorRef _probe;
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    public static Props Props(ActorRef _probe) {
        return Props.create(WorkerActor.class, _probe);
    }

    public WorkerActor(ActorRef probe){
        _probe = probe;
    }

    private int messageCount = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        WorkMessage.class,
                        message -> {
                            String pathName = self().path().name();
                            messageCount++;
                            log.info("[{}] ChildActor InMessage : {} - {}", pathName, message, messageCount);
                            // Routee가 a일때 임의 지연
                            if(pathName.equals("$a")){
                                log.info("SomeBlocking - 300ms");
                                Thread.sleep(300);
                            }
                            _probe.tell("completed", ActorRef.noSender());
                        })
                .build();
    }
}