package com.webnori.springweb.example.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.webnori.springweb.example.akka.models.FakeSlowMode;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

// https://doc.akka.io/docs/akka/current/index-actors.html  - Classic Actor

public class HelloWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ActorRef probe;

    private boolean isBlockForTest = false;

    private Long blockTime;

    private int receivedCount = 0;

    public static Props Props() {
        return Props.create(HelloWorld.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(ActorRef.class, actorRef -> {
            this.probe = actorRef;
            getSender().tell("done", getSelf());
        })
        .match(FakeSlowMode.class, message -> {
            isBlockForTest = true;
            blockTime = message.bockTime;
            log.info("Switch SlowMode:{}", self().path());
        })
        .match(String.class, s -> {
            receivedCount++;

            if(receivedCount % 50 == 0){
                //log.info("Received:{} Count:{}", s, receivedCount);
            }

            if (isBlockForTest) Thread.sleep(blockTime);
            if (probe != null) {
                probe.tell("world", this.context().self());
                //log.info("Received String message: {}", s);
            } else {
                //log.info("Received String message: {}", s);
            }
        })
        .matchAny(o -> log.info("received unknown message"))
        .build();
    }
}