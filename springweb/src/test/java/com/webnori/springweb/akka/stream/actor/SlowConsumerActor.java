package com.webnori.springweb.akka.stream.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.example.akka.models.FakeSlowMode;

import java.util.Random;

import static java.lang.Thread.sleep;

public class SlowConsumerActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props Props() {
        return Props.create(SlowConsumerActor.class);
    }

    public static Props Props(String dispatcher) {
        return Props.create(SlowConsumerActor.class).withDispatcher(dispatcher);
    }


    private ActorRef probe;

    private ActorRef tpsActor;

    private double currentTps;

    private long totalProcessCount = 0;


    @Override
    public Receive createReceive() {

        tpsActor = context().actorOf(TPSActor.Props(), "tpsActor");
        tpsActor.tell(self(), ActorRef.noSender());

        return receiveBuilder()
        .match(TPSInfo.class, tps -> {
            currentTps = tps.tps;
        })
        .match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        })
        .match(String.class, s -> {
            tpsActor.tell("SomeEvent", ActorRef.noSender());

            long sleepValue = 0;
            if( (currentTps > 50) && totalProcessCount % 10 == 0){
                sleepValue =(long)currentTps;
                sleep(sleepValue);
            }

            totalProcessCount++;
            //log.info("World - Total:{} Sleep:{}", totalProcessCount, sleepValue);
            probe.tell("world", ActorRef.noSender());
        })
        .matchAny(o -> log.info("received unknown message")).build();
    }

}
