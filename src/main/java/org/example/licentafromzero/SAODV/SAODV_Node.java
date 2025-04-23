package org.example.licentafromzero.SAODV;

import javafx.util.Pair;
import org.example.licentafromzero.AODV.AODV_Message;
import org.example.licentafromzero.AODV.AODV_RoutingTableEntry;
import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;
import org.example.licentafromzero.Domain.Node;

import java.security.*;
import java.util.*;

public class SAODV_Node extends Node {
    private int sequenceNumber = 0;
    private int broadcastId = 0;
    private boolean updatedPaths = true;
    boolean tempula = false;

    private ArrayList<Message> waitingMessages = new ArrayList<>();
    private Map<Integer, SAODV_RoutingTableEntry> routingTable = new HashMap<>();
    private Set<Pair<Integer, Integer>> knownMessageIDs = new HashSet<>(); // format: <sourceID,broadcastID>

    private KeyPair keyPair;
    private Map<Integer, PublicKey> keyChain = new HashMap<>();

    //TODO: copy the doubleSign and add it to the routint table, and use it
    //TODO: Some messages RREQ are falsly marked as fake, even though source and signedBy seem corect
    //TODO: some text messagees are actually signed by random nodes and detected as fake.
    //TODO: test logic for when to send RREP and when to forward it if too busy

    public SAODV_Node(int x, int y, int id, KeyPair keyPair) {
        super(x, y, id);
        SAODV_RoutingTableEntry routeToSelf = new SAODV_RoutingTableEntry(id,id,0,0, totalRunTime);
        this.keyPair = keyPair;
    }

    public SAODV_Node(int x, int y, int id, int communicationRadius, KeyPair keyPair) {
        super(x, y, id, communicationRadius);
        SAODV_RoutingTableEntry routeToSelf = new SAODV_RoutingTableEntry(id,id,0,0, totalRunTime);
        routingTable.put(id, routeToSelf);
        this.keyPair = keyPair;
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            if(totalRunTime == -1 ){
                discoverNeighbours();
            }

//            if(totalRunTime == -1 || totalRunTime - lastNeighbourDiscovery >= Constants.NODE_NEIGHBOUR_DISCOVERY_PERIOD){
//                discoverNeighbours();
//                updatedPaths = false;
//                lastNeighbourDiscovery = totalRunTime;
//                if(Constants.LOG_DETAILS < 2)
//                    System.out.println("Node " + id + " discovering neighbours");
//            }

            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                while (destination == id)
                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                sendMessage(new Message(id, destination, "Hello from " + id));
                lastMessageSent = totalRunTime;
            }

//            if(id == 0 && totalRunTime > Constants.NODE_STARTUP_TIME && !tempula){
//                sendMessage(new Message(id, 9, "Helloo from " + id));
//                tempula = true;
//            }

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
        if(message instanceof SAODV_Message saodvMessage){
            if(saodvMessage.getOriginalSource() == id ||  saodvMessage.getActualSource() == id)
                saodvMessage.signMessage(id, keyPair.getPrivate());

            if(saodvMessage.getMessageType() == MessageType.SAODV_RREQ){
                saodvMessage.setDoubleSignature(generateDoubleSignature());
                log(2, "sending RREQ" + stringifyId(saodvMessage) + " to all");
                broadcastId++;
                sequenceNumber++;
                messageRouter.sendMessage(saodvMessage, neighbours);
            }else if(saodvMessage.getMessageType() == MessageType.SAODV_RERR){
                log(2, "sending RERR(" + stringifyId(saodvMessage) + ") all neighbours for " + saodvMessage.getUnreachableAddresses());
                broadcastId++;
                sequenceNumber++;
                messageRouter.sendMessage(saodvMessage, neighbours);
            }else{
                messageRouter.sendMessage(saodvMessage);
            }
        }else{
            if(message.getMessageType() == MessageType.TEXT){
                if(routingTable.containsKey(message.getDestination())){
                    log(2, "using known route to send to " + message.getDestination());
                    SAODV_RoutingTableEntry tableEntry = routingTable.get(message.getDestination());

                    SAODV_Message saodv_message = new SAODV_Message(id, tableEntry.nextHop, MessageType.SAODV_TEXT,
                            message.getDestination(), message.getText());

                    saodv_message.signMessage(id, keyPair.getPrivate());
                    saodv_message.setDoubleSignature(generateDoubleSignature());

                    messageRouter.sendMessage(saodv_message);
                }else{
                    waitingMessages.add(message);

                    log(2, " beginning route discovery to " + message.getDestination());
                    SAODV_Message rreq = new SAODV_Message(id, MessageType.SAODV_RREQ, message.getDestination(), sequenceNumber, -1, broadcastId);
                    this.knownMessageIDs.add(new Pair<>(id, broadcastId));

                    rreq.signMessage(id, keyPair.getPrivate());
                    rreq.setDoubleSignature(generateDoubleSignature());

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
                neighbours.add(message.getSource());
                break;

            case SAODV_RREQ:
                if(message instanceof SAODV_Message saodvMessage){
                    //drops it is it knows it
                    //incerase it's hopCount
                    //check if the node is the destination!
                    //check if node knows about finalDestination, if yes, send RREP.
                    //else, forward it to all neighbours
                    //Add info about sourceSeqNum to routing table - increase it
                    Pair<Integer, Integer> msgId = new Pair<>(saodvMessage.getOriginalSource(), saodvMessage.getBroadcastId());
                    if(!knownMessageIDs.contains(msgId)){
                        knownMessageIDs.add(msgId);

                        if(!saodvMessage.verifySignature( keyChain.get(saodvMessage.getOriginalSource()))) {
                            log(2, "message RREQ " + stringifyId(saodvMessage) + " is fake, ignoring " + saodvMessage);
                            break;
                        }
                        saodvMessage.increaseHopCount();
                        //add route to source of RREQ
                        if(!routingTable.containsKey(saodvMessage.getOriginalSource()) ||
                                routingTable.get(saodvMessage.getOriginalSource()).getDestSeqNum() < saodvMessage.getSourceSeqNum() ||
                                routingTable.get(saodvMessage.getOriginalSource()).getHopCount() > saodvMessage.getHopCount()){

                            SAODV_RoutingTableEntry tableEntry = new SAODV_RoutingTableEntry(saodvMessage.getOriginalSource(),
                                    saodvMessage.getSource(), saodvMessage.getSourceSeqNum(), saodvMessage.getHopCount(), totalRunTime);
                            routingTable.put(saodvMessage.getOriginalSource(), tableEntry);
                        }


                        //check if the node is finalDestination
                        if(saodvMessage.getFinalDestination() == id){
                            log(2, "is the finalDestination for: " + stringifyId(saodvMessage) + " -> " + saodvMessage.getFinalDestination() + " sending RREP");
                            SAODV_Message rrep = new SAODV_Message(id, saodvMessage.getSource(), MessageType.SAODV_RREP,
                                    id, saodvMessage.getOriginalSource(), sequenceNumber);
                            sequenceNumber++;
                            sendMessage(rrep);
                        }else if(routingTable.containsKey(saodvMessage.getFinalDestination())){
                            //if node is not the finalDestination, but it knows a fresher route
                            SAODV_RoutingTableEntry routingTableEntry = routingTable.get(saodvMessage.getFinalDestination());
                            if(routingTableEntry.destSeqNum > saodvMessage.getDestSeqNum() && messages.size() < Constants.NODE_SAODV_FORWARD_BUFFER_SIZE){
                                log(2, "knows route, and has fresher info, sending RREP");
                                SAODV_Message rrep = new SAODV_Message(id, saodvMessage.getSource(), MessageType.SAODV_RREP,
                                        routingTableEntry.destAddr, saodvMessage.getOriginalSource(), routingTableEntry.destSeqNum);
                                sendMessage(rrep);
                            }
                        }
                        else{
                            sendMessage(saodvMessage);
                        }
                    }else{
                        log(1, "RREQ( " +stringifyId(saodvMessage) + " -> " + saodvMessage.getFinalDestination() +" )is known, discarding");
                    }
                }
                break;
            case SAODV_RREP:
                if(message instanceof SAODV_Message saodvMessage){
                    //check if  node is the final destinaition
                    //add it to the routing table
                    //if it is final destiation stop, else, dorward it
                    //add route to source of RREQ

                    if(!saodvMessage.verifySignature( keyChain.get(saodvMessage.getOriginalSource()))) {
                        log(2, "message RREP " + stringifyId(saodvMessage) + " is fake, ignoring " + saodvMessage);
                        break;
                    }

                    saodvMessage.increaseHopCount();
                    if(saodvMessage.getActualSource() != saodvMessage.getOriginalSource()){
                        log(2, "RREP is not from the target node, but a cached one!");
                    }

                    if(!routingTable.containsKey(saodvMessage.getOriginalSource()) ||
                            routingTable.get(saodvMessage.getOriginalSource()).getDestSeqNum() < saodvMessage.getSourceSeqNum() ||
                            routingTable.get(saodvMessage.getOriginalSource()).getHopCount() > saodvMessage.getHopCount()){

                        SAODV_RoutingTableEntry tableEntry = new SAODV_RoutingTableEntry(saodvMessage.getOriginalSource(),
                                saodvMessage.getSource(), saodvMessage.getSourceSeqNum(), saodvMessage.getHopCount(), totalRunTime);
                        routingTable.put(saodvMessage.getOriginalSource(), tableEntry);
                    }
                    if(saodvMessage.getFinalDestination() != id){
                        if(routingTable.containsKey(saodvMessage.getFinalDestination())) {
                            saodvMessage.setDestination(routingTable.get(saodvMessage.getFinalDestination()).getNextHop());
                            sendMessage(saodvMessage);
                        }else{
                            log(2, "cannot forward message to " + saodvMessage.getFinalDestination() + " because route is broken, adding to waiting messages");
                            waitingMessages.add(saodvMessage);
                        }
                    }else{
                        log(2, "received requested route to " + saodvMessage.getOriginalSource());
                        //loop over all messages in waitingMessages and send them if  there is a destination:

                        ArrayList<Message> copyWaitingMessages = new ArrayList<>(waitingMessages);
                        for(Message waitingMessage : copyWaitingMessages) {
                            if (routingTable.containsKey(waitingMessage.getDestination())) {
                                int nextHop = routingTable.get(waitingMessage.getDestination()).getNextHop();
                                if(waitingMessage instanceof SAODV_Message aodv_message){
                                    aodv_message.setDestination(nextHop);
                                    sendMessage(saodvMessage);
                                }else {
                                    SAODV_Message message1 = new SAODV_Message(id, nextHop, MessageType.SAODV_TEXT,
                                            waitingMessage.getDestination(), waitingMessage.getText());
//                                    message1.signMessage(id, keyPair.getPrivate());
                                    sendMessage(message1);
                                }
                                waitingMessages.remove(waitingMessage);

                            }
                        }

                    }
                }
                break;
            case SAODV_TEXT:
                if(message instanceof SAODV_Message saodvMessage){
                    if(!saodvMessage.verifySignature( keyChain.get(saodvMessage.getOriginalSource()))) {
                        log(2, "message TEXT " + stringifyId(saodvMessage) + " is fake, ignoring " + saodvMessage);
                        break;
                    }

                    if(saodvMessage.getFinalDestination() == id){
                        log(3, "received text: " + message.getText());
                    }else{
                        //if it reatches this the route should be already known, no need to check it
                        //however better safe then sorry
                        if(routingTable.containsKey(saodvMessage.getFinalDestination())) {
                            SAODV_RoutingTableEntry routingTableEntry = routingTable.get(saodvMessage.getFinalDestination());
                            saodvMessage.setDestination(routingTableEntry.nextHop);
                            sendMessage(saodvMessage);
                        }else{
                            log(2, "cannot forward message to " + saodvMessage.getFinalDestination() + " because route is broken, restarting route discovery");
                            sendMessage(new Message(saodvMessage.getOriginalSource(), saodvMessage.getFinalDestination(), saodvMessage.getText()));
                        }
                    }
                }
                break;
            case SAODV_RERR:
                if(message instanceof SAODV_Message saodvMessage){
                    if(!saodvMessage.verifySignature( keyChain.get(saodvMessage.getOriginalSource()))) {
                        log(2, "message RRER " + stringifyId(saodvMessage) + " is fake, ignoring " + saodvMessage);
                        break;
                    }

                    Pair<Integer, Integer> id = new Pair<>(saodvMessage.getOriginalSource(), saodvMessage.getBroadcastId());
                    if(!knownMessageIDs.contains(id)){
                        knownMessageIDs.add(id);
                        sendMessage(saodvMessage);
                        updateRoutes(saodvMessage);
                    }
                }
                break;
        }
    }

    public String stringifyId(SAODV_Message saodvMessage){
        return "[" + saodvMessage.getOriginalSource() + "|" + saodvMessage.getSourceSeqNum() + "]";
    }

    public Map<Integer, SAODV_RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }


    private void updateRoutes(){
        ArrayList<Integer> routesToRemove = new ArrayList<>();
        for (Map.Entry<Integer, SAODV_RoutingTableEntry> entry : routingTable.entrySet()) {
            SAODV_RoutingTableEntry rte = entry.getValue();

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
            SAODV_Message rerr = new SAODV_Message(id, MessageType.SAODV_RERR, sequenceNumber, broadcastId, routesToRemove);
            sendMessage(rerr);
        }

        for (Integer key : routesToRemove) {
            routingTable.remove(key);
        }
    }

    //for updating broken routes received by RERR
    private void updateRoutes(SAODV_Message saodvMessage) {
        ArrayList<Integer> unreachableAddresses = saodvMessage.getUnreachableAddresses();
        int rerrSenderId = saodvMessage.getOriginalSource();
        ArrayList<Integer> routesToRemove = new ArrayList<>();

        for (Integer unreachableDest : unreachableAddresses) {
            SAODV_RoutingTableEntry rte = routingTable.get(unreachableDest);
            if (rte != null && rte.nextHop == rerrSenderId) {
                log(2, " removing route to " + unreachableDest + " via broken next hop " + rte.nextHop);
                routesToRemove.add(unreachableDest);
            }
        }

        for (Integer dest : routesToRemove) {
            routingTable.remove(dest);
        }
    }

    private byte[] generateDoubleSignature(){
        try {
            String fakeRREP = new AODV_Message(id, -1, MessageType.SAODV_RREP, id, -1, sequenceNumber).toString();
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(keyPair.getPrivate());
            signer.update(fakeRREP.getBytes());
            return signer.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Message> getWaitingMessages() {
        return filterMessages(waitingMessages);
    }

    public ArrayList<Message> getWaitingControlMessages() {
        ArrayList<Message> controlMessages = new ArrayList<>();
        for (Message message : waitingMessages) {
            if (message.getMessageType() != MessageType.TEXT &&
                    message.getMessageType() != MessageType.SAODV_TEXT) {
                controlMessages.add(message);
            }
        }
        return filterMessages(controlMessages);
    }

    public ArrayList<Message> filterMessages(ArrayList<Message> waitingMessages) {
        ArrayList<Integer> unreachable = new ArrayList<>();
//        unreachable.addAll(Arrays.asList(5,7,8));
        ArrayList<Message> filteredMessages = new ArrayList<>();

        for (Message message : waitingMessages) {
            if (!unreachable.contains(message.getSource()) &&
                    !unreachable.contains(message.getDestination())) {
                filteredMessages.add(message);
            }
        }
        return filteredMessages;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public void setKeyChain(Map<Integer, PublicKey> keyChain) {
        this.keyChain = keyChain;
    }
}
