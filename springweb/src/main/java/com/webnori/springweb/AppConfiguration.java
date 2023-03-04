package com.webnori.springweb;

import akka.actor.ActorSystem;
import com.webnori.springweb.example.akka.HelloWorld;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class AppConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("akka-spring-demo");
        system.actorOf(HelloWorld.Props(),"HelloWorld");
        return system;
    }
}