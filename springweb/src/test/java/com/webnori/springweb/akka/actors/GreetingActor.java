package com.webnori.springweb.akka.actors;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import javax.naming.Context;

public class GreetingActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props Props() {
        return Props.create(GreetingActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(String.class, s -> {
            log.info("{}:Received String message: {}", self().path(), s);
        })
        .matchAny(o -> log.info("received unknown message")).build();
    }
}
