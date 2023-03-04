package com.webnori.springweb.example.restservice;

import java.util.concurrent.atomic.AtomicLong;

import akka.actor.ActorRef;
import com.webnori.springweb.AppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private AppConfiguration appConfiguration;

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {

        String testMessage = String.format(template, name);

        appConfiguration.actorSystem().actorSelection("/user/HelloWorld").tell(testMessage, ActorRef.noSender());

        return new Greeting(counter.incrementAndGet(), testMessage );
    }
}