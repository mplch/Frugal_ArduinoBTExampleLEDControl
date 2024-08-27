package com.sarmale.frugalarduinobtexampleledcontrol;

/* DEFINITELY SHOULD RANAME THIS CLASS */
/* Communication, Comm, Exchange, Buffer, idk.. */

public class Exchange {

    boolean isConnected;
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
