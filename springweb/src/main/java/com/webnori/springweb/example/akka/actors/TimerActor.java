package com.webnori.springweb.example.akka.actors;


import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;

public class TimerActor extends AbstractActorWithTimers {

    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef helloActor;

    public TimerActor() {

        // OnlyOnce Timer - Start Timer
        //getTimers().startSingleTimer(TICK_KEY, new FirstTick(), Duration.ofMillis(500));

        // Create Child Actor
        helloActor = context().actorOf(HelloWorld.Props(), "helloActor");

    }

    public static Props Props() {
        return Props.create(TimerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FirstTick.class, message -> {
                    // do something useful here
                    log.info("First Tick");

                    // Repeat Timer
                    getTimers().startPeriodicTimer(TICK_KEY, new Tick(), Duration.ofSeconds(1));
                })
                .match(Tick.class, message -> {
                    // do something useful here
                    log.info("Tick");
                    helloActor.tell("Hello~", self());
                })
                .build();
    }

    private static final class FirstTick {
    }

    private static final class Tick {
    }
}