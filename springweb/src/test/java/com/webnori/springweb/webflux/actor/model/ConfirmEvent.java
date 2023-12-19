package com.webnori.springweb.webflux.actor.model;

import lombok.Data;

@Data
public class ConfirmEvent {

    private String message;

    public ConfirmEvent(String message) {
        this.message = message;
    }

}
