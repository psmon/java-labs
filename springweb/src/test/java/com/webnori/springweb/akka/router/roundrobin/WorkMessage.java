package com.webnori.springweb.akka.router.roundrobin;

import com.webnori.springweb.example.akka.actors.cluster.MySerializable;

public final class WorkMessage implements MySerializable {
    private static final long serialVersionUID = 1L;
    public final String payload;

    public WorkMessage(String payload) {
        this.payload = payload;
    }
}