package com.webnori.springweb.example.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.routing.RoundRobinPool;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import com.webnori.springweb.example.akka.actors.TimerActor;
import lombok.Getter;

// 클래스 목적 :
// Actor시스템을 생성하고, 액터관리 Spring 디펜던시가 없이 Low코드로 구현
// Spring Bean활용시 다음 링크 참조 : https://www.baeldung.com/akka-with-spring
public final class AkkaManager {
    private String hostname;

    private String role;

    private String port;

    private String seedNodes;

    private static AkkaManager INSTANCE;

    @Getter
    private final ActorSystem actorSystem;

    @Getter
    private ActorRef greetActor;

    @Getter
    private ActorRef routerActor;

    boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }

    private AkkaManager() {

        hostname = System.getenv("akka.hostname");
        role = System.getenv("akka.role");
        port = System.getenv("akka.port");
        seedNodes = System.getenv("akka.seed-nodes");

        Boolean isStandAlone = isEmptyString(System.getenv("akka.hostname")) ||
                isEmptyString(System.getenv("akka.role")) || isEmptyString(System.getenv("akka.port")) ||
                isEmptyString(System.getenv("akka.seed-nodes"));

        String minConfig = String.format("akka.remote.artery.canonical.hostname = \"%s\" \n " +
        "akka.remote.artery.canonical.port = %s \n" +
        "akka.cluster.roles = [%s] \n " +
        "akka.cluster.seed-nodes = %s \n ", hostname,port,role,seedNodes);

        final Config clusterConfig = ConfigFactory.parseString(
                minConfig).withFallback(
                ConfigFactory.load("application"));


        Config myConfig = ConfigFactory.parseString("something=byCode");
        // load the normal config stack (system props,
        // then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();

        Config testConfig = ConfigFactory.load("test.conf");

        Config combined;

        if(isStandAlone){
            combined = myConfig
                    .withFallback(testConfig)
                    .withFallback(regularConfig);
        }
        else{
            combined = myConfig
                    .withFallback(testConfig)
                    .withFallback(clusterConfig)
                    .withFallback(regularConfig);
        }


        // put the result in between the overrides
        // (system props) and defaults again
        Config completeConfig = ConfigFactory.load(combined);
        // completeConfig = regularConfig(file) + clusterConfig(env) + testConfig;

        // Create Actor System
        actorSystem = ActorSystem.create("akka-spring-demo", completeConfig);
        //actorSystem.logConfiguration();


    }

    private void InitActor(){
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
