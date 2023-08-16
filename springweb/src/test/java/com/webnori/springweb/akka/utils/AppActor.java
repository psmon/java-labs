package com.webnori.springweb.akka.utils;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.akka.router.routing.actor.WorkMessage;
import com.webnori.springweb.akka.router.routing.actor.WorkerActor;

public class AppActor extends AbstractActor {

    public static Props Props() {
        return Props.create(AppActor.class);
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    boolean isCompletedTask;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        String.class,
                        message -> {
                            if(message.equals("stop")){
                                if(isCompletedTask){
                                    sender().tell(Done.getInstance(), ActorRef.noSender());
                                    log.info("=== Grace Ful Down ===");
                                }
                            }
                            if(message.equals("completed")){
                                isCompletedTask = true;
                            }
                        })
                .build();
    }
}
