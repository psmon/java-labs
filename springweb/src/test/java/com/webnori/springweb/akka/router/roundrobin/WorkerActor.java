package com.webnori.springweb.akka.router.roundrobin;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import java.util.ArrayList;
import java.util.List;

public class WorkerActor extends AbstractActor {

    private ActorRef _probe;
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    public static Props Props(ActorRef _probe) {
        return Props.create(WorkerActor.class, _probe);
    }

    public WorkerActor(ActorRef probe){
        _probe = probe;
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        WorkMessage.class,
                        message -> {
                            log.info("[{}] ChildActor InMessage : {}", self().path().name(), message);
                            _probe.tell("completed", ActorRef.noSender());
                        })
                .build();
    }
}