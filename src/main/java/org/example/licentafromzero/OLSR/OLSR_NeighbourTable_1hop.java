package org.example.licentafromzero.OLSR;

import org.example.licentafromzero.Domain.Constants;

public class OLSR_NeighbourTable_1hop {
    private int id;
    private long timeReceived;

    public OLSR_NeighbourTable_1hop(int id, long timeReceived) {
        this.id = id;
        this.timeReceived = timeReceived;
    }

    public boolean isExpired(){
        return timeReceived + Constants.OLSR_NEIGHBOR_EXPIRATION_TIME > System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimeReceived() {
        return timeReceived;
    }

    public void setTimeReceived(long timeReceived) {
        this.timeReceived = timeReceived;
    }

    public void setTimeReceivedNow() {
        this.timeReceived = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "OLSR_RoutingTable_1hop{" +
                "id=" + id +
                ", timeReceived=" + timeReceived +
                '}';
    }
}
