package com.webnori.springweb.akka.stream.actor.model;

import java.util.Objects;

public class TPSInfo {
    public double tps;

    public TPSInfo(double tps){
        this.tps = tps;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TPSInfo tpsInfo = (TPSInfo) obj;
        return tps == tpsInfo.tps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tps);
    }
}
