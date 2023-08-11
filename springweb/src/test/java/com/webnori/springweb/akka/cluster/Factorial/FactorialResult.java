package com.webnori.springweb.akka.cluster.Factorial;

import com.webnori.springweb.example.akka.actors.cluster.MySerializable;

import java.math.BigInteger;

public class FactorialResult implements MySerializable {
    private static final long serialVersionUID = 1L;
    public final int n;
    public final BigInteger factorial;

    FactorialResult(int n, BigInteger factorial) {
        this.n = n;
        this.factorial = factorial;
    }
}
