package com.webnori.springweb.example.akka.actors.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.example.akka.models.FakeSlowMode;

// https://doc.akka.io/docs/akka/current/index-actors.html  - Classic Actor

public class ClusterHelloWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private boolean isBlockForTest = false;

    Cluster cluster = Cluster.get(getContext().system());

    public static Props Props() {
        return Props.create(ClusterHelloWorld.class);
    }

    //subscribe to cluster changes
    @Override
    public void preStart() {
        cluster.subscribe(self(), (ClusterEvent.SubscriptionInitialStateMode) ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
    }

    //re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ClusterEvent.MemberUp.class, mUp -> {
            log.info("Member is Up: {}", mUp.member());
        }).match(ClusterEvent.UnreachableMember.class, mUnreachable -> {
            log.info("Member detected as unreachable: {}", mUnreachable.member());
        }).match(ClusterEvent.MemberRemoved.class, mRemoved -> {
            log.info("Member is Removed: {}", mRemoved.member());
        }).match(ClusterEvent.MemberEvent.class, message -> {
            // ignore
        }).match(String.class, s -> {
            log.info("Received String message: {} {}", s, context().self().path());
        })
        .match(TestClusterMessages.Ping.class, s -> {
            log.info("Received Ping message: {}",  context().self().path());
        })
        .build();
    }

}