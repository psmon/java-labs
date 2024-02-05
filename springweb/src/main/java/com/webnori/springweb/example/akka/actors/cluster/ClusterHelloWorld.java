package com.webnori.springweb.example.akka.actors.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.webnori.springweb.example.akka.models.FakeSlowMode;

public class ClusterHelloWorld extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

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
        // 구현가능 분산처리 메시지 사용자 정의부분
        }).match(String.class, s -> {
            log.info("Received String message: {} {}", s, context().self().path());
        })
        .match(TestClusterMessages.Ping.class, s -> {
            log.info("Received Ping message: {}",  context().self().path());
        })
        .build();
    }
}