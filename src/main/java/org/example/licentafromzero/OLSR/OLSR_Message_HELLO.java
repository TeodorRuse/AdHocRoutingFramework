package org.example.licentafromzero.OLSR;

import org.example.licentafromzero.AODV.AODV_Message;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.util.ArrayList;
import java.util.HashMap;

public class OLSR_Message_HELLO extends Message {
    private ArrayList<OLSR_NeighbourTable_1hop> neighbours;
    private ArrayList<Integer> mrpsSender;

    public OLSR_Message_HELLO(OLSR_Message_HELLO message) {
        super(message);
        this.neighbours = message.getNeighbours();
        this.mrpsSender = message.getMrpsSender();
    }

    @Override
    public Message copy() {
        return new OLSR_Message_HELLO(this);
    }

    public OLSR_Message_HELLO(int source, int destination, ArrayList<OLSR_NeighbourTable_1hop> neighbours, ArrayList<Integer> mrpsSender) {
        super(source, destination, MessageType.OLSR_HELLO, false);
        this.neighbours = neighbours;
        this.mrpsSender = mrpsSender;
    }

    public ArrayList<OLSR_NeighbourTable_1hop> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(ArrayList<OLSR_NeighbourTable_1hop> neighbours) {
        this.neighbours = neighbours;
    }

    public ArrayList<Integer> getMrpsSender() {
        return mrpsSender;
    }

    public void setMrpsSender(ArrayList<Integer> mrpsSender) {
        this.mrpsSender = mrpsSender;
    }

    @Override
    public String toString() {
        return "OLSR_Message_HELLO{" +
                "neighbours=" + neighbours +
                ", mrpsSender=" + mrpsSender +
                ", source=" + source +
                ", destination=" + destination +
                ", text='" + text + '\'' +
                ", isSuccessful=" + isSuccessful +
                ", numberFramesShown=" + numberFramesShown +
                ", messageType=" + messageType +
                ", isMulticast=" + isMulticast +
                "} " + super.toString();
    }
}
