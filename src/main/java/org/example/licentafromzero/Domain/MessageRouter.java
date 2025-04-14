package org.example.licentafromzero.Domain;

import java.util.ArrayList;

public class MessageRouter {
    private ArrayList<Node> nodes;
    private ArrayList<Message> messages;

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
//                sendMessage(new Message(message.getSource(), node.getId(), MessageType.TEXT, false));
                Message message1 = new Message(message);
                message1.setDestination(node.getId());
                message1.setMulticast(false);
                sendMessage(message1);
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
                    destination.addMessage(message);
                    message.setSuccessful(true);
                    messages.add(message);
                } else {
                    System.out.println("Failed to send - out of range: " + source.getId() + " " + destination.getId());
                    message.setSuccessful(false);
                    messages.add(message);
                }
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
}
