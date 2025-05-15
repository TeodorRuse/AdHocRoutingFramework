//package org.example.licentafromzero.CBRP;
//
//import javafx.util.Pair;
//import org.example.licentafromzero.AODV.AODV_Message;
//import org.example.licentafromzero.AODV.AODV_RoutingTableEntry;
//import org.example.licentafromzero.Domain.Constants;
//import org.example.licentafromzero.Domain.Message;
//import org.example.licentafromzero.Domain.MessageType;
//import org.example.licentafromzero.Domain.Node;
//
//import java.util.*;
//
///*
//TODO:
// Did not implement CAT(Cluster Adjency Table - it is kind of stored in routing table)
// Did not implement one way connection and communication - not relevant for me
//
// */
//
//public class CBRP_Node extends Node {
//    //TODO: finnish treating all messages
//    //TODO: add neighbour discovery
//    //TODO: Add cluster formation logic
//    //TODO: add Cluster head election
//    //TODO: add unidirectional link
//
//    private int status = 0; // 0 = Undecided | 1 =Cluster Head | 2 = Cluster Member
//    private int clusterHeadId = -1;
//    private int clusterId = -1;
//    private long lastStateChange = System.currentTimeMillis();
//    private boolean updatingTable = false;
//    private int broadcastID = 0;
//
//
//    private long lastHelloSent = 0;
//    //TODO: Do not remove, will transform into constants!
////    private int helloInterval;      // Time between HELLO broadcasts
////    private int clusterHeadTimeout; // Time before considering a cluster head lost
////    private int neighborTimeout;    // Time before considering a neighbor lost
//
//
//    private ArrayList<Message> waitingMessages = new ArrayList<>();
//    private ArrayList<CBRP_NeighbourTableEntry> neighbourTable = new ArrayList<>();
//    private ArrayList<Integer> clusterMembers = new ArrayList<>();
//    private Map<Integer, CBRP_RoutingTableEntry> routingTable = new HashMap<>();
//    private Set<Pair<Integer, Integer>> knownMessageIDs = new HashSet<>(); // format: <sourceID,broadcastID>
//
//    public CBRP_Node(int id, int x, int y) {
//        super(id, x, y);
//    }
//
//    public CBRP_Node(int id, int x, int y, int commRadius) {
//        super(x,y,id, commRadius);
//
//    }
//
//    @Override
//    public void turnOn(int runtTime) {
//        long startTime = System.currentTimeMillis();
//
//        while(System.currentTimeMillis() < startTime + runtTime) {
//            while (!messages.isEmpty()) {
//                handleMessage(messages.remove(0));
//            }
//
//            //TODO: overwrite!
//            discoverNeighbours();
//
//            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
//                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
//                while (destination == id)
//                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
//                sendMessage(new Message(id, destination, "Hello from " + id));
//                lastMessageSent = totalRunTime;
//            }
//
////            if(totalRunTime > Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION + lastNeighbourDiscovery && !updatedPaths){
////                updateRoutes();
////                updatedPaths = true;
////            }
//
////            if(id == 0)
////                move();
//
//            try {
//                Thread.sleep(Constants.NODE_DELAY);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            totalRunTime += System.currentTimeMillis() - startTime;
//        }
//    }
//
//    //TODO: STOP BEING RETARDED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
//    //TODO: Functions are for handleMessage, not for receiveMessage....fix it!
//    @Override
//    public void sendMessage(Message message) {
//        message.setSource(id);
//        if(status == 0){ //node is undecided
//            log(2, "is undecided and cannot send message " + message);
//
//        }else if(status == 1){ //node is not CH -> send to CH
//            CBRP_Message cbrpMessage = new CBRP_Message(id, clusterHeadId, MessageType.CBRP_TEXT, message.getDestination(), -1, message.getText());
//            messageRouter.sendMessage(cbrpMessage);
//            log(2, "sending to CH message " + message);
//
//        }else{
//            switch(message.getMessageType()){
//                case TEXT:
//                    //CH initiated messaege. check if route is known, and forward it.
//                    // If no route is known, discover it
//                    if(routingTable.containsKey(message.getDestination())){
//                        CBRP_Message cbrpMessage = new CBRP_Message(id, routingTable.get(message.getDestination()).nextHop,
//                                MessageType.CBRP_TEXT, message.getDestination(),
//                                routingTable.get(message.getDestination()).destinationClusterId ,message.getText());
//                        messageRouter.sendMessage(cbrpMessage);
//                    }
//                    else{
//                        waitingMessages.add(message);
//                        beginRouteDiscovery(message.getDestination());
//                    }
//                    break;
//                case CBRP_TEXT:
//                    if(message instanceof CBRP_Message cbrpMessage) {
//                        if(routingTable.containsKey(cbrpMessage.getFinalDestination())){
//                            cbrpMessage.setDestination(routingTable.get(cbrpMessage.getFinalDestination()).nextHop);
//                            messageRouter.sendMessage(cbrpMessage);
//                        }else{
//                            waitingMessages.add(message);
//                            beginRouteDiscovery(message.getDestination());
//                        }
//                    }
//                    break;
//                case CBRP_RREQ, CBRP_RERR:
//                    HashSet<Integer> clusterHeads = new HashSet<>();
//                    for(CBRP_RoutingTableEntry entry: routingTable.values()){
//                        if(entry.destinationIsClusterHead){
//                            clusterHeads.add(entry.destinationId);
//                        }
//                    }
//                    this.messageRouter.sendMessage(message, clusterHeads);
//                    break;
//                case CBRP_RREP:
//                    if(message instanceof CBRP_Message cbrpMessage){
//                        if(this.routingTable.containsKey(cbrpMessage.getFinalDestination())) {
//                            int nextHop = this.routingTable.get(cbrpMessage.getFinalDestination()).nextHop;
//                            cbrpMessage.setDestination(nextHop);
//                            this.messageRouter.sendMessage(cbrpMessage);
//                        }else{
//                            waitingMessages.add(message);
//                            beginRouteDiscovery(message.getDestination());
//                        }
//                    }
//                    break;
//                default:
//                    super.sendMessage(message);
//                    break;
//            }
//        }
//    }
//
//    @Override
//    public void handleMessage(Message message) {
//        log(1, "received " + message.getMessageType() + " from " + message.getSource());
//
//        switch (message.getMessageType()) {
//            case TEXT:
//                break;
//            case NEIGHBOUR_SYN:
//                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
//                break;
//            case NEIGHBOUR_ACK:
//                newNeighbours.add(message.getSource());
//                break;
//
//            case CBRP_TEXT:
//                break;
//
//            case CBRP_NEIGHBOUR_HELLO:
//                if(message instanceof CBRP_Message cbrpMessage){
//                    for(CBRP_NeighbourTableEntry entry: cbrpMessage.getNeighbourTable()){
//                        if(entry.id!=id){
//                            //if is not this node, and if it does not know the destination already, or, if it does, if the route is shorter than 2 (neighbour of neighbour)
//                            if(!this.routingTable.containsKey(entry.id) || this.routingTable.get(entry.id).hopCount >2) {
//                                CBRP_RoutingTableEntry routingTableEntry = new CBRP_RoutingTableEntry(entry.id, entry.clusterId, entry.isClusterHead, cbrpMessage.getSource(), 2);
//                                this.routingTable.put(entry.id, routingTableEntry);
//                            }
//                        }
//                    }
//                }
//                break;
//            case CBRP_RREQ:
//                if(message instanceof CBRP_Message cbrpMessage) {
//                    if (!this.knownMessageIDs.contains(new Pair<>(cbrpMessage.getActualSource(), cbrpMessage.getId()))) {
//                        this.knownMessageIDs.add(new Pair<>(cbrpMessage.getActualSource(), cbrpMessage.getId()));
//                        //check if node is destination clusterHead, and then check if message is for one of the members, or for itself
//                        if (cbrpMessage.getDestinationClusterId() == id) {
//                            if(status != 2){
//                                cbrpMessage.setDestination(clusterHeadId);
//                                sendMessage(cbrpMessage);
//                            }else {
//                                //TODO
//                                // CBRP_RoutingTableEntry entry = cbrpMessage.getSomething();
//                                if (cbrpMessage.getFinalDestination() == id) {
//                                    log(2, "received route to " + cbrpMessage.getFinalDestination());
//                                } else if (clusterMembers.contains(cbrpMessage.getFinalDestination())) {
//                                    log(2, "received route to " + cbrpMessage.getFinalDestination() + " requested by member");
//                                }
//                                sendWaitingMessages();
//                            }
//                        } else {
//                            //TODO: check if route is known, then send RREP, or ignore
//                        }
//                    }
//                }
//                break;
//            case CBRP_RREP:
//                if(message instanceof CBRP_Message cbrpMessage){
//                    //TODO: add Route to routing table, and try to send all messages
//                }
//                break;
//            case CBRP_RERR:
//                if(message instanceof CBRP_Message cbrpMessage) {
//                    //TODO: update routing table and send to all other CH
//                }
//                break;
//        }
//    }
//
//    private void sendWaitingMessages(){
////        ArrayList<Message> copyWaitingMessages = new ArrayList<>(waitingMessages);
////        for(Message waitingMessage : copyWaitingMessages) {
////            if (routingTable.containsKey(waitingMessage.getDestination())) {
////                int nextHop = routingTable.get(waitingMessage.getDestination()).getNextHop();
////                if(waitingMessage instanceof AODV_Message aodv_message){
////                    aodv_message.setDestination(nextHop);
////                    sendMessage(aodv_message);
//////                }else {
//////                    AODV_Message message1 = new AODV_Message(id, nextHop, MessageType.AODV_TEXT,
//////                            waitingMessage.getDestination(), waitingMessage.getText());
//////                    sendMessage(message1);
////                }
////                waitingMessages.remove(waitingMessage);
////            }
////        }
//    }
//
//    private void beginRouteDiscovery(int destination){
//        //TODO
//    }
//
//    private void updateRoutes(){
//
//    }
//
//
//}
