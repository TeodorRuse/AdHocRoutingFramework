package org.example.licentafromzero.Domain;

import java.util.ArrayList;
import java.util.HashSet;

public class MessageRouter {
    protected ArrayList<Node> nodes;
    protected ArrayList<Message> messages;

    public MessageRouter() {
        this.nodes = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public void addNode(Node node){
        nodes.add(node);
    }

    public void sendMessage(Message message) {
        Node source = null, destination = null;

        if(message.isMulticast()) {
            for(Node node: nodes){
                if(node.getId() != message.getSource()) {
//                sendMessage(new Message(message.getSource(), node.getId(), MessageType.TEXT, false));

                    Message message1 = message.copy();
                    message1.setDestination(node.getId());
                    message1.setMulticast(false);
                    sendMessage(message1);
                }
            }
        }
        else {
             for (Node node : nodes) {
                if (node.getId() == message.getDestination()) {
                    destination = node;
                }
                if (node.getId() == message.getSource()) {
                    source = node;
                }
            }
            if (source != null && destination != null){
                if (canSend(source, destination)) {
                    Util.log(0, source.getId(), " successfully sent "+ message.getMessageType() + " : " + source.getId() + " -> " + destination.getId());
                    destination.addMessage(message);
                    message.setSuccessful(true);
                    messages.add(message);
                } else {
                    Util.log(0, source.getId(), " failed to send "+ message.getMessageType() + " - out of range: " + source.getId() + " -> " + destination.getId());
                    message.setSuccessful(false);
                    messages.add(message);
                }
            }
        }
    }

    public void sendMessage(Message message, HashSet<Integer> destinations) {
        for(Node node: nodes){
            if(node.getId() != message.getSource() && destinations.contains(node.getId())) {
                Message message1 = message.copy();
                message1.setDestination(node.getId());
                message1.setMulticast(false);
                sendMessage(message1);
            }
        }
    }



    public boolean canSend(Node source, Node destination){
        double sourceX = source.getX();
        double sourceY = source.getY();
        double radius = source.getCommunicationRadius();

        double destX = destination.getX();
        double destY = destination.getY();

        double distance = Math.sqrt(Math.pow(destX - sourceX, 2) + Math.pow(destY - sourceY, 2));

        if (distance <= radius) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }
    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public int getNumberTextsSent(){
        int ret = 0;

        for(Message message : messages){
            if(message.getMessageType() == MessageType.TEXT ||
                    message.getMessageType() == MessageType.DSR_TEXT ||
                    message.getMessageType() == MessageType.AODV_TEXT ||
                    message.getMessageType() == MessageType.SAODV_TEXT||
                    message.getMessageType() == MessageType.CBRP_TEXT ||
                    message.getMessageType() == MessageType.OLSR_TEXT) {

                if (message.isSuccessful())
                    ret++;
            }
        }
        return ret;
    }

    public double getProcentSuccessfulTexts(){
        double suc = 0;
        double unsuc = 0;

        for(Message message : messages){
            if(message.getMessageType() == MessageType.TEXT ||
                message.getMessageType() == MessageType.DSR_TEXT ||
                message.getMessageType() == MessageType.AODV_TEXT ||
                message.getMessageType() == MessageType.SAODV_TEXT ||
                message.getMessageType() == MessageType.CBRP_TEXT ||
                message.getMessageType() == MessageType.OLSR_TEXT) {

                if (message.isSuccessful())
                    suc++;
                else
                    unsuc++;
            }
        }
        if(suc+unsuc == 0)
            return -1;
        return suc/(suc + unsuc)*100;
    }
}
