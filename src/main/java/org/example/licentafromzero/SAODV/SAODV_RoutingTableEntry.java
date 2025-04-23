package org.example.licentafromzero.SAODV;

import java.security.PublicKey;

public class SAODV_RoutingTableEntry {
    int destAddr;
    int nextHop;
    int destSeqNum;
    int hopCount;
    long receivedTime;
//    PublicKey destPublicKey;
    byte[] doubleSignature;

    public SAODV_RoutingTableEntry(int destAddr, int nextHop, int destSeqNum, int hopCount, long receivedTime) {
        this.destAddr = destAddr;
        this.nextHop = nextHop;
        this.destSeqNum = destSeqNum;
        this.hopCount = hopCount;
        this.receivedTime = receivedTime;
    }

    public SAODV_RoutingTableEntry(int destAddr, int nextHop, int destSeqNum, int hopCount, long receivedTime, byte[] doubleSignature) {
        this.destAddr = destAddr;
        this.nextHop = nextHop;
        this.destSeqNum = destSeqNum;
        this.hopCount = hopCount;
        this.receivedTime = receivedTime;
        this.doubleSignature = doubleSignature;
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

    public byte[] getDoubleSignature() {
        return doubleSignature;
    }

    public void setDoubleSignature(byte[] doubleSignature) {
        this.doubleSignature = doubleSignature;
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
