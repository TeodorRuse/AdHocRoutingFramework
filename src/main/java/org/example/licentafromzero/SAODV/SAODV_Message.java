package org.example.licentafromzero.SAODV;

import org.example.licentafromzero.AODV.AODV_Message;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

public class SAODV_Message extends Message {
    private int sourceSeqNum, destSeqNum;
    private int broadcastId; //broadcastId is unique to each node. The node addr and broadcastId create a unique pair
    private long timeSent; //timeSent is used to identify out of date info
    private int hopCount;
    private int originalSource; //first node that sends it;
    private int actualSource; //if the node that sends the RREP is not the target node, and uses cached;
    private int finalDestination; //searched for address
    private ArrayList<Integer> unreachableAddresses = new ArrayList<>();
    private byte[] signature;
    private byte[] doubleSignature;
    private int signedBy;

    //for CLONE
    public SAODV_Message(SAODV_Message other){
        super(other);
        this.sourceSeqNum = other.sourceSeqNum;
        this.destSeqNum = other.destSeqNum;
        this.timeSent = other.timeSent;
        this.hopCount = other.hopCount;
        this.finalDestination = other.finalDestination;
        this.unreachableAddresses = other.unreachableAddresses;
        this.originalSource = other.originalSource;
        this.broadcastId = other.broadcastId;
        this.actualSource = other.actualSource;
        this.signature = other.signature;
        this.doubleSignature = other.doubleSignature;
        this.signedBy = other.signedBy;
    }

    @Override
    public Message copy() {
        return new SAODV_Message(this);
    }


    //for RREQ
    public SAODV_Message(int source, MessageType messageType , int finalDestination, int sourceSeqNum, int destSeqNum, int broadcastId){
        super(source, -1, messageType, true);
        this.originalSource = source;
        this.finalDestination = finalDestination;
        this.sourceSeqNum = sourceSeqNum;
        this.destSeqNum = destSeqNum;
        this.broadcastId = broadcastId;
        this.hopCount = 0;
    }

    //for RREP
    public SAODV_Message(int source, int destination, MessageType messageType, int originalSource ,int finalDestination, int destSeqNum){
        super(source, destination, messageType, false);
        this.originalSource = originalSource;
        this.finalDestination = finalDestination;
        this.destSeqNum = destSeqNum;
        this.hopCount = 0;
        this.timeSent = System.currentTimeMillis();
        this.actualSource = source;
    }

    //for RERR
    public SAODV_Message(int source, MessageType messageType, int sourceSeqNum, int broadcastId, ArrayList<Integer> unreachableAddresses){
        super(source, -1, messageType, true);
        this.originalSource = source;
        this.sourceSeqNum = sourceSeqNum;
        this.broadcastId = broadcastId;
        this.unreachableAddresses = unreachableAddresses;
    }
    //still for RERR
    public SAODV_Message(int source, MessageType messageType, int sourceSeqNum, int broadcastId, int unreachableAddress){
        super(source, -1, messageType, true);
        this.originalSource = source;
        this.sourceSeqNum = sourceSeqNum;
        this.broadcastId = broadcastId;
        ArrayList<Integer> unreachableAddresses = new ArrayList<>();
        unreachableAddresses.add(unreachableAddress);
        this.unreachableAddresses = unreachableAddresses;
    }

    //for TEXT
    public SAODV_Message(int source, int destination, MessageType messageType, int finalDestination, String text){
        super(source, destination, text, messageType, false);
        this.finalDestination = finalDestination;
    }

    private String getSignedData() {
        return this.toString();
    }

    public void signMessage(int id, PrivateKey privateKey){
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(getSignedData().getBytes());
            this.signature = signer.sign();
            this.signedBy = id;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Verify the message with the sender's public key
    public boolean verifySignature(PublicKey publicKey){
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(getSignedData().getBytes());
            return verifier.verify(this.signature);
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    // Optional: helper to print the signature
    public String getSignatureBase64() {
        return Base64.getEncoder().encodeToString(signature);
    }

    public int getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(int originalSource) {
        this.originalSource = originalSource;
    }

    public int getSourceSeqNum() {
        return sourceSeqNum;
    }

    public void setSourceSeqNum(int sourceSeqNum) {
        this.sourceSeqNum = sourceSeqNum;
    }

    public int getDestSeqNum() {
        return destSeqNum;
    }

    public void setDestSeqNum(int destSeqNum) {
        this.destSeqNum = destSeqNum;
    }

    public int getBroadcastId() {
        return broadcastId;
    }

    public void setBroadcastId(int broadcastId) {
        this.broadcastId = broadcastId;
    }

    public long getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public int getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(int finalDestination) {
        this.finalDestination = finalDestination;
    }

    public ArrayList<Integer> getUnreachableAddresses() {
        return unreachableAddresses;
    }

    public int getActualSource() {
        return actualSource;
    }

    public void setActualSource(int actualSource) {
        this.actualSource = actualSource;
    }

    public void setUnreachableAddresses(ArrayList<Integer> unreachableAddresses) {
        this.unreachableAddresses = unreachableAddresses;
    }
    public void increaseHopCount(){
        hopCount++;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getDoubleSignature() {
        return doubleSignature;
    }

    public void setDoubleSignature(byte[] doubleSignature) {
        this.doubleSignature = doubleSignature;
    }



    @Override
    public String toString() {
        return "SAODV_Message | " +
                "origSrc=" + originalSource + " | " +
                "actualSrc=" + actualSource + " | " +
                "finalDst=" + finalDestination + " | " +
                "srcSeq=" + sourceSeqNum + " | " +
                "dstSeq=" + destSeqNum + " | " +
                "bcastID=" + broadcastId + " | " +
                "sent=" + timeSent + " | " +
                "unreach=" + unreachableAddresses + " | " +
                "text='" + text + "' | " +
                "signedBy=" + signedBy;
    }
}
