package com.webnori.springweb.akka.actors;


import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;

public class TestTimerActor extends AbstractActorWithTimers {

    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public TestTimerActor() {
        // OnlyOnce Timer - Start Timer
        getTimers().startSingleTimer(TICK_KEY, new FirstTick(), Duration.ofMillis(500));
    }

    public static Props Props() {
        return Props.create(TestTimerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, message -> {
                    log.info(message);
                })
                .match(FirstTick.class, message -> {
                    // do something useful here
                    log.info("First Tick");

                    // Repeat Timer
                    getTimers().startPeriodicTimer(TICK_KEY, new Tick(), Duration.ofSeconds(1));
                })
                .match(Tick.class, message -> {
                    // do something useful here
                    log.info("Tick");
                })
                .build();
    }

    private static final class FirstTick {
    }

    private static final class Tick {
    }
}