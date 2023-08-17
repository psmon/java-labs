package com.webnori.springweb.akka.utils;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class WorkStatsActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private boolean isAtLeastOnceLog;

    private int remainWork;

    private int errorCount;

    public static Props Props() {
        return Props.create(WorkStatsActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        String.class,
                        message -> {

                            switch (message){
                                case "stop":{
                                    if (remainWork == 0) {
                                        sender().tell(Done.getInstance(), ActorRef.noSender());

                                        if(!isAtLeastOnceLog){
                                            log.info("=== Grace Ful Down === Works:{},Errors:{}",remainWork,errorCount);
                                        }
                                        isAtLeastOnceLog = true;
                                    }
                                }
                                break;
                                case "increse":{
                                    remainWork++;
                                }
                                break;
                                case "decrese":{
                                    if(remainWork>0)  remainWork--;
                                }
                                break;
                                case "decrese-exception":{
                                    if(remainWork>0)  remainWork--;
                                    errorCount++;
                                }
                                break;
                            }
                        })
                .build();
    }
}
