package com.six15.myapplication;

/**
 * Created by George on 5/16/2016.
 */
public class TextMessage {

    public TextMessage(String clientId, String message){
        this.clientId = clientId;
        this.message = message;
    }

    public TextMessage(String clientId, String fname, String message){
        this.clientId = clientId;
        this.friendlyName = fname;
        this.message = message;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String clientId;
    private String friendlyName;
    private String message;


}
