package com.webnori.springweb.akka.cluster.hasinggroup;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.ConsistentHashingRouter;
import akka.routing.FromConfig;

public class StatsService extends AbstractActor {

    // This router is used both with lookup and deploy of routees. If you
    // have a router with only lookup of routees you can use Props.empty()
    // instead of Props.create(StatsWorker.class).
    ActorRef workerRouter =
            getContext()
                    .actorOf(FromConfig.getInstance().props(Props.create(StatsWorker.class)), "workerRouter");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        StatsMessages.StatsJob.class,
                        job -> !job.getText().isEmpty(),
                        job -> {
                            String[] words = job.getText().split(" ");
                            ActorRef replyTo = getSender();

                            // create actor that collects replies from workers
                            ActorRef aggregator =
                                    getContext().actorOf(Props.create(StatsAggregator.class, words.length, replyTo));

                            // send each word to a worker
                            for (String word : words) {
                                workerRouter.tell(new ConsistentHashingRouter.ConsistentHashableEnvelope(word, word), aggregator);
                            }
                        })
                .build();
    }
}
