package org.example.licentafromzero.Domain;

import java.util.ArrayList;
import java.util.HashSet;

public class MessageRouter_Wormhole extends MessageRouter{
    public MessageRouter_Wormhole() {
        this.nodes = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    @Override
    public boolean canSend(Node source, Node destination){
        //Wired link between nodes
        if((source.getId() == 6 || source.getId() == 10) &&
                (destination.getId() == 6 || destination.getId() == 10)) {
//            System.err.println("USING WIRED LINK");
            return true;
        }


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
}
