package com.ibnux.smsgatewaymqtt.data;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class LogMessage {
    @Id
    public long id;
    public long time;
    public String message;

    public LogMessage(){}

    public LogMessage(String msg){
        this.time = System.currentTimeMillis();
        this.message = msg;
    }
}
