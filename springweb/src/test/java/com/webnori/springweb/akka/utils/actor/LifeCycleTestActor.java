package com.webnori.springweb.akka.utils.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class LifeCycleTestActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public int workCountForTest;

    private ActorRef probe;

    public static Props Props() {
        return Props.create(LifeCycleTestActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, message -> {
                    switch (message) {
                        case "someWork": {
                            workCountForTest++;
                            Thread.sleep(500);
                            log.info("WorkCount:{}", workCountForTest);

                            if(workCountForTest ==10){
                                probe.tell("done", ActorRef.noSender());
                            }
                        }
                    }
                })
                .match(ActorRef.class, actorRef -> {
                    this.probe = actorRef;
                })
                .build();
    }
}
