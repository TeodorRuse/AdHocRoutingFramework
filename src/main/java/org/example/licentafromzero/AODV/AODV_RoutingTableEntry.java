package org.example.licentafromzero.AODV;

public class AODV_RoutingTableEntry {
    int destAddr;
    int nextHop;
    int destSeqNum;
    int hopCount;
    long receivedTime;

    public AODV_RoutingTableEntry(int destAddr, int nextHop, int destSeqNum, int hopCount) {
        this.destAddr = destAddr;
        this.nextHop = nextHop;
        this.destSeqNum = destSeqNum;
        this.hopCount = hopCount;
        this.receivedTime = System.currentTimeMillis();
    }

    public int getDestAddr() {
        return destAddr;
    }

    public void setDestAddr(int destAddr) {
        this.destAddr = destAddr;
    }

    public int getNextHop() {
        return nextHop;
    }

    public void setNextHop(int nextHop) {
        this.nextHop = nextHop;
    }

    public int getDestSeqNum() {
        return destSeqNum;
    }

    public void setDestSeqNum(int destSeqNum) {
        this.destSeqNum = destSeqNum;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public long getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(long receivedTime) {
        this.receivedTime = receivedTime;
    }

    @Override
    public String toString() {
        return "AODV_RoutingTableEntry{" +
                "destAddr=" + destAddr +
                ", nextHop=" + nextHop +
                ", destSeqNum=" + destSeqNum +
                ", hopCount=" + hopCount +
                ", receivedTime=" + receivedTime +
                '}';
    }
}
