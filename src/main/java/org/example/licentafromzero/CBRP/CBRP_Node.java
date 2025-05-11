package org.example.licentafromzero.CBRP;

import javafx.util.Pair;
import org.example.licentafromzero.AODV.AODV_Message;
import org.example.licentafromzero.AODV.AODV_RoutingTableEntry;
import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;
import org.example.licentafromzero.Domain.Node;

import java.util.*;

/*
TODO:
 Did not implement CAT(Cluster Adjency Table - it is kind of stored in routing table)
 Did not implement one way connection and communication - not relevant for me

 */

public class CBRP_Node extends Node {
    //TODO: finnish treating all messages
    //TODO: add neighbour discovery
    //TODO: Add cluster formation logic
    //TODO: add Cluster head election
    //TODO: add unidirectional link

    private int status = 0; // 0 = Undecided | 1 =Cluster Head | 2 = Cluster Member
    private int clusterHeadId = -1;
    private int clusterId = -1;
    private long lastStateChange = System.currentTimeMillis();
    private boolean updatingTable = false;
    private int broadcastID = 0;


    private long lastHelloSent = 0;
    //TODO: Do not remove, will transform into constants!
//    private int helloInterval;      // Time between HELLO broadcasts
//    private int clusterHeadTimeout; // Time before considering a cluster head lost
//    private int neighborTimeout;    // Time before considering a neighbor lost


    private ArrayList<Message> waitingMessages = new ArrayList<>();
    private ArrayList<CBRP_NeighbourTableEntry> neighbourTable = new ArrayList<>();
    private ArrayList<Integer> clusterMembers = new ArrayList<>();
    private Map<Integer, CBRP_RoutingTableEntry> routingTable = new HashMap<>();
    private Set<Pair<Integer, Integer>> knownMessageIDs = new HashSet<>(); // format: <sourceID,broadcastID>

    public CBRP_Node(int id, int x, int y) {
        super(id, x, y);
    }

    public CBRP_Node(int id, int x, int y, int commRadius) {
        super(x,y,id, commRadius);

    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            //TODO: overwrite!
            discoverNeighbours();

            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                while (destination == id)
                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                sendMessage(new Message(id, destination, "Hello from " + id));
                lastMessageSent = totalRunTime;
            }

//            if(totalRunTime > Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION + lastNeighbourDiscovery && !updatedPaths){
//                updateRoutes();
//                updatedPaths = true;
//            }

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

    //TODO: STOP BEING RETARDED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
    //TODO: Functions are for handleMessage, not for receiveMessage....fix it!
    @Override
    public void sendMessage(Message message) {
        message.setSource(id);
        switch(message.getMessageType()){
            case TEXT:
                if(status == 0){ //node is undecided
                    log(2, "cannot send message to " + message.getDestination() + " because the node is undecided!");
                }
                else if(status == 1){ //node is not CH
                    CBRP_Message cbrpMessage = new CBRP_Message(id, clusterHeadId, MessageType.CBRP_TEXT, message.getDestination(), -1, message.getText());
                    messageRouter.sendMessage(cbrpMessage);
                }
                else if(status == 2){ //node is CH
                    //CH initiated messaege. Check if destinaition is in same cluster, then check if route is known, and forward it.
                    // If no route is known, discover it

                    if(clusterMembers.contains(message.getDestination())){
                        CBRP_Message cbrpMessage = new CBRP_Message(id, message.getDestination(), MessageType.CBRP_TEXT,
                                message.getDestination(), clusterId, message.getText());
                        messageRouter.sendMessage(cbrpMessage);

                    }else if(routingTable.containsKey(message.getDestination())){
                        CBRP_Message cbrpMessage = new CBRP_Message(id, routingTable.get(message.getDestination()).nextHop,
                                MessageType.CBRP_TEXT, message.getDestination(),
                                routingTable.get(message.getDestination()).destinationClusterId ,message.getText());
                        messageRouter.sendMessage(cbrpMessage);
                    }
                    else{
                        waitingMessages.add(message);
                        beginRouteDiscovery(message.getDestination());
                    }
                }
                break;
            case CBRP_TEXT:
                if(message instanceof CBRP_Message cbrpMessage) {
                    if (status == 1) {
                        //message is not CH but received CBRP_TEXT -> is probably gateway node, so forward it to the CH.
                        cbrpMessage.setDestination(routingTable.get(cbrpMessage.getDestinationClusterId()).nextHop);
                        messageRouter.sendMessage(cbrpMessage);

                    } else if (status == 2) {
                        //message is CH and receivedd CBRP_TEXT -> check if finalDest is in it's cluster, else send to apropriate CH
                        if(clusterId == cbrpMessage.getDestinationClusterId()){
                            cbrpMessage.setDestination(cbrpMessage.getFinalDestination());
                            messageRouter.sendMessage(cbrpMessage);
                        }else if(routingTable.containsKey(cbrpMessage.getFinalDestination())){
                            cbrpMessage.setDestination(routingTable.get(cbrpMessage.getFinalDestination()).nextHop);
                            messageRouter.sendMessage(cbrpMessage);
                        }else{
                            waitingMessages.add(message);
                            beginRouteDiscovery(message.getDestination());
                        }
                    }
                }
                break;
            case CBRP_NEIGHBOUR_HELLO:
                if(message instanceof CBRP_Message cbrpMessage){
                    for(var entry: cbrpMessage.getNeighbourTable()){
                        if(entry.id!=id){
                            CBRP_RoutingTableEntry routingTableEntry = new CBRP_RoutingTableEntry(entry.id,entry.clusterId, cbrpMessage.getSource());
                            this.routingTable.put(entry.id, routingTableEntry);
                        }
                    }
                }
                break;
            case CBRP_RREQ:
                if(message instanceof CBRP_Message cbrpMessage){
                    //TODO: add id to known id's
                    //check if node is destination clusterHead, and then check if message is for one of the members, or for itself
                    if(cbrpMessage.getDestinationClusterId() == id){
                        if(cbrpMessage.getFinalDestination() == id){
                            //TODO
                        }else if(clusterMembers.contains(cbrpMessage.getFinalDestination())){
                            //TODO
                        }
                    }else{
                        //TODO: check if route is known, then send RREP, or ignore
                    }
                }
                break;
            case CBRP_RREP:
                if(message instanceof CBRP_Message cbrpMessage){
                    //TODO: add Route to routing table, and try to send all messages
                }
                break;
            case CBRP_RERR:
                if(message instanceof CBRP_Message cbrpMessage) {
                    //TODO: update routing table and send to all other CH
                }
                break;
            default:
                super.sendMessage(message);
                break;
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

//            case AODV_RREQ:
//                if(message instanceof AODV_Message aodvMessage){
//                    //drops it is it knows it
//                    //incerase it's hopCount
//                    //check if the node is the destination!
//                    //check if node knows about finalDestination, if yes, send RREP.
//                    //else, forward it to all neighbours
//                    //Add info about sourceSeqNum to routing table - increase it
//                    Pair<Integer, Integer> msgId = new Pair<>(aodvMessage.getOriginalSource(), aodvMessage.getBroadcastId());
//                    if(!knownMessageIDs.contains(msgId)){
//                        knownMessageIDs.add(msgId);
//                        aodvMessage.increaseHopCount();
//                        //add route to source of RREQ
//                        if(!routingTable.containsKey(aodvMessage.getOriginalSource()) ||
//                                routingTable.get(aodvMessage.getOriginalSource()).getDestSeqNum() < aodvMessage.getSourceSeqNum() ||
//                                routingTable.get(aodvMessage.getOriginalSource()).getHopCount() > aodvMessage.getHopCount()){
//
//                            AODV_RoutingTableEntry tableEntry = new AODV_RoutingTableEntry(aodvMessage.getOriginalSource(),
//                                    aodvMessage.getSource(), aodvMessage.getSourceSeqNum(), aodvMessage.getHopCount(), totalRunTime);
//                            routingTable.put(aodvMessage.getOriginalSource(), tableEntry);
//
//                            sendWaitingMessages();
//                        }
//
//
//                        //check if the node is finalDestination
//                        if(aodvMessage.getFinalDestination() == id){
//                            log(2, "is the finalDestination for: " + stringifyId(aodvMessage) + " -> " + aodvMessage.getFinalDestination() + " sending RREP");
//                            AODV_Message rrep = new AODV_Message(id, aodvMessage.getSource(), MessageType.AODV_RREP,
//                                    id, aodvMessage.getOriginalSource(), sequenceNumber);
//                            sequenceNumber++;
//                            sendMessage(rrep);
//                        }else if(routingTable.containsKey(aodvMessage.getFinalDestination())){
//                            //if node is not the finalDestination, but it knows a fresher route
//                            AODV_RoutingTableEntry routingTableEntry = routingTable.get(aodvMessage.getFinalDestination());
//                            if(routingTableEntry.destSeqNum > aodvMessage.getDestSeqNum()){
//                                log(2, "knows route, and has fresher info, sending RREP");
//                                AODV_Message rrep = new AODV_Message(id, aodvMessage.getSource(), MessageType.AODV_RREP,
//                                        routingTableEntry.destAddr, aodvMessage.getOriginalSource(), routingTableEntry.destSeqNum);
//                                sendMessage(rrep);
//                            }
//                        }
//                        else{
//                            sendMessage(aodvMessage);
//                        }
//                    }else{
//                        log(1, "RREQ( " +stringifyId(aodvMessage) + " -> " + aodvMessage.getFinalDestination() +" )is known, discarding");
//                    }
//                }
//                break;
//            case AODV_RREP:
//                if(message instanceof AODV_Message aodvMessage){
//                    //check if  node is the final destinaition
//                    //add it to the routing table
//                    //if it is final destiation stop, else, dorward it
//                    //add route to source of RREQ
//
//                    aodvMessage.increaseHopCount();
//                    if(aodvMessage.getActualSource() != aodvMessage.getOriginalSource()){
//                        log(2, "RREP is not from the target node, but a cached one!");
//                    }
//
//                    if(!routingTable.containsKey(aodvMessage.getOriginalSource()) ||
//                            routingTable.get(aodvMessage.getOriginalSource()).getDestSeqNum() < aodvMessage.getSourceSeqNum() ||
//                            routingTable.get(aodvMessage.getOriginalSource()).getHopCount() > aodvMessage.getHopCount()){
//
//                        AODV_RoutingTableEntry tableEntry = new AODV_RoutingTableEntry(aodvMessage.getOriginalSource(),
//                                aodvMessage.getSource(), aodvMessage.getSourceSeqNum(), aodvMessage.getHopCount(), totalRunTime);
//                        routingTable.put(aodvMessage.getOriginalSource(), tableEntry);
//
//                        sendWaitingMessages();
//                    }
//                    if(aodvMessage.getFinalDestination() != id){
//                        if(routingTable.containsKey(aodvMessage.getFinalDestination())) {
//                            aodvMessage.setDestination(routingTable.get(aodvMessage.getFinalDestination()).getNextHop());
//                            sendMessage(aodvMessage);
//                        }else{
//                            log(2, "cannot forward message to " + aodvMessage.getFinalDestination() + " because route is broken, adding to waiting messages");
//                            waitingMessages.add(aodvMessage);
//                        }
//                    }else{
//                        log(2, "received requested route to " + aodvMessage.getOriginalSource());
//                        //loop over all messages in waitingMessages and send them if  there is a destination:
//
////                        ArrayList<Message> copyWaitingMessages = new ArrayList<>(waitingMessages);
////                        for(Message waitingMessage : copyWaitingMessages) {
////                            if (routingTable.containsKey(waitingMessage.getDestination())) {
////                                int nextHop = routingTable.get(waitingMessage.getDestination()).getNextHop();
////                                if(waitingMessage instanceof AODV_Message aodv_message){
////                                    aodv_message.setDestination(nextHop);
////                                    sendMessage(aodvMessage);
////                                }else {
////                                    AODV_Message message1 = new AODV_Message(id, nextHop, MessageType.AODV_TEXT,
////                                            waitingMessage.getDestination(), waitingMessage.getText());
////                                    sendMessage(message1);
////                                }
////                                waitingMessages.remove(waitingMessage);
////
////                            }
////                        }
//
//                    }
//                }
//                break;
//            case AODV_TEXT:
//                if(message instanceof AODV_Message aodvMessage){
//                    if(aodvMessage.getFinalDestination() == id){
//                        log(3, "received text: " + message.getText());
//                    }else{
//                        //if it reatches this the route should be already known, no need to check it
//                        //however better safe then sorry
//                        if(routingTable.containsKey(aodvMessage.getFinalDestination())) {
//                            AODV_RoutingTableEntry routingTableEntry = routingTable.get(aodvMessage.getFinalDestination());
//                            aodvMessage.setDestination(routingTableEntry.nextHop);
//                            sendMessage(aodvMessage);
//                        }else{
//                            log(2, "cannot forward message to " + aodvMessage.getFinalDestination() + " because route is broken, restarting route discovery");
//                            sendMessage(new Message(aodvMessage.getOriginalSource(), aodvMessage.getFinalDestination(), aodvMessage.getText()));
//                        }
//                    }
//                }
//                break;
//            case AODV_RERR:
//                if(message instanceof AODV_Message aodvMessage){
//                    Pair<Integer, Integer> id = new Pair<>(aodvMessage.getOriginalSource(), aodvMessage.getBroadcastId());
//                    if(!knownMessageIDs.contains(id)){
//                        knownMessageIDs.add(id);
//                        sendMessage(aodvMessage);
//                        updateRoutes(aodvMessage);
//                    }
//                }
//                break;
        }
    }

    private void sendWaitingMessages(){
//        ArrayList<Message> copyWaitingMessages = new ArrayList<>(waitingMessages);
//        for(Message waitingMessage : copyWaitingMessages) {
//            if (routingTable.containsKey(waitingMessage.getDestination())) {
//                int nextHop = routingTable.get(waitingMessage.getDestination()).getNextHop();
//                if(waitingMessage instanceof AODV_Message aodv_message){
//                    aodv_message.setDestination(nextHop);
//                    sendMessage(aodv_message);
////                }else {
////                    AODV_Message message1 = new AODV_Message(id, nextHop, MessageType.AODV_TEXT,
////                            waitingMessage.getDestination(), waitingMessage.getText());
////                    sendMessage(message1);
//                }
//                waitingMessages.remove(waitingMessage);
//            }
//        }
    }

    private void beginRouteDiscovery(int destination){
        //TODO
    }

    private void updateRoutes(){

    }


}
