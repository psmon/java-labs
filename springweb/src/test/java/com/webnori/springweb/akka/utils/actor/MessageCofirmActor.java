package com.webnori.springweb.akka.utils.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MessageCofirmActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ActorRef probe;

    public static Props Props() {
        return Props.create(MessageCofirmActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        })
        .match(String.class, s -> {
            if (probe != null) {
                probe.tell(s , this.context().self());
            } else {
                log.info("Received String message: {}", s);
            }
        })
        .matchAny(o -> log.info("received unknown message"))
        .build();
    }
}