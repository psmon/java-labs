package com.webnori.springweb.akka.cluster.hasinggroup;

import akka.actor.AbstractActor;

import java.util.HashMap;
import java.util.Map;

public class StatsWorker extends AbstractActor {

    Map<String, Integer> cache = new HashMap<String, Integer>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        String.class,
                        word -> {
                            Integer length = cache.get(word);
                            if (length == null) {
                                length = word.length();
                                cache.put(word, length);
                            }
                            getSender().tell(length, getSelf());
                        })
                .build();
    }
}
