package org.example.licentafromzero.OLSR;

import org.example.licentafromzero.Domain.Constants;

public class OLSR_RoutingTableEntry {
    private int destination, nextHop, hopCount;
    private long timeReceived;

    public OLSR_RoutingTableEntry(int destination, int nextHop, int hopCount) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.hopCount = hopCount;
        this.timeReceived = System.currentTimeMillis();
    }

    public boolean isExpired(){
        return timeReceived + Constants.OLSR_NEIGHBOR_EXPIRATION_TIME >  System.currentTimeMillis();
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public int getNextHop() {
        return nextHop;
    }

    public void setNextHop(int nextHop) {
        this.nextHop = nextHop;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public long getTimeReceived() {
        return timeReceived;
    }

    public void setTimeReceived(long timeReceived) {
        this.timeReceived = timeReceived;
    }
}
