package com.webnori.springweb.example.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

// https://doc.akka.io/docs/akka/current/index-actors.html  - Classic Actor

public class HelloWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef probe;

    private  boolean isBlockForTest = false;

    public static Props Props() {
        return Props.create(HelloWorld.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, s -> {
            log.info("Received String message: {}", s);

            // ForTest
            if (probe != null) {

                if(s.equals("command:tobeslow")){
                    isBlockForTest = true;
                }

                if(isBlockForTest) Thread.sleep(500L);

                probe.tell("world", this.context().self());
            }

        }).match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        }).matchAny(o -> log.info("received unknown message")).build();
    }

}