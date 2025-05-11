package org.example.licentafromzero.CBRP;

import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.util.ArrayList;

public class CBRP_Message extends Message {
    private int actualSource;
    private int finalDestination;
    private int id;
    private int state;     // 0 = Undecided | 1 =Cluster Head | 2 = Cluster Member
    private ArrayList<CBRP_NeighbourTableEntry> neighbourTable = new ArrayList<>();
    private ArrayList<Integer> unreachableAddresses = new ArrayList<>();
    private int hopCount = 0;
    private int destinationClusterId;

    public CBRP_Message(CBRP_Message other){
        super(other);
        this.actualSource = other.actualSource;
        this.finalDestination = other.finalDestination;
        this.state = other.state;
        this.neighbourTable = other.neighbourTable;
        this.unreachableAddresses = other.unreachableAddresses;
        this.hopCount = other.hopCount;
        this.id = other.id;
        this.destinationClusterId = other.destinationClusterId;
    }

    @Override
    public Message copy() {
        return new CBRP_Message(this);
    }

    //for RREQ
    public CBRP_Message(int source, MessageType messageType ,int finalDestination, int state, int id){
        super(source, -1, messageType, true);
        this.actualSource = source;
        this.finalDestination = finalDestination;
        this.state = state;
        this.hopCount = 0;
        this.id = id;
    }

    //for RREP
    public CBRP_Message(int source, int destination, MessageType messageType, int actualSource ,int finalDestination, int state){
        super(source, destination, messageType, false);
        this.actualSource = actualSource;
        this.finalDestination = finalDestination;
        this.state = state;
        this.hopCount = 0;
    }

    //for RERR
    public CBRP_Message(int source, int destination, MessageType messageType, int actualSource ,int finalDestination, int state, ArrayList<Integer> unreachableAddresses){
        super(source, destination, messageType, false);
        this.actualSource = actualSource;
        this.finalDestination = finalDestination;
        this.state = state;
        this.hopCount = 0;
        this.unreachableAddresses = unreachableAddresses;
    }

    //for TEXT
    public CBRP_Message(int source, int destination, MessageType messageType, int finalDestination, int destinationClusterId, String text){
        super(source, destination, text);
        this.finalDestination = finalDestination;
        this.messageType = messageType;
        this.destinationClusterId = destinationClusterId;
        this.text = text;
    }

    //for NEIGHBOUR_HELLO
    public CBRP_Message(int source, int destination, MessageType messageType, int state, ArrayList<CBRP_NeighbourTableEntry> neighbourTable){
        super(source, destination, messageType, false);
        this.state = state;
        this.neighbourTable = neighbourTable;
        this.hopCount = 0;
    }

    public int getActualSource() {
        return actualSource;
    }

    public void setActualSource(int actualSource) {
        this.actualSource = actualSource;
    }

    public int getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(int finalDestination) {
        this.finalDestination = finalDestination;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public ArrayList<CBRP_NeighbourTableEntry> getNeighbourTable() {
        return neighbourTable;
    }

    public void setNeighbourTable(ArrayList<CBRP_NeighbourTableEntry> neighbourTable) {
        this.neighbourTable = neighbourTable;
    }

    public ArrayList<Integer> getUnreachableAddresses() {
        return unreachableAddresses;
    }

    public void setUnreachableAddresses(ArrayList<Integer> unreachableAddresses) {
        this.unreachableAddresses = unreachableAddresses;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDestinationClusterId() {
        return destinationClusterId;
    }

    public void setDestinationClusterId(int destinationClusterId) {
        this.destinationClusterId = destinationClusterId;
    }
}
