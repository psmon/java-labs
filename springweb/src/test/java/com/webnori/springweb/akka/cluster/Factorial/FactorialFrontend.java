package com.webnori.springweb.akka.cluster.Factorial;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.FromConfig;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class FactorialFrontend extends AbstractActor {
    final int upToN;
    final boolean repeat;

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    ActorRef backend = getContext().actorOf(FromConfig.getInstance().props(),
            "factorialBackendRouter");

    ActorRef probe;

    public FactorialFrontend(int upToN, boolean repeat) {
        this.upToN = upToN;
        this.repeat = repeat;
    }

    @Override
    public void preStart() {
        //sendJobs();
        getContext().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FactorialRequest.class, request -> {
                    int upToN = request.upToN;
                    log.info("Starting batch of factorials up to [{}]", upToN);
                    for (Integer n = 1; n <= upToN; n++) {
                        backend.tell(n, self());
                    }
                    probe = getSender();
                })
                .match(FactorialResult.class, result -> {
                    if (result.n == upToN) {
                        log.info("FactorialResult {}! = {}", result.n, result.factorial);
                        if (repeat)
                            sendJobs();
                        else
                            getContext().stop(self());

                        //forward to probe
                        probe.tell(result, ActorRef.noSender());
                    }
                })
                .match(ReceiveTimeout.class, message -> {
                    log.info("Timeout");
                    sendJobs();
                })
                .build();
    }

    void sendJobs() {
        log.info("Starting batch of factorials up to [{}]", upToN);
        for (int n = 1; n <= upToN; n++) {
            backend.tell(n, self());
        }
    }

}