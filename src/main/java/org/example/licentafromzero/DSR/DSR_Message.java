package org.example.licentafromzero.DSR;

import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.util.ArrayList;

public class DSR_Message extends Message {
    private long requestId;
    private ArrayList<Integer> routeRecord = new ArrayList<>();
    private ArrayList<Integer> finalRoute = new ArrayList<>();
    private int ttl=100;
    private Integer finalDestination;

    //for DSR_RREQ
    public DSR_Message(int source, int destination, int finalDestination ,MessageType messageType, boolean isMulticast, long requestId) {
        super(source, destination, messageType, isMulticast);
        this.requestId = requestId;
        this.routeRecord = new ArrayList<>();
        this.finalRoute = new ArrayList<>();
        this.finalDestination = finalDestination;
    }

    //for DSR_TEXT
    public DSR_Message(int source, int destination, MessageType messageType, ArrayList<Integer> routeRecord, String text) {
        super(source, destination, messageType, false);
        this.routeRecord = routeRecord;
        setText(text);
        this.finalDestination = source; //finalDestination stores the initial source. Because lazy
    }

    //for DSR_RREP
    public DSR_Message(int source, int destination, MessageType messageType, ArrayList<Integer> routeRecord, ArrayList<Integer> finalRoute) {
        super(source, destination, messageType, false);
        this.ttl = 10;
        this.routeRecord = routeRecord;
        this.finalRoute = finalRoute;
    }

    // for DSR_RERR
    public DSR_Message(int source, int destination, MessageType messageType, Integer node1, Integer node2,  long requestId) {
        super(source, destination, messageType, true);
        this.requestId = requestId;
        ArrayList<Integer> brokenLink = new ArrayList<>();
        brokenLink.add(node1);
        brokenLink.add(node2);
        this.finalRoute = brokenLink;
    }

    //for Clone
    public DSR_Message(DSR_Message other) {
        super(other);

        this.requestId = other.requestId;
        this.ttl = other.ttl;

        this.routeRecord = new ArrayList<>(other.routeRecord);
        this.finalRoute = new ArrayList<>(other.finalRoute);
        this.finalDestination = other.finalDestination;
    }


    @Override
    public Message copy() {
        return new DSR_Message(this);
    }


    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public ArrayList<Integer> getRouteRecord() {
        return routeRecord;
    }

    public void setRouteRecord(ArrayList<Integer> routeRecord) {
        this.routeRecord = routeRecord;
    }

    public void addToRouteRecord(Integer id){
        this.routeRecord.add(id);
        this.ttl--;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public ArrayList<Integer> getFinalRoute() {
        return finalRoute;
    }

    public void setFinalRoute(ArrayList<Integer> finalRoute) {
        this.finalRoute = finalRoute;
    }

    public Integer getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(Integer finalDestination) {
        this.finalDestination = finalDestination;
    }


    @Override
    public String toString() {
        return "DSR_Message{" +
                "requestId=" + requestId +
                ", routeRecord=" + routeRecord +
                ", finalRoute=" + finalRoute +
                ", ttl=" + ttl +
                ", finalDestination=" + finalDestination +
                "} " + super.toString();
    }
}
