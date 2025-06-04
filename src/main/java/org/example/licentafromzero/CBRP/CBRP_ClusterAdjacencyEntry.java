package org.example.licentafromzero.CBRP;

public class CBRP_ClusterAdjacencyEntry {
    private int gateway; // Gateway node to reach the adjacent cluster
    private int linkStatus; // LINK_BIDIRECTIONAL, LINK_FROM, LINK_TO
    private long lastUpdated; // Timestamp when this entry was last updated

    public CBRP_ClusterAdjacencyEntry(int gateway, int linkStatus, long lastUpdated) {
        this.gateway = gateway;
        this.linkStatus = linkStatus;
        this.lastUpdated = lastUpdated;
    }

    public int getGateway() {
        return gateway;
    }

    public void setGateway(int gateway) {
        this.gateway = gateway;
    }

    public int getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(int linkStatus) {
        this.linkStatus = linkStatus;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "CBRP_ClusterAdjacencyEntry{" +
                "gateway=" + gateway +
                ", linkStatus=" + linkStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}