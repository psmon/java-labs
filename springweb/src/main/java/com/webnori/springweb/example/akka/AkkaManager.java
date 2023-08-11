package com.webnori.springweb.example.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.springweb.example.akka.actors.HelloWorld;
import com.webnori.springweb.example.akka.actors.TimerActor;
import com.webnori.springweb.example.akka.actors.cluster.ClusterListener;
import lombok.Getter;

// 클래스 목적 :
// Actor시스템을 생성하고, 액터관리 Spring 디펜던시 없이 로우코드로 구현
// Spring Bean 활용시 참조 : https://www.baeldung.com/akka-with-spring
public final class AkkaManager {
    private static AkkaManager INSTANCE;
    @Getter
    private final ActorSystem actorSystem;
    private String akkaConfig;
    private String role;
    @Getter
    private ActorRef greetActor;

    @Getter
    private ActorRef routerActor;

    private AkkaManager() {

        akkaConfig = System.getenv("akka.cluster-config");
        role = System.getenv("akka.role");

        actorSystem = serverStart("ClusterSystem", akkaConfig, role);

        InitActor();
    }

    public static AkkaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AkkaManager();
        }
        return INSTANCE;
    }

    boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }

    private ActorSystem serverStart(String sysName, String clusterConfig, String role) {

        Config regularConfig = ConfigFactory.load();

        Config combined;

        Boolean isCluster = !isEmptyString(clusterConfig) || !isEmptyString(role);

        if (isCluster) {
            final Config newConfig = ConfigFactory.parseString(
                    String.format("akka.cluster.roles = [%s]", role)).withFallback(
                    ConfigFactory.load(clusterConfig));
            combined = newConfig
                    .withFallback(regularConfig);
        } else {
            final Config newConfig = ConfigFactory.parseString(
                    String.format("akka.cluster.roles = [%s]", "seed")).withFallback(
                    ConfigFactory.load("cluster"));
            combined = newConfig
                    .withFallback(regularConfig);
        }

        ActorSystem serverSystem = ActorSystem.create(sysName, combined);
        serverSystem.actorOf(Props.create(ClusterListener.class), "clusterListener");
        return serverSystem;
    }

    private void InitActor() {
        // Create Some Actor
        greetActor = actorSystem.actorOf(HelloWorld.Props()
                .withDispatcher("my-dispatcher"), "HelloWorld");

        // Create Router Actor
        routerActor = actorSystem.actorOf(new RoundRobinPool(5)
                .props(HelloWorld.Props()), "roundRobinPool");

        actorSystem.actorOf(TimerActor.Props()
                .withDispatcher("my-blocking-dispatcher"), "TimerActor");

    }

}
