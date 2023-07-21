package com.webnori.springweb.akka;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.webnori.springweb.example.akka.AkkaManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.scalatestplus.junit.JUnitSuite;


/**
 * Base class for all runnable example tests written in Java
 */
abstract class AbstractJavaTest extends JUnitSuite {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = AkkaManager.getInstance().getActorSystem();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

}