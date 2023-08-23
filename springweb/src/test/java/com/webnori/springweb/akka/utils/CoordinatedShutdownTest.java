package com.webnori.springweb.akka.utils;


import akka.Done;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.akka.utils.actor.WorkStatusActor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest
public class CoordinatedShutdownTest {
    private static Logger logger = LoggerFactory.getLogger(CoordinatedShutdownTest.class);

    private static ActorSystem actorSystem;

    private static ActorRef appActor;

    private static ActorSystem serverStart(String sysName, String config, String role) {
        final Config newConfig = ConfigFactory.parseString(
                String.format("akka.cluster.roles = [%s]", role)).withFallback(
                ConfigFactory.load(config));

        ActorSystem serverSystem = ActorSystem.create(sysName, newConfig);
        return serverSystem;
    }

    @BeforeClass
    public static void bootUp() {
        actorSystem = serverStart("ClusterSystem", "router-test", "seed");
        logger.info("========= sever loaded =========");
        appActor = actorSystem.actorOf(WorkStatusActor.Props(), "APPActor");
    }

    @AfterClass
    public static void bootDown() {

        logger.info("========= try graceful down =========");
        int retryCount = 5;
        for (int i = 0; i < retryCount; i++) {
            CoordinatedShutdown.get(actorSystem).addTask(
                    CoordinatedShutdown.PhaseBeforeServiceUnbind(), "WorkCheckTask",
                    () -> {
                        return akka.pattern.Patterns.ask(appActor, "stop", Duration.ofSeconds(1))
                                .thenApply(reply -> Done.getInstance());
                    });
        }
    }

    @Test
    public void GraceArOKTest() throws InterruptedException {
        appActor.tell("increse", ActorRef.noSender());
        appActor.tell("increse", ActorRef.noSender());
        appActor.tell("decrese", ActorRef.noSender());
        appActor.tell("decrese-exception", ActorRef.noSender());
    }

    @Test
    public void GraceNotOKTest() throws InterruptedException {
        appActor.tell("increse", ActorRef.noSender());
    }
}
