package com.webnori.springweb.example.akka.actors.cluster;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

public class ClusterHelloStatsWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    Map<String, Integer> cache = new HashMap<String, Integer>();
    Cluster cluster = Cluster.get(getContext().system());

    public static Props Props() {
        return Props.create(ClusterHelloStatsWorld.class);
    }

    //subscribe to cluster changes
    @Override
    public void preStart() {
        cluster.subscribe(self(), (ClusterEvent.SubscriptionInitialStateMode) ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(String.class, word -> {
            log.info("Received String message: {} {}", word, context().self().path());
            // HashKey를 특정 클러스터드가 담당하는지 분산처리 코드
            Integer count = cache.get(word);
            if (count == null) {
                count = 1;
                cache.put(word, count);
            } else {
                Integer incCount = count + 1;
                cache.put(word, incCount);
            }
        })
        .build();
    }
}