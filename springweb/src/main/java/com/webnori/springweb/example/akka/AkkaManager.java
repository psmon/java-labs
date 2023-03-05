package com.webnori.springweb.example.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.Builder;
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

        // Create Actor System
        actorSystem = ActorSystem.create("akka-spring-demo");

        // Create Some Actor
        greetActor = actorSystem.actorOf(HelloWorld.Props(), "HelloWorld");

    }

    public static AkkaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AkkaManager();
        }

        return INSTANCE;
    }

}
