package com.webnori.springweb.webflux.actor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.akka.stream.actor.model.TPSInfo;
import com.webnori.springweb.webflux.actor.model.ConfirmEvent;
import com.webnori.springweb.webflux.actor.model.TPSReq;

import java.time.Duration;

public class SubScribeActor extends AbstractActorWithTimers {
    private static final Object TICK_TPS_KEY = "TickKey";

    private static final Object TICK_TPSRESET_KEY = "TickKey2";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected long transactionCount = 0;
    protected double tps;
    protected double lastTps;
    private ActorRef probe;

    public SubScribeActor() {

        // OnlyOnce Timer - Start Timer
        getTimers().startSingleTimer(TICK_TPS_KEY, new FirstTick(), Duration.ofMillis(500));

    }

    public static Props Props() {
        return Props.create(SubScribeActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ActorRef.class, actorRef -> {
                    this.probe = actorRef;
                    getSender().tell("done", getSelf());
                }).match(FirstTick.class, message -> {
                    log.info("First Tick");
                    getTimers().startPeriodicTimer(TICK_TPS_KEY, new TPSCheckTick(), Duration.ofMillis(1000));
                }).match(TPSReq.class, message -> {
                    getSender().tell(new TPSInfo(lastTps), ActorRef.noSender());
                })
                // TPS 측정기능과 Confirm기능
                .match(ConfirmEvent.class, message -> {
                    transactionCount++;
                    if (this.probe != null) {
                        probe.tell(message, ActorRef.noSender());
                    }
                }).match(String.class, message -> {
                    transactionCount++;
                }).match(TPSCheckTick.class, message -> {
                    long startTime = System.currentTimeMillis() - 1000;
                    long endTime = System.currentTimeMillis();
                    tps = transactionCount / ((endTime - startTime) / 1000);
                    if (tps > 0) {
                        lastTps = tps;
                        getTimers().startPeriodicTimer(TICK_TPSRESET_KEY, new TPSResetHalfTick(), Duration.ofMillis(500));
                    }
                    transactionCount = 0;
                    log.info("TPS:" + lastTps);
                }).match(TPSResetHalfTick.class, message -> {
                    lastTps = lastTps / 2;
                    getTimers().startPeriodicTimer(TICK_TPSRESET_KEY, new TPSResetTick(), Duration.ofMillis(500));
                }).match(TPSResetTick.class, message -> {
                    lastTps = 0;
                }).build();
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
