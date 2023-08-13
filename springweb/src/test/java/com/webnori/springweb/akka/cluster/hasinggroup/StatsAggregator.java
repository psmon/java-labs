package com.webnori.springweb.akka.cluster.hasinggroup;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StatsAggregator extends AbstractActor {

    final int expectedResults;
    final ActorRef replyTo;
    final List<Integer> results = new ArrayList<Integer>();

    public StatsAggregator(int expectedResults, ActorRef replyTo) {
        this.expectedResults = expectedResults;
        this.replyTo = replyTo;
    }

    @Override
    public void preStart() {
        getContext().setReceiveTimeout(Duration.ofSeconds(3));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        Integer.class,
                        wordCount -> {
                            results.add(wordCount);
                            if (results.size() == expectedResults) {
                                int sum = 0;
                                for (int c : results) {
                                    sum += c;
                                }
                                double meanWordLength = ((double) sum) / results.size();
                                replyTo.tell(new StatsMessages.StatsResult(meanWordLength), getSelf());
                                getContext().stop(getSelf());
                            }
                        })
                .match(
                        ReceiveTimeout.class,
                        x -> {
                            replyTo.tell(new StatsMessages.JobFailed("Service unavailable, try again later"), getSelf());
                            getContext().stop(getSelf());
                        })
                .build();
    }
}
