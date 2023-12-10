package com.webnori.springweb.akka.stream.actor;


import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.akka.stream.actor.model.TPSInfo;

import java.time.Duration;

public class TpsMeasurementActor extends AbstractActorWithTimers {

    private static final Object TICK_KEY = "TickKey";

    private static final Object TICK_KEY2 = "TickKey2";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected long transactionCount = 0;

    private ActorRef probe;

    protected  double tps;

    protected double lastTps;

    public TpsMeasurementActor() {

        // OnlyOnce Timer - Start Timer
        getTimers().startSingleTimer(TICK_KEY, new FirstTick(), Duration.ofMillis(500));

    }

    public static Props Props() {
        return Props.create(TpsMeasurementActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ActorRef.class, actorRef -> {
                    this.probe = actorRef;
                    getSender().tell("done", getSelf());
                })
                .match(FirstTick.class, message -> {
                    // do something useful here
                    log.info("First Tick");
                    // Repeat Timer
                    getTimers().startPeriodicTimer(TICK_KEY, new TPSCheckTick(), Duration.ofMillis(1000));
                })
                .match(String.class, message -> {
                    if(message.equals("tps")){
                        getSender().tell(new TPSInfo(lastTps), ActorRef.noSender());
                    }else{
                        transactionCount++;
                    }
                })
                .match(TPSCheckTick.class, message -> {
                    long startTime = System.currentTimeMillis() - 1000;
                    long endTime = System.currentTimeMillis();
                    tps = transactionCount  / ((endTime - startTime) / 1000);
                    if(tps > 0){
                        lastTps = tps;
                        getTimers().startPeriodicTimer(TICK_KEY2, new TPSResetHalfTick(), Duration.ofMillis(500));
                    }
                    transactionCount = 0;
                    log.info("TPS:" + lastTps);
                })
                .match(TPSResetHalfTick.class, message -> {
                    lastTps = lastTps / 2;
                    getTimers().startPeriodicTimer(TICK_KEY2, new TPSResetTick(), Duration.ofMillis(500));
                })
                .match(TPSResetTick.class, message -> {
                    lastTps = 0;
                })
                .build();
    }

    private static final class FirstTick {
    }

    private static final class TPSCheckTick {
    }

    private static final class TPSResetTick {
    }

    private static final class TPSResetHalfTick {
    }

}