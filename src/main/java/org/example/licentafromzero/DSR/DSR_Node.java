package org.example.licentafromzero.DSR;

import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;
import org.example.licentafromzero.Domain.Node;

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

            discoverNeighbours();

//            if(totalRunTime >= Constants.NODE_STARTUP_TIME)
//                Constants.SIMULATION_DELAY_BETWEEN_FRAMES = 100;

            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                while (destination == id)
                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                sendMessage(new Message(id, destination, "Hello from " + id));
                lastMessageSent = totalRunTime;
            }

            if(totalRunTime > Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION + lastNeighbourDiscovery && !updatedPaths){
                updateRoutes();
                updatedPaths = true;
            }

//            if(id == 0) {
//                move();
//            }

            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }

    @Override
    public void sendMessage(Message message){
        message.setSource(id);
        if(message instanceof DSR_Message dsrMessage){
            if(dsrMessage.getMessageType() == MessageType.DSR_RREQ ) {
                if(Constants.LOG_LEVEL < 3)
                    System.out.println("Node " + id + " sending RREQ(" + dsrMessage.getRequestId() + ") all neighbours for " + dsrMessage.getFinalDestination());
                this.messageRouter.sendMessage(dsrMessage, neighbours);
            } else if(dsrMessage.getMessageType() == MessageType.DSR_RERR){
                if(Constants.LOG_LEVEL < 3)
                    System.out.println("Node " + id + " sending RERR(" + dsrMessage.getRequestId() + ") all neighbours for " + dsrMessage.getFinalRoute());
                this.messageRouter.sendMessage(dsrMessage, neighbours);
            } else {
                this.messageRouter.sendMessage(dsrMessage);
            }
        }else {
            if(message.getMessageType() == MessageType.TEXT) {
                if (knownRoutes.containsKey(message.getDestination())) {
                    if(Constants.LOG_LEVEL < 3)
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
        if(Constants.LOG_LEVEL < 2)
            System.out.println("Node " + id + " received " + message.getMessageType() + " from: " + message.getSource());
        switch (message.getMessageType()) {
            case TEXT:
                break;
            case NEIGHBOUR_SYN:
                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
                break;
            case NEIGHBOUR_ACK:
                newNeighbours.add(message.getSource());
                break;

            case DSR_RREQ:
                if(message instanceof DSR_Message dsrMessage) {
                    if(dsrMessage.getTtl() > 0 && !knownMessageIds.contains(dsrMessage.getRequestId())) {
                        if (dsrMessage.getFinalDestination() == id) {
                            dsrMessage.addToRouteRecord(id);
                            if(Constants.LOG_LEVEL < 3)
                                System.out.println("RREQ " + dsrMessage.getRouteRecord() + " reached destination, sending routeReply");
                            ArrayList<Integer> route = dsrMessage.getRouteRecord();
                            ArrayList<Integer> finalRoute = new ArrayList<>(route);
                            Collections.reverse(route);
                            int nextHop = route.remove(0);
                            DSR_Message rrep = new DSR_Message(id, nextHop, MessageType.DSR_RREP, route, finalRoute);
                            sendMessage(rrep);
                        } else {
                            dsrMessage.addToRouteRecord(id);
                            knownMessageIds.add(dsrMessage.getRequestId());
                            sendMessage(dsrMessage);
                        }
                    }
                }
                break;
            case DSR_TEXT:
                if(message instanceof DSR_Message dsrMessage) {
                    if(dsrMessage.getRouteRecord().isEmpty()){
                        if(Constants.LOG_LEVEL < 4)
                            System.out.println("Node " + id + " is destination for " + dsrMessage.getText() + " from: " + dsrMessage.getFinalDestination());
                    }else {
                        int nextHop = dsrMessage.getRouteRecord().remove(0);
                        dsrMessage.setDestination(nextHop);
                        sendMessage(dsrMessage);
                    }
                }
                break;
            case DSR_RREP:
                if(message instanceof DSR_Message dsrMessage) {
                    if(dsrMessage.getRouteRecord().isEmpty()){
                        addRoute(dsrMessage.getFinalRoute());

                        sendMessage(waitingMessage);
                    }else{
                        int nextHop = dsrMessage.getRouteRecord().remove(0);
                        dsrMessage.setDestination(nextHop);
                        sendMessage(dsrMessage);
                    }
                }break;
            case DSR_RERR:
                if(message instanceof DSR_Message dsrMessage &&
                    !knownMessageIds.contains(dsrMessage.getRequestId())) {

                    updateRoute(dsrMessage.getFinalRoute());
                    knownMessageIds.add(dsrMessage.getRequestId());
                    sendMessage(dsrMessage);
                }break;
        }
    }

    private void addRoute(ArrayList<Integer> route) {
        for (int i = 0; i < route.size(); i++) {
            Integer targetNode = route.get(i);

            ArrayList<Integer> subRoute = new ArrayList<>(route.subList(0, i + 1));

            if (!knownRoutes.containsKey(targetNode) || subRoute.size() < knownRoutes.get(targetNode).size()) {
                knownRoutes.put(targetNode, subRoute);
            }
        }
    }
    //for updating broken routes received from RERR
    private void updateRoute(ArrayList<Integer> brokenRoute) {
        Integer nodeA = brokenRoute.get(0);
        Integer nodeB = brokenRoute.get(1);

        // Create a list of keys to remove (can't modify map while iterating)
        ArrayList<Integer> routesToRemove = new ArrayList<>();

        for (Map.Entry<Integer, ArrayList<Integer>> entry : knownRoutes.entrySet()) {
            ArrayList<Integer> route = entry.getValue();
            boolean isBroken = false;

            for (int i = 0; i < route.size() - 1; i++) {
                if (route.get(i).equals(nodeA) && route.get(i+1).equals(nodeB)) {
                    isBroken = true;
                    break;
                }
            }

            if (isBroken) {
                if(Constants.LOG_LEVEL < 3)
                    System.out.println("Node " + id + " removing broken route " + entry.getKey() + ": " + route);
                routesToRemove.add(entry.getKey());
            }
        }

        for (Integer key : routesToRemove) {
            knownRoutes.remove(key);
        }
    }

    private void updateRoutes(){
        ArrayList<Integer> routesToRemove = new ArrayList<>();
        for (Map.Entry<Integer, ArrayList<Integer>> entry : knownRoutes.entrySet()) {
            ArrayList<Integer> route = entry.getValue();

            if (route.size() >= 2) {
                Integer secondElement = route.get(1);

                if (!neighbours.contains(secondElement)) {
                    if(Constants.LOG_LEVEL < 3)
                        System.out.println("Node " + id + " route broken: " + route);
                    DSR_Message dsrMessage = new DSR_Message(id, -1, MessageType.DSR_RERR, id, secondElement, System.currentTimeMillis());

                    sendMessage(dsrMessage);

                    routesToRemove.add(entry.getKey());
                }
            }
        }

        for (Integer key : routesToRemove) {
            knownRoutes.remove(key);
        }
    }

    public Map<Integer, ArrayList<Integer>> getKnownRoutes() {
        return knownRoutes;
    }
}
