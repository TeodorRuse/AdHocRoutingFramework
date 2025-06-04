package org.example.licentafromzero.OLSR;

import javafx.util.Pair;
import org.example.licentafromzero.AODV.AODV_Message;
import org.example.licentafromzero.Domain.*;
import org.example.licentafromzero.Domain.Timer;

import java.util.*;
import java.util.stream.Collectors;

public class OLSR_Node extends Node {
    private ArrayList<OLSR_NeighbourTable_1hop> neighbourTable_oneHop = new ArrayList<>();
    private HashMap<Integer, OLSR_NeighbourTable_2hop> neighbourTable_twoHop = new HashMap<>();
    private HashMap<Integer, OLSR_RoutingTableEntry> routingTable = new HashMap<>();

    private ArrayList<Integer> multipleRelayPoints = new ArrayList<>();
    private ArrayList<Integer> mrpSelectors = new ArrayList<>();

    private int seqNum = 0;
    private ArrayList<Pair<Integer, Integer>> knownMessages = new ArrayList<>();

    private Timer helloTimer = new Timer(Constants.OLSR_HELLO_INTERVAL);
    private Timer tcTimer = new Timer(Constants.OLSR_TC_INTERVAL);
    private Timer resendTimer = new Timer(Constants.OLSR_RESEND_TIME);
    private Timer randomMessageTimer;

    public OLSR_Node(int x, int y, int id) {
        super(x, y, id);

        this.randomMessageTimer = new Timer(random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL);
    }

    public OLSR_Node(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);

        this.randomMessageTimer = new Timer(random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL);
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty() && System.currentTimeMillis() < startTime + runtTime) {
                handleMessage(messages.remove(0));
            }

            discoverNeighbours();
            updateTimers();

            if(id == 0)
                move();

//          TODO: attack
//            if(id == 7 && routingTable.containsKey(5)){
//                log(3,"Overflowing node 5, sending 100 random messages");
//                OLSR_Message_TEXT overflowMessage = new OLSR_Message_TEXT(id, 5, "Denial Of Service", 5);
//                for(int i=0;i<100;i++)
//                    sendMessage(overflowMessage);
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
    public void sendMessage(Message message) {

        message.setSource(id);
        switch (message.getMessageType()){
            case TEXT:
                OLSR_Message_TEXT msgTEXT = new OLSR_Message_TEXT(message.getSource(),-1, message.getText(), message.getDestination());
                if(routingTable.containsKey(msgTEXT.getFinalDestination())) {
                    msgTEXT.setDestination(routingTable.get(msgTEXT.getFinalDestination()).getNextHop());
                    messageRouter.sendMessage(msgTEXT);
                }else{
                    waitingMessages.add(msgTEXT);
                }
                break;
            case OLSR_TC:
                OLSR_Message_TC msgTC = (OLSR_Message_TC) message;
                messageRouter.sendMessage(msgTC, neighbours);
                break;
            case OLSR_HELLO:
                OLSR_Message_HELLO msgHello = (OLSR_Message_HELLO) message;
                messageRouter.sendMessage(msgHello, neighbours);
                break;
            case OLSR_TEXT:
                OLSR_Message_TEXT msgOLSRTEXT = (OLSR_Message_TEXT) message;

                if(routingTable.containsKey(msgOLSRTEXT.getFinalDestination()) &&
                        routingTable.get(msgOLSRTEXT.getFinalDestination()).getNextHop() != id) {
                    msgOLSRTEXT.setDestination(routingTable.get(msgOLSRTEXT.getFinalDestination()).getNextHop());
                    log(1, "sending message further to "+ routingTable.get(msgOLSRTEXT.getFinalDestination()).getNextHop() + " to reach " + msgOLSRTEXT.getFinalDestination());
                    messageRouter.sendMessage(msgOLSRTEXT);
                }else{
                    waitingMessages.add(msgOLSRTEXT);
                    log(1, " no route known to reach " + msgOLSRTEXT.getFinalDestination());
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
        switch (message.getMessageType()){
            case TEXT:
                break;
            case NEIGHBOUR_SYN:
                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
                break;
            case NEIGHBOUR_ACK:
                newNeighbours.add(message.getSource());
                break;


            case OLSR_TEXT:
                OLSR_Message_TEXT msgText = (OLSR_Message_TEXT) message;
                if(msgText.getFinalDestination() == id){
                    log(3, "received text: " + message.getText());
                }else{
                    sendMessage(msgText);
                }
                break;
            case OLSR_HELLO:
                OLSR_Message_HELLO msgHello = (OLSR_Message_HELLO) message;
                ArrayList<Integer> nodeNeighbours = new ArrayList<>();

                //update 2hopneighbours
                for (OLSR_NeighbourTable_1hop entry: msgHello.getNeighbours()){
                    boolean isAlreadyOneHop = false;
                    for(var one: neighbourTable_oneHop){
                        if(one.getId() == entry.getId()) isAlreadyOneHop = true;
                    }

                    if(!isAlreadyOneHop){
                        if(neighbourTable_twoHop.containsKey(entry.getId()))
                            neighbourTable_twoHop.get(entry.getId()).addNextHop(msgHello.getSource());
                        else {
                            OLSR_NeighbourTable_2hop twoHop = new OLSR_NeighbourTable_2hop(entry.getId(), msgHello.getSource());
                            neighbourTable_twoHop.put(entry.getId(), twoHop);
                        }
                        nodeNeighbours.add(entry.getId());
                    }
                }


                updateRoutingTable();

                // if mrps include this node, then build mrp hashmap
                if(msgHello.getMrpsSender().contains(id)){
                    // Only add if not already in the list
                    if(!mrpSelectors.contains(msgHello.getSource())) {
                        mrpSelectors.add(msgHello.getSource());
                    }
                } else {
                    // If this node was previously selected as MPR but is no longer, remove it
                    mrpSelectors.remove(Integer.valueOf(msgHello.getSource()));
                }
                break;


            case OLSR_TC:
                //check if message is known | add to known msgIds | update routing table | forward if mrp;
                OLSR_Message_TC msgTC = (OLSR_Message_TC) message;

                if(!knownMessages.contains(msgTC.getMsgId())){
                    knownMessages.add(msgTC.getMsgId());

                    updateRoutingTable(msgTC);

                    if(!mrpSelectors.isEmpty()){
                        sendMessage(msgTC);
                    }
                }else{
                    log(0, " TC message already received, ignoring");
                }
                break;
        }
    }

    public void updateTimers(){
        //Random TEXT message
        if(randomMessageTimer.tick(totalRunTime) && totalRunTime > Constants.NODE_STARTUP_TIME){
            int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
            while (destination == id)
                destination = random.nextInt(Constants.SIMULATION_NR_NODES);
            sendMessage(new Message(id, destination, "Hello from " + id));
        }

        // HELLO_MESSAGE
        if(helloTimer.tick(totalRunTime)){
            electMRPs();
            OLSR_Message_HELLO msg = new OLSR_Message_HELLO(id, -1, neighbourTable_oneHop, multipleRelayPoints);
            sendMessage(msg);
        }

        // TC_MESSAGE
        if(tcTimer.tick(totalRunTime)){
            // Only send TC messages if we have MPR selectors
            if(!mrpSelectors.isEmpty()) {
                seqNum++;
                ArrayList<Integer> uniqueSelectors = new ArrayList<>(new HashSet<>(mrpSelectors));
                OLSR_Message_TC msg = new OLSR_Message_TC(id, -1, new Pair<>(id, seqNum), uniqueSelectors);
                sendMessage(msg);
            }
        }

        //resend messages
        if(resendTimer.tick(totalRunTime)){
            for(Message msg : waitingMessages){
                OLSR_Message_TEXT msgTxt = (OLSR_Message_TEXT) msg;
                if(routingTable.containsKey(msgTxt.getFinalDestination())){
                    log(2, "sending waiting message to " + msgTxt.getFinalDestination());
                    sendMessage(msgTxt);
                }
            }
        }
    }

    @Override
    public void discoverNeighbours() {
        //modify with proper OLSR way (from HELLO), this is improvised!
        super.discoverNeighbours();

        neighbourTable_oneHop.clear();
        for(int neighbour: neighbours){
            OLSR_NeighbourTable_1hop entry = new OLSR_NeighbourTable_1hop(neighbour, System.currentTimeMillis());
            neighbourTable_oneHop.add(entry);
        }

        ArrayList<Integer> removeKeys = new ArrayList<>();
        for(var key: neighbourTable_twoHop.keySet()){
            for(var oneHop: neighbourTable_oneHop)
                if(oneHop.getId() == neighbourTable_twoHop.get(key).getId()){
                    removeKeys.add(key);
                    break;
                }
        }
        for(var key: removeKeys)
            neighbourTable_twoHop.remove(key);
    }

    public void updateRoutingTable(){ //read all info from one hop and 2 hop tables and update routing table

        ageRoutesAndNeighbours();

        //first add the 2hop
        for(int key: neighbourTable_twoHop.keySet()){
            OLSR_NeighbourTable_2hop neighbour = neighbourTable_twoHop.get(key);
            OLSR_RoutingTableEntry entry = new OLSR_RoutingTableEntry(neighbour.getId(), neighbour.getNextHopNewest(), 2);
            routingTable.put(entry.getDestination(), entry);
        }

        //add 1hop neighbours last to overwrite 2 hop neighbours if they exist
        for(OLSR_NeighbourTable_1hop neighbour: neighbourTable_oneHop){
            OLSR_RoutingTableEntry entry = new OLSR_RoutingTableEntry(neighbour.getId(), neighbour.getId(), 1);
            routingTable.put(entry.getDestination(), entry);
        }

    }

    public void updateRoutingTable(OLSR_Message_TC msg) {
        // Age routes and neighbors first
        ageRoutesAndNeighbours();

        // Get the originator of the TC message
        int tcOriginatorAddr = msg.getOriginalSource();

        // Skip processing if we don't have a route to the TC originator
        if (!routingTable.containsKey(tcOriginatorAddr) &&
                !neighbourTable_oneHop.stream().anyMatch(n -> n.getId() == tcOriginatorAddr)) {
            return;
        }

        // Get the next hop to reach the TC originator
        int nextHopToTcOriginator = getNextHopToNode(tcOriginatorAddr);

        // Get the hop count to the TC originator
        int hopCountToTcOriginator = getHopCountToNode(tcOriginatorAddr);

        // For each advertised node in the TC message
        for (int advertisedNodeAddr : msg.getAdvertisedNodes()) {
            // Skip if the advertised node is this node
            if (advertisedNodeAddr == id) {
                continue;
            }

            // Calculate the new hop count to this advertised node
            int newHopCount = hopCountToTcOriginator + 1;

            // Check if we already have a route to this node
            if (routingTable.containsKey(advertisedNodeAddr)) {
                OLSR_RoutingTableEntry existingEntry = routingTable.get(advertisedNodeAddr);

                // Only update if the new route is better (lower hop count)
                if (newHopCount < existingEntry.getHopCount()) {
                    existingEntry.setNextHop(nextHopToTcOriginator);
                    existingEntry.setHopCount(newHopCount);
                    existingEntry.setTimeReceived(System.currentTimeMillis());
                }
            } else {
                // We don't have a route to this node yet, create a new entry
                OLSR_RoutingTableEntry newEntry = new OLSR_RoutingTableEntry(
                        advertisedNodeAddr,
                        nextHopToTcOriginator,
                        newHopCount
                );
                routingTable.put(advertisedNodeAddr, newEntry);
            }
        }
    }

    private int getHopCountToNode(int nodeAddr) {
        if (neighbourTable_oneHop.stream().anyMatch(n -> n.getId() == nodeAddr)) {
            return 1;
        }

        if (routingTable.containsKey(nodeAddr)) {
            return routingTable.get(nodeAddr).getHopCount();
        }

        return Integer.MAX_VALUE;
    }

    private int getNextHopToNode(int nodeAddr) {
        if (neighbourTable_oneHop.stream().anyMatch(n -> n.getId() == nodeAddr)) {
            return nodeAddr;
        }

        if (routingTable.containsKey(nodeAddr)) {
            return routingTable.get(nodeAddr).getNextHop();
        }

        return -1;
    }

    public void electMRPs() {
        // Clear the current MPR set
        multipleRelayPoints.clear();

        // Step 1: Add all neighbors with willingness = WILL_ALWAYS to the MPR set
        // For simplicity in this implementation, we'll just add neighbors that are necessary

        // Step 2: Calculate the degree of each 1-hop neighbor
        Map<Integer, Integer> nodeDegrees = new HashMap<>();
        for (OLSR_NeighbourTable_1hop neighbor : neighbourTable_oneHop) {
            // Count how many 2-hop neighbors this 1-hop neighbor can reach
            int degree = 0;
            for (Map.Entry<Integer, OLSR_NeighbourTable_2hop> entry : neighbourTable_twoHop.entrySet()) {
                OLSR_NeighbourTable_2hop twoHopNeighbor = entry.getValue();
                if (twoHopNeighbor.getNextHops().containsKey(neighbor.getId())) {
                    degree++;
                }
            }
            nodeDegrees.put(neighbor.getId(), degree);
        }

        // Step 3: Add to the MPR set those nodes which are the only ones to provide reachability to a 2-hop neighbor
        Set<Integer> coveredTwoHopNeighbors = new HashSet<>();

        // First, identify 2-hop neighbors that can only be reached through one 1-hop neighbor
        for (Map.Entry<Integer, OLSR_NeighbourTable_2hop> entry : neighbourTable_twoHop.entrySet()) {
            int twoHopNeighborId = entry.getKey();
            OLSR_NeighbourTable_2hop twoHopNeighbor = entry.getValue();

            // If this 2-hop neighbor can only be reached through one 1-hop neighbor
            if (twoHopNeighbor.getNextHops().size() == 1) {
                int oneHopNeighborId = twoHopNeighbor.getNextHops().keySet().iterator().next();
                if (!multipleRelayPoints.contains(oneHopNeighborId)) {
                    multipleRelayPoints.add(oneHopNeighborId);
                }
                coveredTwoHopNeighbors.add(twoHopNeighborId);
            }
        }

        // Step 4: While there are still uncovered 2-hop neighbors
        while (coveredTwoHopNeighbors.size() < neighbourTable_twoHop.size()) {
            // Find the 1-hop neighbor that covers the most uncovered 2-hop neighbors
            int bestNeighborId = -1;
            int bestReachability = 0;

            for (OLSR_NeighbourTable_1hop neighbor : neighbourTable_oneHop) {
                // Skip if already in MPR set
                if (multipleRelayPoints.contains(neighbor.getId())) {
                    continue;
                }

                // Count how many uncovered 2-hop neighbors this 1-hop neighbor can reach
                int reachability = 0;
                for (Map.Entry<Integer, OLSR_NeighbourTable_2hop> entry : neighbourTable_twoHop.entrySet()) {
                    int twoHopNeighborId = entry.getKey();
                    OLSR_NeighbourTable_2hop twoHopNeighbor = entry.getValue();

                    // If this 2-hop neighbor is not yet covered and can be reached through this 1-hop neighbor
                    if (!coveredTwoHopNeighbors.contains(twoHopNeighborId) &&
                            twoHopNeighbor.getNextHops().containsKey(neighbor.getId())) {
                        reachability++;
                    }
                }

                // Update best neighbor if this one has better reachability
                if (reachability > bestReachability) {
                    bestReachability = reachability;
                    bestNeighborId = neighbor.getId();
                } else if (reachability == bestReachability && bestReachability > 0) {
                    // If equal reachability, choose the one with higher degree
                    if (nodeDegrees.getOrDefault(neighbor.getId(), 0) >
                            nodeDegrees.getOrDefault(bestNeighborId, 0)) {
                        bestNeighborId = neighbor.getId();
                    }
                }
            }

            // If we found a neighbor that covers some uncovered 2-hop neighbors
            if (bestNeighborId != -1) {
                multipleRelayPoints.add(bestNeighborId);

                // Mark all 2-hop neighbors reachable through this 1-hop neighbor as covered
                for (Map.Entry<Integer, OLSR_NeighbourTable_2hop> entry : neighbourTable_twoHop.entrySet()) {
                    int twoHopNeighborId = entry.getKey();
                    OLSR_NeighbourTable_2hop twoHopNeighbor = entry.getValue();

                    if (twoHopNeighbor.getNextHops().containsKey(bestNeighborId)) {
                        coveredTwoHopNeighbors.add(twoHopNeighborId);
                    }
                }
            } else {
                // No more neighbors can cover additional 2-hop neighbors, break the loop
                break;
            }
        }

        // Step 5: Optimization - Remove redundant MPRs if possible
        // We'll use a safer approach to avoid index out of bounds errors
        List<Integer> mprsToKeep = new ArrayList<>();

        for (Integer mprId : multipleRelayPoints) {
            // Create a temporary set without this MPR
            List<Integer> tempMPRs = new ArrayList<>(multipleRelayPoints);
            tempMPRs.remove(mprId);

            // Check if all 2-hop neighbors are still covered without this MPR
            boolean allCovered = true;
            for (Map.Entry<Integer, OLSR_NeighbourTable_2hop> entry : neighbourTable_twoHop.entrySet()) {
                int twoHopNeighborId = entry.getKey();
                OLSR_NeighbourTable_2hop twoHopNeighbor = entry.getValue();

                boolean covered = false;
                for (Integer remainingMPR : tempMPRs) {
                    if (twoHopNeighbor.getNextHops().containsKey(remainingMPR)) {
                        covered = true;
                        break;
                    }
                }

                if (!covered) {
                    allCovered = false;
                    break;
                }
            }

            // If this MPR is necessary (not all neighbors covered without it), keep it
            if (!allCovered) {
                mprsToKeep.add(mprId);
            }
        }

        // Replace the MPR set with only the necessary MPRs
        multipleRelayPoints.clear();
        multipleRelayPoints.addAll(mprsToKeep);
    }

    public void ageRoutesAndNeighbours() {
        // Clear old routes and neighbours
        ArrayList<OLSR_NeighbourTable_1hop> toRemove1Hop = new ArrayList<>();
        for(OLSR_NeighbourTable_1hop neighbour: neighbourTable_oneHop){
            if(neighbour.isExpired())
                toRemove1Hop.add(neighbour);
        }
        neighbourTable_oneHop.removeAll(toRemove1Hop);

        ArrayList<Integer> toRemove2Hop = new ArrayList<>();
        for(int key: neighbourTable_twoHop.keySet()) {
            OLSR_NeighbourTable_2hop neighbour = neighbourTable_twoHop.get(key);
            neighbour.updateNextHops();
            if(neighbour.isExpired())
                toRemove2Hop.add(key);
        }
        for(int key: toRemove2Hop) {
            neighbourTable_twoHop.remove(key);
        }

        ArrayList<Integer> toRemoveRouting = new ArrayList<>();
        for(int key: routingTable.keySet()) {
            OLSR_RoutingTableEntry entry = routingTable.get(key);
            if(entry.isExpired())
                toRemoveRouting.add(key);
        }
        for(int key: toRemoveRouting) {
            routingTable.remove(key);
        }
    }

    public ArrayList<OLSR_NeighbourTable_1hop> getNeighbourTable_oneHop() {
        return neighbourTable_oneHop;
    }

    public HashMap<Integer, OLSR_NeighbourTable_2hop> getNeighbourTable_twoHop() {
        return neighbourTable_twoHop;
    }

    public HashMap<Integer, OLSR_RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public ArrayList<Integer> getMultipleRelayPoints() {
        return multipleRelayPoints;
    }

    public ArrayList<Integer> getMrpSelectors() {
        return mrpSelectors;
    }

    public String prettyPrintNodeState() {
        StringBuilder sb = new StringBuilder();

        // Format waiting messages
        sb.append("\n===== WAITING MESSAGES =====\n");
        if (waitingMessages.isEmpty()) {
            sb.append("No waiting messages\n");
        } else {
            sb.append(waitingMessages.stream()
                    .map(Message::prettyPrint)
                    .collect(Collectors.joining("\n ")));
        }

        // Format one-hop neighbors
        sb.append("\n\n===== ONE-HOP NEIGHBORS =====\n");
        if (neighbourTable_oneHop.isEmpty()) {
            sb.append("No one-hop neighbors\n");
        } else {
            for (OLSR_NeighbourTable_1hop neighbor : neighbourTable_oneHop) {
                sb.append(String.format("Node ID: %d | Last Updated: %s | Expired: %s\n",
                        neighbor.getId(),
                        neighbor.getTimeReceived(),
                        neighbor.isExpired() ? "Yes" : "No"));
            }
        }

        // Format two-hop neighbors
        sb.append("\n===== TWO-HOP NEIGHBORS =====\n");
        if (neighbourTable_twoHop.isEmpty()) {
            sb.append("No two-hop neighbors\n");
        } else {
            for (Map.Entry<Integer, OLSR_NeighbourTable_2hop> entry : neighbourTable_twoHop.entrySet()) {
                OLSR_NeighbourTable_2hop twoHopNeighbor = entry.getValue();
                sb.append(String.format("Two-Hop Node ID: %d | Reachable through: ", entry.getKey()));

                if (twoHopNeighbor.getNextHops().isEmpty()) {
                    sb.append("No valid next hops");
                } else {
                    sb.append(twoHopNeighbor.getNextHops().keySet().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")));
                }
                sb.append(" | Expired: ").append(twoHopNeighbor.isExpired() ? "Yes" : "No").append("\n");
            }
        }

        // Format routing table
        sb.append("\n===== ROUTING TABLE =====\n");
        if (routingTable.isEmpty()) {
            sb.append("No routing entries\n");
        } else {
            for (Map.Entry<Integer, OLSR_RoutingTableEntry> entry : routingTable.entrySet()) {
                OLSR_RoutingTableEntry route = entry.getValue();
                sb.append(String.format("Destination: %d | Next Hop: %d | Hop Count: %d | Last Updated: %s | Expired: %s\n",
                        route.getDestination(),
                        route.getNextHop(),
                        route.getHopCount(),
                        route.getTimeReceived(),
                        route.isExpired() ? "Yes" : "No"));
            }
        }

        // Format MPR list
        sb.append("\n===== MULTIPLE RELAY POINTS (MPRs) =====\n");
        if (multipleRelayPoints.isEmpty()) {
            sb.append("No MPRs selected\n");
        } else {
            sb.append("MPRs: ").append(multipleRelayPoints.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")))
                    .append("\n");
        }

        // Format MPR selectors
        sb.append("\n===== MPR SELECTORS =====\n");
        if (mrpSelectors.isEmpty()) {
            sb.append("No nodes have selected this node as MPR\n");
        } else {
            sb.append("Selectors: ").append(mrpSelectors.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")))
                    .append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toInfo() {
        return super.toInfo() + "\n\n" +
                "OLSR Info\n" +
                "---------\n" +
                "MPRs: " + multipleRelayPoints + "\n" +
                "MPR Selectors: " + mrpSelectors + "\n" +
                "Sequence Number: " + seqNum + "\n" +
                "Known Messages: " + knownMessages.size() + "\n" +
                "One-Hop Neighbours: " + neighbourTable_oneHop.size() + "\n" +
                "Two-Hop Neighbours: " + neighbourTable_twoHop.size() + "\n" +
                "Routing Table: " + routingTable.size();
    }
}
