package com.webnori.springweb.example.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

// https://doc.akka.io/docs/akka/2.7.0/typed/actors.html

public class HelloWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef probe;

    public static Props Props() {
        return Props.create(HelloWorld.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, s -> {
            log.info("Received String message: {}", s);
            if (probe != null) {
                probe.tell("world", this.context().self());
            }
        }).match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        }).matchAny(o -> log.info("received unknown message")).build();
    }

}