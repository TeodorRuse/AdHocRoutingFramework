package org.example.licentafromzero.CBRP;

public class CBRP_RoutingTableEntry {
    public int destinationId;
    public int destinationClusterId;
    public int nextHop;

    public CBRP_RoutingTableEntry(int destinationId, int destinationClusterId, int nextHop) {
        this.destinationId = destinationId;
        this.nextHop = nextHop;
        this.destinationClusterId = destinationClusterId;
    }

    @Override
    public String toString() {
        return "CBRP_RoutingTableEntry{" +
                "destinationId=" + destinationId +
                ", destinationClusterId=" + destinationClusterId +
                ", nextHop=" + nextHop +
                '}';
    }
}
