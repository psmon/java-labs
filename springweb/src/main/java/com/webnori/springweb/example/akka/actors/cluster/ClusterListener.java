package com.webnori.springweb.example.akka.actors.cluster;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ClusterListener extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());

    //subscribe to cluster changes
    @Override
    public void preStart() {
        cluster.subscribe(self(), (ClusterEvent.SubscriptionInitialStateMode) ClusterEvent.initialStateAsEvents(),
                MemberEvent.class, UnreachableMember.class);
    }

    //re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(MemberUp.class, mUp -> {
            log.info("Member is Up: {}", mUp.member());
        }).match(UnreachableMember.class, mUnreachable -> {
            log.info("Member detected as unreachable: {}", mUnreachable.member());
        }).match(MemberRemoved.class, mRemoved -> {
            log.info("Member is Removed: {}", mRemoved.member());
        }).match(MemberEvent.class, message -> {
            // ignore
        }).build();
    }
}