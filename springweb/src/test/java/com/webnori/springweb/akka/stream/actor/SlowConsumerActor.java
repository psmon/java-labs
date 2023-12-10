package com.webnori.springweb.akka.stream.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.webnori.springweb.akka.stream.actor.model.TPSInfo;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.time.Duration;

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

    private long totalProcessCount = 0;

    //Anything above this value will slow down the consumer.
    private int tpsLimit = 400;

    @Override
    public Receive createReceive() {

        tpsActor = context().actorOf(TpsMeasurementActor.Props(), "tpsActor");
        tpsActor.tell(self(), ActorRef.noSender());

        return receiveBuilder()
        .match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        })
        .match(String.class, s -> {
            tpsActor.tell("SomeEvent", ActorRef.noSender());
            long sleepValue = 0;

            // tps measurement
            Timeout timeout = Timeout.create(Duration.ofSeconds(1));
            Future<Object> future = Patterns.ask(tpsActor, "tps", timeout);
            TPSInfo result = (TPSInfo)Await.result(future, timeout.duration());

            if( result.tps > tpsLimit ){
                sleepValue =(long)result.tps;
                sleep(sleepValue);
                log.info("World Slow - Total:{} Sleep:{}", totalProcessCount, sleepValue);
            }

            totalProcessCount++;
            probe.tell("world", ActorRef.noSender());
        })
        .matchAny(o -> log.info("received unknown message")).build();
    }
}
