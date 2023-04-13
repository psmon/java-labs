package com.webnori.springweb.example.akka;


import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import scala.Console;

import java.time.Duration;

public class TimerActor extends AbstractActorWithTimers {

    private static Object TICK_KEY = "TickKey";
    private static final class FirstTick {
    }
    private static final class Tick {
    }

    public static Props Props() {
        return Props.create(TimerActor.class);
    }

    public TimerActor() {
        getTimers().startSingleTimer(TICK_KEY, new FirstTick(), Duration.ofMillis(500));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FirstTick.class, message -> {
                    // do something useful here
                    Console.println("First Tick");
                    getTimers().startPeriodicTimer(TICK_KEY, new Tick(), Duration.ofSeconds(1));
                })
                .match(Tick.class, message -> {
                    // do something useful here
                    Console.println("Tick");
                })
                .build();
    }
}