package org.example.licentafromzero.AODV;

import javafx.util.Pair;
import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;
import org.example.licentafromzero.Domain.Node;

import java.util.*;

public class AODV_Node extends Node {
    private int sequenceNumber = 0;
    private int broadcastId = 0;

    private ArrayList<Message> waitingMessages = new ArrayList<>();
    private Map<Integer, AODV_RoutingTableEntry> routingTable = new HashMap<>();
    private Set<Pair<Integer, Integer>> knownMessageIDs = new HashSet<>(); // format: <sourceID,broadcastID>

    public AODV_Node(int x, int y, int id) {
        super(x, y, id);
        AODV_RoutingTableEntry routeToSelf = new AODV_RoutingTableEntry(id,id,0,0, totalRunTime);
        routingTable.put(id, routeToSelf);
    }

    public AODV_Node(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
        AODV_RoutingTableEntry routeToSelf = new AODV_RoutingTableEntry(id,id,0,0, totalRunTime);
        routingTable.put(id, routeToSelf);
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            discoverNeighbours();

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

//            if(id == 0)
//                move();

            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }

    @Override
    public void sendMessage(Message message) {
        message.setSource(id);
        if(message instanceof AODV_Message aodvMessage){

            if(aodvMessage.getMessageType() == MessageType.AODV_RREQ){
                log(2, "sending RREQ" + stringifyId(aodvMessage) + " to all");
                broadcastId++;
                sequenceNumber++;
                messageRouter.sendMessage(aodvMessage, neighbours);
            }else if(aodvMessage.getMessageType() == MessageType.AODV_RERR){
                log(2, "sending RERR(" + stringifyId(aodvMessage) + ") all neighbours for " + aodvMessage.getUnreachableAddresses());
                broadcastId++;
                sequenceNumber++;
                messageRouter.sendMessage(aodvMessage, neighbours);
            }else{
                messageRouter.sendMessage(aodvMessage);
            }
        }else{
            if(message.getMessageType() == MessageType.TEXT){
                if(routingTable.containsKey(message.getDestination())){
                    log(2, "using known route to send to " + message.getDestination());
                    AODV_RoutingTableEntry tableEntry = routingTable.get(message.getDestination());

                    AODV_Message aodv_message = new AODV_Message(id, tableEntry.nextHop, MessageType.AODV_TEXT,
                            message.getDestination(), message.getText());
                    messageRouter.sendMessage(aodv_message);
                }else{
                    waitingMessages.add(message);

                    log(2, " beginning route discovery to " + message.getDestination());
                    AODV_Message rreq = new AODV_Message(id, MessageType.AODV_RREQ, message.getDestination(), sequenceNumber, -1, broadcastId);
                    this.knownMessageIDs.add(new Pair<>(id, broadcastId));
                    messageRouter.sendMessage(rreq, neighbours);
                }
            }else{
                super.sendMessage(message);
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        log(1, "received " + message.getMessageType() + " from " + message.getSource());

        switch (message.getMessageType()) {
            case TEXT:
                break;
            case NEIGHBOUR_SYN:
                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
                break;
            case NEIGHBOUR_ACK:
                newNeighbours.add(message.getSource());
                break;

            case AODV_RREQ:
                if(message instanceof AODV_Message aodvMessage){
                    //drops it is it knows it
                    //incerase it's hopCount
                    //check if the node is the destination!
                    //check if node knows about finalDestination, if yes, send RREP.
                    //else, forward it to all neighbours
                    //Add info about sourceSeqNum to routing table - increase it
                    Pair<Integer, Integer> msgId = new Pair<>(aodvMessage.getOriginalSource(), aodvMessage.getBroadcastId());
                    if(!knownMessageIDs.contains(msgId)){
                        knownMessageIDs.add(msgId);
                        aodvMessage.increaseHopCount();
                        //add route to source of RREQ
                        if(!routingTable.containsKey(aodvMessage.getOriginalSource()) ||
                                routingTable.get(aodvMessage.getOriginalSource()).getDestSeqNum() < aodvMessage.getSourceSeqNum() ||
                                routingTable.get(aodvMessage.getOriginalSource()).getHopCount() > aodvMessage.getHopCount()){

                            AODV_RoutingTableEntry tableEntry = new AODV_RoutingTableEntry(aodvMessage.getOriginalSource(),
                                    aodvMessage.getSource(), aodvMessage.getSourceSeqNum(), aodvMessage.getHopCount(), totalRunTime);
                            routingTable.put(aodvMessage.getOriginalSource(), tableEntry);
                        }


                        //check if the node is finalDestination
                        if(aodvMessage.getFinalDestination() == id){
                            log(2, "is the finalDestination for: " + stringifyId(aodvMessage) + " -> " + aodvMessage.getFinalDestination() + " sending RREP");
                            AODV_Message rrep = new AODV_Message(id, aodvMessage.getSource(), MessageType.AODV_RREP,
                                    id, aodvMessage.getOriginalSource(), sequenceNumber);
                            sequenceNumber++;
                            sendMessage(rrep);
                        }else if(routingTable.containsKey(aodvMessage.getFinalDestination())){
                            //if node is not the finalDestination, but it knows a fresher route
                            AODV_RoutingTableEntry routingTableEntry = routingTable.get(aodvMessage.getFinalDestination());
                            if(routingTableEntry.destSeqNum > aodvMessage.getDestSeqNum()){
                                log(2, "knows route, and has fresher info, sending RREP");
                                AODV_Message rrep = new AODV_Message(id, aodvMessage.getSource(), MessageType.AODV_RREP,
                                        routingTableEntry.destAddr, aodvMessage.getOriginalSource(), routingTableEntry.destSeqNum);
                                sendMessage(rrep);
                            }
                        }
                        else{
                            sendMessage(aodvMessage);
                        }
                    }else{
                        log(1, "RREQ( " +stringifyId(aodvMessage) + " -> " + aodvMessage.getFinalDestination() +" )is known, discarding");
                    }
                }
                break;
            case AODV_RREP:
                if(message instanceof AODV_Message aodvMessage){
                    //check if  node is the final destinaition
                    //add it to the routing table
                    //if it is final destiation stop, else, dorward it
                    //add route to source of RREQ

                    aodvMessage.increaseHopCount();
                    if(aodvMessage.getActualSource() != aodvMessage.getOriginalSource()){
                        log(2, "RREP is not from the target node, but a cached one!");
                    }

                    if(!routingTable.containsKey(aodvMessage.getOriginalSource()) ||
                            routingTable.get(aodvMessage.getOriginalSource()).getDestSeqNum() < aodvMessage.getSourceSeqNum() ||
                            routingTable.get(aodvMessage.getOriginalSource()).getHopCount() > aodvMessage.getHopCount()){

                        AODV_RoutingTableEntry tableEntry = new AODV_RoutingTableEntry(aodvMessage.getOriginalSource(),
                                aodvMessage.getSource(), aodvMessage.getSourceSeqNum(), aodvMessage.getHopCount(), totalRunTime);
                        routingTable.put(aodvMessage.getOriginalSource(), tableEntry);
                    }
                    if(aodvMessage.getFinalDestination() != id){
                        if(routingTable.containsKey(aodvMessage.getFinalDestination())) {
                            aodvMessage.setDestination(routingTable.get(aodvMessage.getFinalDestination()).getNextHop());
                            sendMessage(aodvMessage);
                        }else{
                            log(2, "cannot forward message to " + aodvMessage.getFinalDestination() + " because route is broken, adding to waiting messages");
                            waitingMessages.add(aodvMessage);
                        }
                    }else{
                        log(2, "received requested route to " + aodvMessage.getOriginalSource());
                        //loop over all messages in waitingMessages and send them if  there is a destination:

                        ArrayList<Message> copyWaitingMessages = new ArrayList<>(waitingMessages);
                        for(Message waitingMessage : copyWaitingMessages) {
                            if (routingTable.containsKey(waitingMessage.getDestination())) {
                                int nextHop = routingTable.get(waitingMessage.getDestination()).getNextHop();
                                if(waitingMessage instanceof AODV_Message aodv_message){
                                    aodv_message.setDestination(nextHop);
                                    sendMessage(aodvMessage);
                                }else {
                                    AODV_Message message1 = new AODV_Message(id, nextHop, MessageType.AODV_TEXT,
                                            waitingMessage.getDestination(), waitingMessage.getText());
                                    sendMessage(message1);
                                }
                                waitingMessages.remove(waitingMessage);

                            }
                        }

                    }
                }
                break;
            case AODV_TEXT:
                if(message instanceof AODV_Message aodvMessage){
                    if(aodvMessage.getFinalDestination() == id){
                        log(3, "received text: " + message.getText());
                    }else{
                        //if it reatches this the route should be already known, no need to check it
                        //however better safe then sorry
                        if(routingTable.containsKey(aodvMessage.getFinalDestination())) {
                            AODV_RoutingTableEntry routingTableEntry = routingTable.get(aodvMessage.getFinalDestination());
                            aodvMessage.setDestination(routingTableEntry.nextHop);
                            sendMessage(aodvMessage);
                        }else{
                            log(2, "cannot forward message to " + aodvMessage.getFinalDestination() + " because route is broken, restarting route discovery");
                            sendMessage(new Message(aodvMessage.getOriginalSource(), aodvMessage.getFinalDestination(), aodvMessage.getText()));
                        }
                    }
                }
                break;
            case AODV_RERR:
                if(message instanceof AODV_Message aodvMessage){
                    Pair<Integer, Integer> id = new Pair<>(aodvMessage.getOriginalSource(), aodvMessage.getBroadcastId());
                    if(!knownMessageIDs.contains(id)){
                        knownMessageIDs.add(id);
                        sendMessage(aodvMessage);
                        updateRoutes(aodvMessage);
                    }
                }
                break;
        }
    }

    public void beginRouteDiscovery(int finalDestination){
        log(2, " beginning route discovery to " + finalDestination);
        AODV_Message rreq = new AODV_Message(id, MessageType.AODV_RREQ, finalDestination, sequenceNumber, -1, broadcastId);
        this.knownMessageIDs.add(new Pair<>(id, broadcastId));
        messageRouter.sendMessage(rreq, neighbours);
    }

    public void sendWaitingMessages(){
        ArrayList<Message> copyWaitingMessages = new ArrayList<>(waitingMessages);
        for(Message waitingMessage : copyWaitingMessages) {
            if (routingTable.containsKey(waitingMessage.getDestination())) {
                int nextHop = routingTable.get(waitingMessage.getDestination()).getNextHop();
                if(waitingMessage instanceof AODV_Message aodv_message){
                    aodv_message.setDestination(nextHop);
                    sendMessage(aodv_message);
                }else {
                    AODV_Message message1 = new AODV_Message(id, nextHop, MessageType.AODV_TEXT,
                            waitingMessage.getDestination(), waitingMessage.getText());
                    sendMessage(message1);
                }
                waitingMessages.remove(waitingMessage);
            }
        }
    }

    public String stringifyId(AODV_Message aodvMessage){
        return "[" + aodvMessage.getOriginalSource() + "|" + aodvMessage.getSourceSeqNum() + "]";
    }

    public Map<Integer, AODV_RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }


    private void updateRoutes(){
        ArrayList<Integer> routesToRemove = new ArrayList<>();
        for (Map.Entry<Integer, AODV_RoutingTableEntry> entry : routingTable.entrySet()) {
            AODV_RoutingTableEntry rte = entry.getValue();

            if (!neighbours.contains(rte.nextHop)) {
                log(2, "route to " + rte.destAddr + " via " + rte.nextHop + " is broken.");
                routesToRemove.add(entry.getKey());
            }
            if(rte.getReceivedTime() + Constants.NODE_AODV_STALE_ROUTE_PERIOD < totalRunTime){
                log(2, "route to " + rte.destAddr + " via " + rte.nextHop + " is stale.");
                routesToRemove.add(entry.getKey());
            }
        }

        if(!routesToRemove.isEmpty()) {
            AODV_Message rerr = new AODV_Message(id, MessageType.AODV_RERR, sequenceNumber, broadcastId, routesToRemove);
            sendMessage(rerr);
        }

        for (Integer key : routesToRemove) {
            routingTable.remove(key);
        }
    }

    //for updating broken routes received by RERR
    private void updateRoutes(AODV_Message aodvMessage) {
        ArrayList<Integer> unreachableAddresses = aodvMessage.getUnreachableAddresses();
        int rerrSenderId = aodvMessage.getOriginalSource();
        ArrayList<Integer> routesToRemove = new ArrayList<>();

        for (Integer unreachableDest : unreachableAddresses) {
            AODV_RoutingTableEntry rte = routingTable.get(unreachableDest);
            if (rte != null && rte.nextHop == rerrSenderId) {
                log(2, " removing route to " + unreachableDest + " via broken next hop " + rte.nextHop);
                routesToRemove.add(unreachableDest);
            }
        }

        for (Integer dest : routesToRemove) {
            routingTable.remove(dest);
        }
    }

    public ArrayList<Message> getWaitingMessages() {
        return waitingMessages;
    }

    public ArrayList<Message> getWaitingControlMessages() {
        ArrayList<Message> controlMessages = new ArrayList<>();
        for (Message message : waitingMessages) {
            if (message.getMessageType() != MessageType.TEXT &&
                    message.getMessageType() != MessageType.AODV_TEXT) {
                controlMessages.add(message);
            }
        }
        return controlMessages;
    }


}
