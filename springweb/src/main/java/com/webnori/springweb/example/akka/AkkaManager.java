package com.webnori.springweb.example.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

public final class AkkaManager {

    private static AkkaManager INSTANCE;


    private final ActorSystem actorSystem;

    public ActorSystem getActorSystem(){
        return actorSystem;
    }

    private final ActorRef greetActor;

    public  ActorRef getGreetActor(){
        return greetActor;
    }


    private AkkaManager() {

        Config myConfig = ConfigFactory.parseString("something=byCode");
        // load the normal config stack (system props,
        // then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();
        // override regular stack with myConfig
        Config combined = myConfig.withFallback(regularConfig);
        // put the result in between the overrides
        // (system props) and defaults again
        Config completeConfig = ConfigFactory.load(combined);
        // completeConfig = regularConfig + myConfig;

        // Create Actor System
        actorSystem = ActorSystem.create("akka-spring-demo", completeConfig);
        //actorSystem.logConfiguration();

        // Create Some Actor
        greetActor = actorSystem.actorOf(HelloWorld.Props().withDispatcher("my-dispatcher") , "HelloWorld");

    }


    public static AkkaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AkkaManager();
        }

        return INSTANCE;
    }

}
