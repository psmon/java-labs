package com.webnori.lighthouse.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.webnori.lighthouse.akka.cluster.ClusterListener;
import lombok.Getter;


public final class AkkaManager {
    private static AkkaManager INSTANCE;
    @Getter
    private final ActorSystem actorSystem;
    private String akkaConfig;
    private String role;

    private String hostname;

    private String hostport;

    private String seed;


    @Getter
    private ActorRef greetActor;

    @Getter
    private ActorRef routerActor;

    private AkkaManager() {

        akkaConfig = System.getenv("akka.cluster-config");
        role = System.getenv("akka.role");
        hostname = System.getenv("akka.hostname");
        hostport = System.getenv("akka.hostport");
        seed = System.getenv("akka.seed");

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

        Boolean isCluster = !isEmptyString(clusterConfig) || !isEmptyString(role) || !isEmptyString(hostname)
                || !isEmptyString(hostport) || !isEmptyString(seed);

        if (isCluster) {
            Config newConfig = ConfigFactory.parseString(
                    String.format("akka.cluster.roles = [%s]", role)).withFallback(
                    ConfigFactory.load(clusterConfig));

            newConfig = ConfigFactory.parseString(
                    String.format("akka.cluster.seed-nodes  = [\"%s\"] ", seed)).withFallback(
                    ConfigFactory.load(newConfig));

            newConfig = ConfigFactory.parseString(
                    String.format("akka.remote.artery.canonical.hostname = \"%s\" ", hostname)).withFallback(
                    ConfigFactory.load(newConfig));

            newConfig = ConfigFactory.parseString(
                    String.format("akka.remote.artery.canonical.port = %s ", hostport)).withFallback(
                    ConfigFactory.load(newConfig));

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

    }

}
