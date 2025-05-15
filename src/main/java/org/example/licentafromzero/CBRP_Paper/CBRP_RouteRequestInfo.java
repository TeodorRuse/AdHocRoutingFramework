package org.example.licentafromzero.CBRP_Paper;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class CBRP_RouteRequestInfo {
    private int originalSource; // Node that originated the RREQ
    private int targetAddress; // Target node the RREQ is looking for
    private int sourceSeqNum; // Sequence number of the source
    private int destSeqNum; // Sequence number of the destination (if known)
    private int broadcastId; // Unique ID for this RREQ
    private int identification; // Unique identifier for this RREQ
    
    // List of cluster heads the RREQ has visited
    private List<Integer> clusterAddresses = new ArrayList<>();
    
    // List of neighboring cluster heads and their gateway nodes
    private List<Pair<Integer, Integer>> neighboringClusterHeadGatewayPairs = new ArrayList<>();

    public CBRP_RouteRequestInfo(int originalSource, int targetAddress, int sourceSeqNum, 
                                int destSeqNum, int broadcastId) {
        this.originalSource = originalSource;
        this.targetAddress = targetAddress;
        this.sourceSeqNum = sourceSeqNum;
        this.destSeqNum = destSeqNum;
        this.broadcastId = broadcastId;
        this.identification = originalSource * 10000 + broadcastId; // Simple way to create unique ID
    }

    // Copy constructor
    public CBRP_RouteRequestInfo(CBRP_RouteRequestInfo other) {
        this.originalSource = other.originalSource;
        this.targetAddress = other.targetAddress;
        this.sourceSeqNum = other.sourceSeqNum;
        this.destSeqNum = other.destSeqNum;
        this.broadcastId = other.broadcastId;
        this.identification = other.identification;
        
        if (other.clusterAddresses != null) {
            this.clusterAddresses = new ArrayList<>(other.clusterAddresses);
        }
        
        if (other.neighboringClusterHeadGatewayPairs != null) {
            this.neighboringClusterHeadGatewayPairs = new ArrayList<>(other.neighboringClusterHeadGatewayPairs);
        }
    }

    public void addClusterAddress(int clusterHead) {
        if (!clusterAddresses.contains(clusterHead)) {
            clusterAddresses.add(clusterHead);
        }
    }

    public void addNeighboringClusterHead(int clusterHead, int gateway) {
        Pair<Integer, Integer> pair = new Pair<>(clusterHead, gateway);
        if (!neighboringClusterHeadGatewayPairs.contains(pair)) {
            neighboringClusterHeadGatewayPairs.add(pair);
        }
    }

    public int getOriginalSource() {
        return originalSource;
    }

    public int getTargetAddress() {
        return targetAddress;
    }

    public int getSourceSeqNum() {
        return sourceSeqNum;
    }

    public int getDestSeqNum() {
        return destSeqNum;
    }

    public int getBroadcastId() {
        return broadcastId;
    }

    public int getIdentification() {
        return identification;
    }

    public List<Integer> getClusterAddresses() {
        return clusterAddresses;
    }

    public List<Pair<Integer, Integer>> getNeighboringClusterHeadGatewayPairs() {
        return neighboringClusterHeadGatewayPairs;
    }

    @Override
    public String toString() {
        return "CBRP_RouteRequestInfo{" +
                "originalSource=" + originalSource +
                ", targetAddress=" + targetAddress +
                ", sourceSeqNum=" + sourceSeqNum +
                ", destSeqNum=" + destSeqNum +
                ", broadcastId=" + broadcastId +
                ", identification=" + identification +
                ", clusterAddresses=" + clusterAddresses +
                ", neighboringClusterHeadGatewayPairs=" + neighboringClusterHeadGatewayPairs +
                '}';
    }
}