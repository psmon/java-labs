package com.webnori.springweb.example.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.RoundRobinPool;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import com.webnori.springweb.example.akka.actors.ParentActor;
import com.webnori.springweb.example.akka.actors.TimerActor;
import lombok.Getter;

// 클래스의 목적 :
// Actor를 생성을 정의하고 구조화하는 목적
// DI보다는 Hierarchy 구조적으로 액터를 구성합니다.
public final class AkkaManager {

    private static AkkaManager INSTANCE;

    @Getter
    private final ActorSystem actorSystem;

    @Getter
    private final ActorRef greetActor;

    @Getter
    private final ActorRef routerActor;

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
        greetActor = actorSystem.actorOf(HelloWorld.Props()
                .withDispatcher("my-dispatcher"), "HelloWorld");


        // Create Router Actor
        routerActor = actorSystem.actorOf(new RoundRobinPool(5)
                .props(HelloWorld.Props()), "roundRobinPool");

        actorSystem.actorOf(TimerActor.Props()
                .withDispatcher("my-blocking-dispatcher"),"TimerActor");


    }

    public static AkkaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AkkaManager();
        }

        return INSTANCE;
    }

}
