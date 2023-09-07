package com.webnori.springweb.example.akka.actors;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.example.akka.models.FakeSlowMode;

import java.util.Random;

public class GreetingActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props Props() {
        return Props.create(GreetingActor.class);
    }

    public static Props Props(String dispatcher) {
        return Props.create(GreetingActor.class).withDispatcher(dispatcher);
    }

    private boolean isSlowMode = false;

    private Long blockTime;

    private ActorRef probe;

    private Random rand = new Random();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        })
        .match(FakeSlowMode.class, message -> {
            isSlowMode = true;
            blockTime = message.bockTime;
            log.info("Switch SlowMode:{}", self().path());
        })
        .match(String.class, s -> {

            int sleepSec = 0;
            if(isSlowMode){
                sleepSec = rand.nextInt(500); // ~0.5 랜덤가중치
                Thread.sleep(blockTime + sleepSec);
            }
            //log.info("{}:Completed String message: {} - Delay:{}", self().path(), s, sleepSec);

        })
        .matchAny(o -> log.info("received unknown message")).build();
    }

}
