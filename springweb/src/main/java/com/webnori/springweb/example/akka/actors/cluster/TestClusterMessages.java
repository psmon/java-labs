package com.webnori.springweb.example.akka.actors.cluster;

public class TestClusterMessages implements MySerializable {

    public static class Ping implements MySerializable {}

    public static Ping ping() {
        return new Ping();
    }

}
