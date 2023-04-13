package com.webnori.springweb.example.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

public final class AkkaManager {

    private static AkkaManager INSTANCE;

    @Getter
    private final ActorSystem actorSystem;

    @Getter
    private final ActorRef greetActor;


    private AkkaManager() {

        Config myConfig = ConfigFactory.parseString("something=byCode");
        // load the normal config stack (system props,
        // then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();

        Config testConfig = ConfigFactory.load("test.conf");

        // override regular stack with myConfig
        Config combined = myConfig.withFallback(testConfig).withFallback(regularConfig);
        // put the result in between the overrides
        // (system props) and defaults again
        Config completeConfig = ConfigFactory.load(combined);
        // completeConfig = regularConfig + myConfig + testConfig;

        // Create Actor System
        actorSystem = ActorSystem.create("akka-spring-demo", completeConfig);
        //actorSystem.logConfiguration();

        // Create Some Actor
        greetActor = actorSystem.actorOf(HelloWorld.Props().withDispatcher("my-dispatcher") , "HelloWorld");

        actorSystem.actorOf(TimerActor.Props(),"TimerActor");

    }

    public static AkkaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AkkaManager();
        }

        return INSTANCE;
    }

}
