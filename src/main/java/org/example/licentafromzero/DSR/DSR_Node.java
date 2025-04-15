package org.example.licentafromzero.DSR;

import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;
import org.example.licentafromzero.Domain.Node;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DSR_Node extends Node {

    private ArrayList<Long> knownMessageIds;
    private Map<Integer, ArrayList<Integer>> knownRoutes;
    private Message waitingMessage;
    private boolean tempHasSend = false;
    private boolean tempsend2 = false;

    public DSR_Node(int x, int y, int id) {
        super(x, y, id);
        this.knownMessageIds = new ArrayList<>();
        this.knownRoutes = new HashMap<>();
    }

    public DSR_Node(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
        this.knownMessageIds = new ArrayList<>();
        this.knownRoutes = new HashMap<>();
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime){
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            if(totalRunTime == -1){
                discoverNeighbours();
            }

            if(id == 0 && totalRunTime > 2000 && !tempHasSend){
                System.out.println("Sending Hello message------------------------------------------------------------------");
                Constants.SIMULATION_DELAY_BETWEEN_FRAMES = 500;
                sendMessage(new Message(id, 9, "HELLO!"));
                tempHasSend = true;
            }

//            if(id == 6 && totalRunTime > 2000 && !tempsend2){
//                System.out.println("Sending Hello2 message------------------------------------------------------------------");
//                Constants.SIMULATION_DELAY_BETWEEN_FRAMES = 500;
//                sendMessage(new Message(id, 4, "HELLO2!"));
//                tempsend2 = true;
//            }

            move();

            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }

    @Override
    public void discoverNeighbours() {
        super.discoverNeighbours();
    }

    @Override
    public void sendMessage(Message message){
        if(message instanceof DSR_Message dsrMessage){
            if(dsrMessage.getMessageType() == MessageType.DSR_RREQ) {
                System.out.println("Node " + id + " sending RREQ all neighbours");
                this.messageRouter.sendMessage(dsrMessage, neighbours);
            }
            else
                this.messageRouter.sendMessage(dsrMessage);
        }else {
            if(message.getMessageType() == MessageType.TEXT) {
                if (knownRoutes.containsKey(message.getDestination())) {
                    ArrayList<Integer> route = new ArrayList<>(knownRoutes.get(message.getDestination()));
                    Integer first = route.remove(0);

                    Message textMessage = new DSR_Message(id, first, MessageType.DSR_TEXT, route, message.getText());
                    this.messageRouter.sendMessage(textMessage);
                } else {
                    waitingMessage = message;

                    DSR_Message dsrMessage = new DSR_Message(id, -1, message.getDestination(), MessageType.DSR_RREQ, true, System.currentTimeMillis());
                    this.knownMessageIds.add(dsrMessage.getRequestId());
                    dsrMessage.addToRouteRecord(id);
                    this.messageRouter.sendMessage(dsrMessage, neighbours);
                }
            }else{
                super.sendMessage(message);
            }
        }

    }

    @Override
    public void handleMessage(Message message) {
        System.out.println("Node " + id + " received " + message.getMessageType() + " from: " + message.getSource());
        switch (message.getMessageType()) {
            case TEXT:
                break;
            case NEIGHBOUR_SYN:
                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
                break;
            case NEIGHBOUR_ACK:
                neighbours.add(message.getSource());
                break;

            case DSR_RREQ:
                if(message instanceof DSR_Message dsrMessage) {
                    System.out.println("Node " + id + " is RREQ");
                    if(dsrMessage.getTtl() > 0 && !knownMessageIds.contains(dsrMessage.getRequestId())) {
                        System.out.println("Node " + id + " entered 1");
                        if (dsrMessage.getFinalDestination() == id) {
                            System.out.println("Node " + id + " entered 2");
                            System.out.println("Reached destination, sending routeReply");
                            dsrMessage.addToRouteRecord(id);
                            ArrayList<Integer> route = dsrMessage.getRouteRecord();
                            ArrayList<Integer> finalRoute = new ArrayList<>(route);
                            Collections.reverse(route);
                            int nextHop = route.remove(0);
                            DSR_Message rrep = new DSR_Message(id, nextHop, MessageType.DSR_RREP, route, finalRoute);
                            sendMessage(rrep);
                        } else {
                            System.out.println("Node " + id + " entered 3");
                            dsrMessage.addToRouteRecord(id);
                            dsrMessage.setSource(id);
                            knownMessageIds.add(dsrMessage.getRequestId());
                            sendMessage(dsrMessage);
                        }
                    }
                }
                break;
            case DSR_TEXT:
                if(message instanceof DSR_Message dsrMessage) {
                    if(dsrMessage.getRouteRecord().isEmpty()){
                        System.out.println("Node " + id + " received message " + dsrMessage.getText() + " from: " + dsrMessage.getFinalDestination());
                    }else {
                        int nextHop = dsrMessage.getRouteRecord().remove(0);
                        dsrMessage.setDestination(nextHop);
                        dsrMessage.setSource(id);
                        sendMessage(dsrMessage);
                    }
                }
                break;
            case DSR_RREP:
                if(message instanceof DSR_Message dsrMessage) {
                    //if it was mean for me, and if it was not
                    if(dsrMessage.getRouteRecord().isEmpty()){
                        System.out.println("NODE " + id + " Received route reply :" + dsrMessage.getFinalRoute());
                        Integer finalHop = dsrMessage.getFinalRoute().get(dsrMessage.getFinalRoute().size()-1);
                        knownRoutes.put(finalHop , dsrMessage.getFinalRoute());

                        sendMessage(waitingMessage);
                    }else{
                        int nextHop = dsrMessage.getRouteRecord().remove(0);
                        dsrMessage.setDestination(nextHop);
                        dsrMessage.setSource(id);
                        sendMessage(dsrMessage);
                    }
                }
                break;
        }
    }

}
