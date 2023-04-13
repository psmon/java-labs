package com.webnori.springweb.akka.actors;


import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SafeBatchActor extends AbstractActorWithTimers {

    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ArrayList<String> batchList;

    public SafeBatchActor() {
        // OnlyOnce Timer - Start Timer
        batchList = new ArrayList();
        getTimers().startSingleTimer(TICK_KEY, new FirstTick(), Duration.ofMillis(0));
    }

    public static Props Props() {
        return Props.create(SafeBatchActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, message ->{
                    batchList.add(message);
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
                    log.info("Bath Flush {}",batchList.size());
                    batchList.clear();
                })
                .build();
    }

    private static final class FirstTick {
    }

    private static final class Tick {
    }
}