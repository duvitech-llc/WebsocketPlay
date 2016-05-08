package com.six15.myapplication;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonElement;

public class MessageData {

    @SerializedName("clientID")
    @Expose
    private String clientID;
    @SerializedName("handler")
    @Expose
    private String handler;
    @SerializedName("datatype")
    @Expose
    private String datatype;
    @SerializedName("dispname")
    @Expose
    private String dispname;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("to")
    @Expose
    private String to;
    @SerializedName("from")
    @Expose
    private String from;
    @SerializedName("payload")
    @Expose
    private JsonElement payload;
    @SerializedName("data")
    @Expose
    private Object data;
    @SerializedName("date")
    @Expose
    private String date;

    /**
     *
     * @return The clientID
     */
    public String getClientID() {
        return clientID;
    }

    /**
     *
     * @param clientID
     *            The clientID
     */
    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    /**
     *
     * @return The handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     *
     * @param handler
     *            The handler
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     *
     * @return The datatype
     */
    public String getDatatype() {
        return datatype;
    }

    /**
     *
     * @param datatype
     *            The datatype
     */
    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    /**
     *
     * @return The dispname
     */
    public String getDispname() {
        return dispname;
    }

    /**
     *
     * @param dispname
     *            The dispname
     */
    public void setDispname(String dispname) {
        this.dispname = dispname;
    }

    /**
     *
     * @return The type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     *            The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return The to
     */
    public String getTo() {
        return to;
    }

    /**
     *
     * @param to
     *            The to
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     *
     * @return The from
     */
    public String getFrom() {
        return from;
    }

    /**
     *
     * @param from
     *            The from
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     *
     * @return The payload
     */
    public JsonElement getPayload() {
        return payload;
    }

    /**
     *
     * @param payload
     *            The payload
     */
    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }

    /**
     *
     * @return The data
     */
    public Object getData() {
        return data;
    }

    /**
     *
     * @param data
     *            The data
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     *
     * @return The date
     */
    public String getDate() {
        return date;
    }

    /**
     *
     * @param date
     *            The date
     */
    public void setDate(String date) {
        this.date = date;
    }

}
