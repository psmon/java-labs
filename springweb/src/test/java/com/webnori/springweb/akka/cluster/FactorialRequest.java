package com.webnori.springweb.akka.cluster;

import java.io.Serializable;

public class FactorialRequest implements MySerializable {
    private static final long serialVersionUID = 1L;
    public final Integer upToN;

    public FactorialRequest(int upToN) {
        this.upToN = upToN;
    }
}