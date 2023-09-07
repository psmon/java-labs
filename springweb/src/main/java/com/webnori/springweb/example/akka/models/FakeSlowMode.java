package com.webnori.springweb.example.akka.models;

public class FakeSlowMode {

    public Long bockTime;
    public FakeSlowMode(){
        bockTime = 3000L;
    }
}
