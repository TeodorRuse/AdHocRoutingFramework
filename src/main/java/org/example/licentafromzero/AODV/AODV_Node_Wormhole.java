package org.example.licentafromzero.AODV;

import javafx.util.Pair;
import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.util.ArrayList;

public class AODV_Node_Wormhole extends AODV_Node{

    protected boolean oneTime = true;
    public AODV_Node_Wormhole(int x, int y, int id) {
        super(x, y, id);
    }

    public AODV_Node_Wormhole(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            discoverNeighbours();

            if(id == 2 && totalRunTime > Constants.NODE_STARTUP_TIME && oneTime){
                oneTime = false;
                sendMessage(new Message(id, 9, "Hello from " + id));
            }

//            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
//                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
//                while (destination == id)
//                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
//                sendMessage(new Message(id, destination, "Hello from " + id));
//                lastMessageSent = totalRunTime;
//            }

            if(totalRunTime > Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION + lastNeighbourDiscovery && !updatedPaths){
                updateRoutes();
                updatedPaths = true;
            }

            if(id == 0)
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
    public void sendMessage(Message message) {
        message.setSource(id);

        switch(message.getMessageType()){
            case AODV_RREQ:
                if(message instanceof AODV_Message aodvMessage){
                    log(2, "sending RREQ" + stringifyId(aodvMessage) + " to all");
                    broadcastId++;
                    sequenceNumber++;
                    messageRouter.sendMessage(aodvMessage, neighbours);
                }
                break;
            case AODV_RERR:
                if(message instanceof AODV_Message aodvMessage){
                    log(2, "sending RERR(" + stringifyId(aodvMessage) + ") all neighbours for " + aodvMessage.getUnreachableAddresses());
                    broadcastId++;
                    sequenceNumber++;
                    messageRouter.sendMessage(aodvMessage, neighbours);
                }
                break;
            case AODV_TEXT, AODV_RREP:
                if(message instanceof AODV_Message aodvMessage){
                    messageRouter.sendMessage(aodvMessage);
                }
                break;
            case TEXT:
                if(routingTable.containsKey(message.getDestination())){
                    log(2, "using known route to send to " + message.getDestination());
                    AODV_RoutingTableEntry tableEntry = routingTable.get(message.getDestination());

                    AODV_Message aodv_message = new AODV_Message(id, tableEntry.nextHop, MessageType.AODV_TEXT,
                            message.getDestination(), message.getText());
                    messageRouter.sendMessage(aodv_message);
                }else{
                    waitingMessages.add(message);

                    log(1, "beginning route discovery to " + message.getDestination());
                    beginRouteDiscovery(message.getDestination());

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

                            sendWaitingMessages();
                        }

                        //TODO: Attack!!
//                        if(id == 6 && aodvMessage.getSource() == 2){
//                            log(4, "received RREQ to 2, attacking and sending fake RREP");
//                            AODV_Message rrep = new AODV_Message(id, aodvMessage.getSource(), MessageType.AODV_RREP,
//                                   9, aodvMessage.getOriginalSource(), 10000);
//                            sendMessage(rrep);
//
//                            AODV_RoutingTableEntry tableEntry = new AODV_RoutingTableEntry(9,
//                                    7, 0, 6, totalRunTime);
//                            routingTable.put(aodvMessage.getOriginalSource(), tableEntry);
//
//                            break;
//                        }


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

                        sendWaitingMessages();
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
                            log(2, "forwarding message to " + aodvMessage.getDestination() + " to reach " + aodvMessage.getFinalDestination());
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
}
