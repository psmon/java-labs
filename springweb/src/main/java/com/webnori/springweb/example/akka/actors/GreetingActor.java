package com.webnori.springweb.example.akka.actors;


import akka.actor.AbstractActor;
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

    private Random rand = new Random();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(FakeSlowMode.class, message -> {
            isSlowMode = true;
            log.info("Switch SlowMode:{}", self().path(), isSlowMode);
        })
        .match(String.class, s -> {

            int sleepSec = 0;
            if(isSlowMode){
                // 0.5 + 랜덤 1초
                sleepSec = rand.nextInt(1000) + 500;
                Thread.sleep(sleepSec);
            }
            //log.info("{}:Completed String message: {} - Delay:{}", self().path(), s, sleepSec);

        })
        .matchAny(o -> log.info("received unknown message")).build();
    }

}
