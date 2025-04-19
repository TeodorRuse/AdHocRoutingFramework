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

    public DSR_Node(int x, int y, int id) {
        super(x, y, id);
        this.knownMessageIds = new ArrayList<>();
        this.knownRoutes = new HashMap<>();
        ArrayList<Integer> routeToSelf = new ArrayList<>();
        routeToSelf.add(id);
        knownRoutes.put(id, routeToSelf);
    }

    public DSR_Node(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
        this.knownMessageIds = new ArrayList<>();
        this.knownRoutes = new HashMap<>();
        ArrayList<Integer> routeToSelf = new ArrayList<>();
        routeToSelf.add(id);
        knownRoutes.put(id, routeToSelf);
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime){
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            //TODO: There is periodic version in basic node
            if(totalRunTime == -1){
                discoverNeighbours();
                if(Constants.LOG_DETAILS < 3)
                    System.out.println("Node " + id + " discovering neighbours");
            }

            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                while (destination == id)
                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                sendMessage(new Message(id, destination, "Hello from " + id));
                lastMessageSent = totalRunTime;
            }

//            move();

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
            if(dsrMessage.getMessageType() == MessageType.DSR_RREQ ) {
                if(Constants.LOG_DETAILS < 3)
                    System.out.println("Node " + id + " sending RREQ(" + dsrMessage.getRequestId() + ") all neighbours for " + dsrMessage.getFinalDestination());
                this.messageRouter.sendMessage(dsrMessage, neighbours);
            }
            else {
                this.messageRouter.sendMessage(dsrMessage);
            }
        }else {
            if(message.getMessageType() == MessageType.TEXT) {
                if (knownRoutes.containsKey(message.getDestination())) {
                    if(Constants.LOG_DETAILS < 3)
                        System.out.println("Node: " + id + " using known route!");
                    ArrayList<Integer> route = new ArrayList<>(knownRoutes.get(message.getDestination()));
                    route.remove(0);
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
        if(Constants.LOG_DETAILS < 2)
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
                    if(dsrMessage.getTtl() > 0 && !knownMessageIds.contains(dsrMessage.getRequestId())) {
                        if (dsrMessage.getFinalDestination() == id) {
                            dsrMessage.addToRouteRecord(id);
                            if(Constants.LOG_DETAILS < 3)
                                System.out.println("RREQ " + dsrMessage.getRouteRecord() + " reached destination, sending routeReply");
                            ArrayList<Integer> route = dsrMessage.getRouteRecord();
                            ArrayList<Integer> finalRoute = new ArrayList<>(route);
                            Collections.reverse(route);
                            int nextHop = route.remove(0);
                            DSR_Message rrep = new DSR_Message(id, nextHop, MessageType.DSR_RREP, route, finalRoute);
                            sendMessage(rrep);
                        } else {
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
                        if(Constants.LOG_DETAILS < 4)
                            System.out.println("Node " + id + " is destination for " + dsrMessage.getText() + " from: " + dsrMessage.getFinalDestination());
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
                    if(dsrMessage.getRouteRecord().isEmpty()){
//                        Integer finalHop = dsrMessage.getFinalRoute().get(dsrMessage.getFinalRoute().size()-1);
//                        knownRoutes.put(finalHop , dsrMessage.getFinalRoute());
                        addRoute(dsrMessage.getFinalRoute());

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

    void addRoute(ArrayList<Integer> route) {
        for (int i = 0; i < route.size(); i++) {
            Integer targetNode = route.get(i);

            ArrayList<Integer> subRoute = new ArrayList<>(route.subList(0, i + 1));

            if (!knownRoutes.containsKey(targetNode) || subRoute.size() < knownRoutes.get(targetNode).size()) {
                knownRoutes.put(targetNode, subRoute);
            }
        }
    }

    public Map<Integer, ArrayList<Integer>> getKnownRoutes() {
        return knownRoutes;
    }
}
