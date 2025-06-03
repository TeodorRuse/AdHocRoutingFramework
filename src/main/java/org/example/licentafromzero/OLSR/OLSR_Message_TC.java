package org.example.licentafromzero.OLSR;

import javafx.util.Pair;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.util.ArrayList;
import java.util.HashMap;

public class OLSR_Message_TC extends Message {

    private int originalSource;
    private Pair<Integer, Integer> msgId; //<sourceID, seqNum>
    private ArrayList<Integer> advertisedNodes; //<id, 1h_neighbours[]>

    public OLSR_Message_TC(OLSR_Message_TC message) {
        super(message);
        this.msgId = message.getMsgId();
        this.originalSource = message.getOriginalSource();
        this.advertisedNodes = message.getAdvertisedNodes();
    }

    @Override
    public Message copy() {
        return new OLSR_Message_TC(this);
    }

    public OLSR_Message_TC(int source, int destination, Pair<Integer, Integer> msgId, ArrayList<Integer> advertisedNodes) {
        super(source, destination, MessageType.OLSR_TC, false);
        this.msgId = msgId;
        this.originalSource = source;
        this.advertisedNodes = advertisedNodes;
    }

    public Pair<Integer, Integer> getMsgId() {
        return msgId;
    }

    public void setMsgId(Pair<Integer, Integer> seqNum) {
        this.msgId = seqNum;
    }

    public ArrayList<Integer> getAdvertisedNodes() {
        return advertisedNodes;
    }

    public void setAdvertisedNodes(ArrayList<Integer> advertisedNodes) {
        this.advertisedNodes = advertisedNodes;
    }

    public int getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(int originalSource) {
        this.originalSource = originalSource;
    }

    @Override
    public String toString() {
        return "OLSR_Message_TC{" +
                "originalSource=" + originalSource +
                ", msgId=" + msgId +
                ", advertisedNodes=" + advertisedNodes +
                ", source=" + source +
                ", destination=" + destination +
                ", text='" + text + '\'' +
                ", isSuccessful=" + isSuccessful +
                ", numberFramesShown=" + numberFramesShown +
                ", messageType=" + messageType +
                ", isMulticast=" + isMulticast +
                "} " + super.toString();
    }

    @Override
    public String getInfo() {
        return super.getInfo() +
                "Protocol: OLSR (TC)\n" +
                "Original Source: " + originalSource + "\n" +
                "Message ID: " + msgId + "\n" +
                "Advertised Nodes: " + advertisedNodes + "\n";
    }

}
