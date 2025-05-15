package org.example.licentafromzero.CBRP_Paper;

import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.util.ArrayList;
import java.util.List;

public class CBRP_Message extends Message {
    private int finalDestination; // For source-routed packets
    private List<Integer> sourceRoute; // For source-routed packets
    private boolean repaired = false; // Flag for local repair
    private boolean shortened = false; // Flag for route shortening
    private int originalSource; // Original sender of the message
    
    // Route request specific fields
    private CBRP_RouteRequestInfo routeRequestInfo;
    
    // Route reply specific fields
    private CBRP_RouteReplyInfo routeReplyInfo;
    
    // Route error specific fields
    private CBRP_RouteErrorInfo routeErrorInfo;

    // Constructor for HELLO messages
    public CBRP_Message(int source, int destination, String text, MessageType messageType, boolean isMulticast) {
        super(source, destination, text, messageType, isMulticast);
        this.originalSource = source;
    }

    // Constructor for TEXT messages with source routing
    public CBRP_Message(int source, int destination, String text, MessageType messageType, boolean isMulticast, 
                        int finalDestination, List<Integer> sourceRoute) {
        super(source, destination, text, messageType, isMulticast);
        this.finalDestination = finalDestination;
        this.sourceRoute = new ArrayList<>(sourceRoute);
        this.originalSource = source;
    }

    // Copy constructor
    public CBRP_Message(CBRP_Message other) {
        super(other);
        this.finalDestination = other.finalDestination;
        this.repaired = other.repaired;
        this.shortened = other.shortened;
        this.originalSource = other.originalSource;
        
        if (other.sourceRoute != null) {
            this.sourceRoute = new ArrayList<>(other.sourceRoute);
        }
        
        if (other.routeRequestInfo != null) {
            this.routeRequestInfo = new CBRP_RouteRequestInfo(other.routeRequestInfo);
        }
        
        if (other.routeReplyInfo != null) {
            this.routeReplyInfo = new CBRP_RouteReplyInfo(other.routeReplyInfo);
        }
        
        if (other.routeErrorInfo != null) {
            this.routeErrorInfo = new CBRP_RouteErrorInfo(other.routeErrorInfo);
        }
    }

    @Override
    public Message copy() {
        return new CBRP_Message(this);
    }

    public int getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(int finalDestination) {
        this.finalDestination = finalDestination;
    }

    public List<Integer> getSourceRoute() {
        return sourceRoute;
    }

    public void setSourceRoute(List<Integer> sourceRoute) {
        this.sourceRoute = sourceRoute;
    }

    public boolean isRepaired() {
        return repaired;
    }

    public void setRepaired(boolean repaired) {
        this.repaired = repaired;
    }

    public boolean isShortened() {
        return shortened;
    }

    public void setShortened(boolean shortened) {
        this.shortened = shortened;
    }

    public int getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(int originalSource) {
        this.originalSource = originalSource;
    }

    public CBRP_RouteRequestInfo getRouteRequestInfo() {
        return routeRequestInfo;
    }

    public void setRouteRequestInfo(CBRP_RouteRequestInfo routeRequestInfo) {
        this.routeRequestInfo = routeRequestInfo;
    }

    public CBRP_RouteReplyInfo getRouteReplyInfo() {
        return routeReplyInfo;
    }

    public void setRouteReplyInfo(CBRP_RouteReplyInfo routeReplyInfo) {
        this.routeReplyInfo = routeReplyInfo;
    }

    public CBRP_RouteErrorInfo getRouteErrorInfo() {
        return routeErrorInfo;
    }

    public void setRouteErrorInfo(CBRP_RouteErrorInfo routeErrorInfo) {
        this.routeErrorInfo = routeErrorInfo;
    }

    @Override
    public String toString() {
        return "CBRP_Message{" +
                "source=" + source +
                ", destination=" + destination +
                ", finalDestination=" + finalDestination +
                ", messageType=" + messageType +
                ", text='" + text + '\'' +
                ", isSuccessful=" + isSuccessful +
                ", repaired=" + repaired +
                ", shortened=" + shortened +
                '}';
    }
}