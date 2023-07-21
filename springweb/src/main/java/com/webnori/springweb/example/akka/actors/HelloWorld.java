package com.webnori.springweb.example.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.example.akka.models.FakeSlowMode;

// https://doc.akka.io/docs/akka/current/index-actors.html  - Classic Actor

public class HelloWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef probe;

    private boolean isBlockForTest = false;

    public static Props Props() {
        return Props.create(HelloWorld.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        })
        .match(FakeSlowMode.class, message -> {
            isBlockForTest = true;
            log.info("Switch SlowMode:{}", self().path(), isBlockForTest);
        })
        .match(String.class, s -> {
            if (isBlockForTest) Thread.sleep(3000L);
            if (probe != null) {
                probe.tell("world", this.context().self());
                log.info("Received String message: {}", s);
            } else {
                log.info("Received String message: {}", s);
            }
        })
        .matchAny(o -> log.info("received unknown message"))
        .build();
    }

}