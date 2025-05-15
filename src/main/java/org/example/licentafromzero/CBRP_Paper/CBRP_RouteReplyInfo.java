package org.example.licentafromzero.CBRP_Paper;

import java.util.ArrayList;
import java.util.List;

public class CBRP_RouteReplyInfo {
    private int identification; // Copied from the corresponding RREQ
    private boolean gratuitous; // Flag indicating if this is a gratuitous RREP
    private int finalDestination; // Final destination for this RREP (original RREQ source)
    
    // List of cluster heads to traverse to reach the destination
    private List<Integer> clusterAddresses = new ArrayList<>();
    
    // Calculated hop-by-hop route
    private List<Integer> calculatedRoute = new ArrayList<>();

    public CBRP_RouteReplyInfo(int identification, boolean gratuitous, int finalDestination) {
        this.identification = identification;
        this.gratuitous = gratuitous;
        this.finalDestination = finalDestination;
    }

    // Copy constructor
    public CBRP_RouteReplyInfo(CBRP_RouteReplyInfo other) {
        this.identification = other.identification;
        this.gratuitous = other.gratuitous;
        this.finalDestination = other.finalDestination;
        
        if (other.clusterAddresses != null) {
            this.clusterAddresses = new ArrayList<>(other.clusterAddresses);
        }
        
        if (other.calculatedRoute != null) {
            this.calculatedRoute = new ArrayList<>(other.calculatedRoute);
        }
    }

    public void addToCalculatedRoute(int nodeId) {
        calculatedRoute.add(nodeId);
    }

    public void removeFirstClusterAddress() {
        if (!clusterAddresses.isEmpty()) {
            clusterAddresses.remove(0);
        }
    }

    public int getIdentification() {
        return identification;
    }

    public boolean isGratuitous() {
        return gratuitous;
    }

    public int getFinalDestination() {
        return finalDestination;
    }

    public List<Integer> getClusterAddresses() {
        return clusterAddresses;
    }

    public void setClusterAddresses(List<Integer> clusterAddresses) {
        this.clusterAddresses = clusterAddresses;
    }

    public List<Integer> getCalculatedRoute() {
        return calculatedRoute;
    }

    @Override
    public String toString() {
        return "CBRP_RouteReplyInfo{" +
                "identification=" + identification +
                ", gratuitous=" + gratuitous +
                ", finalDestination=" + finalDestination +
                ", clusterAddresses=" + clusterAddresses +
                ", calculatedRoute=" + calculatedRoute +
                '}';
    }
}