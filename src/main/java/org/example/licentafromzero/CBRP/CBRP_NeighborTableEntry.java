package org.example.licentafromzero.CBRP;

public class CBRP_NeighborTableEntry {
    private int neighborId;
    private int linkStatus; // LINK_BIDIRECTIONAL, LINK_FROM, LINK_TO
    private int role; // C_UNDECIDED, C_HEAD, C_MEMBER
    private long lastHeard; // Timestamp when last heard from this neighbor

    public CBRP_NeighborTableEntry(int neighborId, int linkStatus, int role) {
        this.neighborId = neighborId;
        this.linkStatus = linkStatus;
        this.role = role;
        this.lastHeard = System.currentTimeMillis();
    }

    public int getNeighborId() {
        return neighborId;
    }

    public void setNeighborId(int neighborId) {
        this.neighborId = neighborId;
    }

    public int getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(int linkStatus) {
        this.linkStatus = linkStatus;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public long getLastHeard() {
        return lastHeard;
    }

    public void setLastHeard(long lastHeard) {
        this.lastHeard = lastHeard;
    }

    public boolean isExpired(long currentTime, long timeout) {
        return currentTime - lastHeard > timeout;
    }

    @Override
    public String toString() {
        return "CBRP_NeighborTableEntry{" +
                "neighborId=" + neighborId +
                ", linkStatus=" + linkStatus +
                ", role=" + role +
                ", lastHeard=" + lastHeard +
                '}';
    }
}