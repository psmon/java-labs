package com.webnori.springweb.akka.cluster.Factorial;

import com.webnori.springweb.example.akka.actors.cluster.MySerializable;

public class FactorialRequest implements MySerializable {
    private static final long serialVersionUID = 1L;
    public final Integer upToN;

    public FactorialRequest(int upToN) {
        this.upToN = upToN;
    }
}