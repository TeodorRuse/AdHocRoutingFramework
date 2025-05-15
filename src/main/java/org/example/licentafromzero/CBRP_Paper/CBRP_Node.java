package org.example.licentafromzero.CBRP_Paper;

import javafx.util.Pair;
import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;
import org.example.licentafromzero.Domain.Node;

import java.util.*;

public class CBRP_Node extends Node {
    // Node status constants
    private static final int C_UNDECIDED = 0;
    private static final int C_HEAD = 1;
    private static final int C_MEMBER = 2;

    // Link status constants
    private static final int LINK_BIDIRECTIONAL = 0;
    private static final int LINK_FROM = 1;
    private static final int LINK_TO = 2;

    // Node state
    private int nodeStatus = C_UNDECIDED;
    private int sequenceNumber = 0;
    private int broadcastId = 0;
    private Set<Pair<Integer, Integer>> knownRREQs = new HashSet<>(); // <sourceID, broadcastID>
    private long lastHelloSent = 0;
    private long uTimer = 0; // Timer for undecided state
    private long cTimer = 0; // Timer for cluster head contention
    private boolean uTimerActive = false;
    private boolean cTimerActive = false;

    // Routing tables and caches
    private Map<Integer, CBRP_NeighborTableEntry> neighborTable = new HashMap<>();
    private Map<Integer, List<CBRP_ClusterAdjacencyEntry>> clusterAdjacencyTable = new HashMap<>();
    private Map<Integer, List<Integer>> twoHopTopology = new HashMap<>(); // Node -> List of its neighbors
    private Map<String, List<Integer>> routeCache = new HashMap<>(); // Destination -> Route
    private ArrayList<Message> waitingMessages = new ArrayList<>();

    public CBRP_Node(int x, int y, int id) {
        super(x, y, id);
        // Add self to neighbor table
        CBRP_NeighborTableEntry selfEntry = new CBRP_NeighborTableEntry(id, LINK_BIDIRECTIONAL, nodeStatus);
        neighborTable.put(id, selfEntry);
    }

    public CBRP_Node(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
        // Add self to neighbor table
        CBRP_NeighborTableEntry selfEntry = new CBRP_NeighborTableEntry(id, LINK_BIDIRECTIONAL, nodeStatus);
        neighborTable.put(id, selfEntry);
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        // If this is the first time turning on, schedule undecided timer
        if (totalRunTime == -1 && nodeStatus == C_UNDECIDED) {
            scheduleUTimer();
        }

        while (System.currentTimeMillis() < startTime + runtTime) {
            // Process incoming messages
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            // Check timers
            checkTimers();

            // Send periodic HELLO messages
            if (totalRunTime - lastHelloSent >= Constants.NODE_CBRP_HELLO_INTERVAL) {
                sendHelloMessage();
                lastHelloSent = totalRunTime;
            }

            // Send random text message (for simulation purposes)
            if (totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay) {
                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                while (destination == id) {
                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                }
                sendMessage(new Message(id, destination, "Hello from " + id));
                lastMessageSent = totalRunTime;
            }

            // Process waiting messages if routes are available
            processWaitingMessages();

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
    public void discoverNeighbours() {
        // Override the base Node's neighbor discovery to use HELLO messages instead
        if (totalRunTime == -1 || totalRunTime - lastNeighbourDiscovery >= Constants.NODE_NEIGHBOUR_DISCOVERY_PERIOD) {
            sendHelloMessage();
            lastNeighbourDiscovery = totalRunTime;
            updatingNeighbours = true;
            log(2, "discovering neighbours");
        }

        if (totalRunTime - lastNeighbourDiscovery >= Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION && updatingNeighbours) {
            // Synchronize the base Node's neighbours set with our neighborTable
            neighbours.clear();
            for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
                if (entry.getKey() != id && entry.getValue().getLinkStatus() == LINK_BIDIRECTIONAL) {
                    neighbours.add(entry.getKey());
                }
            }

            // If we're undecided and have bidirectional neighbors, check if we should become a cluster head
            if (nodeStatus == C_UNDECIDED && !neighbours.isEmpty() && !uTimerActive) {
                scheduleUTimer();
            }

            updatedPaths = false;
            updatingNeighbours = false;

            log(2, "updated neighbors: " + neighbours);
        }
    }

    private void checkTimers() {
        long currentTime = totalRunTime;

        if (uTimerActive && currentTime >= uTimer) {
            uTimerActive = false;
            log(2, "undecided timer expired");

            // Force cluster head election for debugging
            if (nodeStatus == C_UNDECIDED) {
                // Elect self as cluster head
                nodeStatus = C_HEAD;
                log(2, "elected self as cluster head");
                sendHelloMessage(); // Triggered HELLO
            }
        }

        // Check contention timer
        if (cTimerActive && currentTime >= cTimer) {
            cTimerActive = false;
            log(2, "contention timer expired");

            // Check if still in contention with other cluster head
            boolean stillInContention = false;
            int contendingHeadId = -1;

            for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
                if (entry.getKey() != id && entry.getValue().getRole() == C_HEAD &&
                        entry.getValue().getLinkStatus() == LINK_BIDIRECTIONAL) {
                    stillInContention = true;
                    contendingHeadId = entry.getKey();
                    break;
                }
            }

            if (stillInContention) {
                // Compare IDs to decide who remains cluster head
                if (id > contendingHeadId) {
                    // Give up cluster head role
                    nodeStatus = C_MEMBER;
                    log(2, "giving up cluster head role to " + contendingHeadId);
                    sendHelloMessage(); // Triggered HELLO
                } else {
                    log(2, "won contention with " + contendingHeadId + ", remaining cluster head");
                }
            }
        }

        // Check for neighbor table entries timeout
        List<Integer> expiredNeighbors = new ArrayList<>();
        for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
            if (entry.getKey() != id && entry.getValue().isExpired(currentTime, (Constants.NODE_CBRP_HELLO_LOSS + 1) * Constants.NODE_CBRP_HELLO_INTERVAL)) {
                expiredNeighbors.add(entry.getKey());
            }
        }

        // Remove expired neighbors
        for (Integer neighborId : expiredNeighbors) {
            neighborTable.remove(neighborId);
            neighbours.remove(neighborId);
            log(2, "neighbor " + neighborId + " expired");
        }

        // If a member node lost all its cluster heads, check if it should become a cluster head
        if (nodeStatus == C_MEMBER) {
            boolean hasClusterHead = false;
            for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
                if (entry.getKey() != id && entry.getValue().getRole() == C_HEAD &&
                        entry.getValue().getLinkStatus() == LINK_BIDIRECTIONAL) {
                    hasClusterHead = true;
                    break;
                }
            }

            if (!hasClusterHead) {
                // Check if this node has the lowest ID among its bidirectional neighbors
                boolean hasLowerIdNeighbor = false;
                for (Integer neighborId : neighbours) {
                    if (neighborId < id) {
                        hasLowerIdNeighbor = true;
                        break;
                    }
                }

                if (!hasLowerIdNeighbor && !neighbours.isEmpty()) {
                    // Become cluster head
                    nodeStatus = C_HEAD;
                    log(2, "became cluster head after losing previous cluster head");
                    sendHelloMessage(); // Triggered HELLO
                } else {
                    // Go to undecided state
                    nodeStatus = C_UNDECIDED;
                    scheduleUTimer();
                    log(2, "lost cluster head, going to undecided state");
                }
            }
        }
    }

    private void scheduleUTimer() {
        uTimer = totalRunTime + Constants.NODE_CBRP_UNDECIDED_PD;
        uTimerActive = true;
        log(2, "scheduled undecided timer to expire at " + uTimer + " (current time: " + totalRunTime + ")");
    }

    private void scheduleCTimer() {
        cTimer = totalRunTime + Constants.NODE_CBRP_CONTENTION_PERIOD;
        cTimerActive = true;
        log(2, "scheduled contention timer to expire at " + cTimer);
    }

    private void sendHelloMessage() {
        // Create HELLO message with neighbor table information
        StringBuilder helloText = new StringBuilder();
        helloText.append("HELLO|").append(nodeStatus).append("|");

        // Add neighbor table entries
        for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
            if (entry.getKey() != id) { // Don't include self
                helloText.append(entry.getKey()).append(",")
                        .append(entry.getValue().getLinkStatus()).append(",")
                        .append(entry.getValue().getRole()).append(";");
            }
        }

        // Create and send the HELLO message
        CBRP_Message helloMessage = new CBRP_Message(id, -1, helloText.toString(), MessageType.CBRP_NEIGHBOUR_HELLO, true);

        // Send to all nodes within range (using the base Node's broadcast mechanism)
        super.sendMessage(helloMessage);

        log(1, "sent HELLO message with status " + nodeStatus + ", neighbors: " + neighbours);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.getMessageType() == MessageType.CBRP_NEIGHBOUR_HELLO) {
            handleHelloMessage((CBRP_Message) message);
        } else if (message instanceof CBRP_Message) {
            CBRP_Message cbrpMessage = (CBRP_Message) message;

            switch (message.getMessageType()) {
                case CBRP_RREQ:
                    handleRouteRequest(cbrpMessage);
                    break;
                case CBRP_RREP:
                    handleRouteReply(cbrpMessage);
                    break;
                case CBRP_RERR:
                    handleRouteError(cbrpMessage);
                    break;
                case CBRP_TEXT:
                    handleTextMessage(cbrpMessage);
                    break;
                default:
                    super.handleMessage(message);
            }
        } else {
            // Handle standard messages (like TEXT)
            if (message.getMessageType() == MessageType.TEXT) {
                // Convert to CBRP_TEXT and handle routing
                sendMessage(message);
            } else if (message.getMessageType() == MessageType.NEIGHBOUR_SYN) {
                // Handle base Node's NEIGHBOUR_SYN message
                // We'll respond but also update our neighbor table
                super.handleMessage(message);

                int sourceId = message.getSource();
                CBRP_NeighborTableEntry entry = neighborTable.getOrDefault(sourceId,
                        new CBRP_NeighborTableEntry(sourceId, LINK_FROM, C_UNDECIDED));
                entry.setLastHeard(totalRunTime);
                neighborTable.put(sourceId, entry);

                log(2, "received NEIGHBOUR_SYN from " + sourceId);
            } else if (message.getMessageType() == MessageType.NEIGHBOUR_ACK) {
                // Handle base Node's NEIGHBOUR_ACK message
                // Update our neighbor table to mark this as bidirectional
                super.handleMessage(message);

                int sourceId = message.getSource();
                CBRP_NeighborTableEntry entry = neighborTable.getOrDefault(sourceId,
                        new CBRP_NeighborTableEntry(sourceId, LINK_FROM, C_UNDECIDED));
                entry.setLinkStatus(LINK_BIDIRECTIONAL);
                entry.setLastHeard(totalRunTime);
                neighborTable.put(sourceId, entry);

                log(2, "received NEIGHBOUR_ACK from " + sourceId + ", marking as bidirectional");
            } else {
                super.handleMessage(message);
            }
        }
    }

    private void handleHelloMessage(CBRP_Message message) {
        String content = message.getText();
        String[] parts = content.split("\\|");

        if (parts.length < 2) {
            log(1, "received invalid HELLO message");
            return;
        }

        int senderStatus = Integer.parseInt(parts[1]);
        int senderId = message.getSource();

        log(2, "received HELLO from " + senderId + " with status " + senderStatus);

        // Update neighbor table with sender information
        CBRP_NeighborTableEntry entry = neighborTable.getOrDefault(senderId,
                new CBRP_NeighborTableEntry(senderId, LINK_FROM, senderStatus));
        entry.setRole(senderStatus);
        entry.setLastHeard(totalRunTime);
        neighborTable.put(senderId, entry);

        // Process neighbor table in HELLO message
        if (parts.length > 2 && !parts[2].isEmpty()) {
            String[] neighborEntries = parts[2].split(";");
            boolean iAmInSenderNeighborTable = false;

            for (String neighborEntry : neighborEntries) {
                if (neighborEntry.isEmpty()) continue;

                String[] entryParts = neighborEntry.split(",");
                if (entryParts.length >= 3) {
                    int neighborId = Integer.parseInt(entryParts[0]);
                    int linkStatus = Integer.parseInt(entryParts[1]);
                    int role = Integer.parseInt(entryParts[2]);

                    if (neighborId == id) {
                        iAmInSenderNeighborTable = true;
                        // Update link status with sender to bidirectional
                        entry.setLinkStatus(LINK_BIDIRECTIONAL);

                        // Add sender to base Node's neighbours set
                        if (!neighbours.contains(senderId)) {
                            neighbours.add(senderId);
                            log(2, "added " + senderId + " to neighbours set");
                        }
                    }

                    // Update two-hop topology information
                    if (linkStatus == LINK_BIDIRECTIONAL) {
                        if (!twoHopTopology.containsKey(senderId)) {
                            twoHopTopology.put(senderId, new ArrayList<>());
                        }
                        if (!twoHopTopology.get(senderId).contains(neighborId)) {
                            twoHopTopology.get(senderId).add(neighborId);
                        }
                    }
                }
            }

            // If I'm not in sender's neighbor table but sender is in mine as bidirectional
            if (!iAmInSenderNeighborTable && entry.getLinkStatus() == LINK_BIDIRECTIONAL) {
                entry.setLinkStatus(LINK_FROM);
                neighbours.remove(senderId);
                log(2, "removed " + senderId + " from neighbours set (unidirectional)");
            }
        }

        // Handle cluster formation logic
        if (senderStatus == C_HEAD && entry.getLinkStatus() == LINK_BIDIRECTIONAL) {
            if (nodeStatus == C_UNDECIDED) {
                // Undecided node becomes a member of this cluster
                nodeStatus = C_MEMBER;
                uTimerActive = false; // Cancel undecided timer
                log(2, "became member of cluster " + senderId);
                sendHelloMessage(); // Triggered HELLO
            } else if (nodeStatus == C_HEAD && !cTimerActive) {
                // Two cluster heads with bidirectional link - start contention
                scheduleCTimer();
                log(2, "started contention with cluster head " + senderId);
            }
        }

        // Process cluster adjacency information if present
        if (parts.length > 4 && parts[3].equals("CAT")) {
            String[] catEntries = parts[4].split(";");

            for (String catEntry : catEntries) {
                if (catEntry.isEmpty()) continue;

                String[] entryParts = catEntry.split(",");
                if (entryParts.length >= 2) {
                    int clusterHeadId = Integer.parseInt(entryParts[0]);
                    int linkStatus = Integer.parseInt(entryParts[1]);

                    // Only process if this is not our cluster and we're a cluster head
                    if (clusterHeadId != id && nodeStatus == C_HEAD) {
                        // Add to cluster adjacency table
                        if (!clusterAdjacencyTable.containsKey(clusterHeadId)) {
                            clusterAdjacencyTable.put(clusterHeadId, new ArrayList<>());
                        }

                        // Check if we already have this gateway
                        boolean gatewayExists = false;
                        for (CBRP_ClusterAdjacencyEntry existingEntry : clusterAdjacencyTable.get(clusterHeadId)) {
                            if (existingEntry.getGateway() == senderId) {
                                existingEntry.setLinkStatus(linkStatus);
                                existingEntry.setLastUpdated(totalRunTime);
                                gatewayExists = true;
                                break;
                            }
                        }

                        if (!gatewayExists) {
                            CBRP_ClusterAdjacencyEntry newEntry = new CBRP_ClusterAdjacencyEntry(
                                    senderId, linkStatus, totalRunTime);
                            clusterAdjacencyTable.get(clusterHeadId).add(newEntry);
                        }
                    }
                }
            }
        }
    }

    private void handleRouteRequest(CBRP_Message message) {
        // Extract RREQ information
        CBRP_RouteRequestInfo rreqInfo = message.getRouteRequestInfo();
        if (rreqInfo == null) {
            log(2, "received invalid RREQ");
            return;
        }

        // Check if we've seen this RREQ before
        Pair<Integer, Integer> rreqId = new Pair<>(rreqInfo.getOriginalSource(), rreqInfo.getBroadcastId());
        if (knownRREQs.contains(rreqId)) {
            log(2, "discarding duplicate RREQ " + rreqId.getKey() + ":" + rreqId.getValue());
            return;
        }

        // Mark this RREQ as seen
        knownRREQs.add(rreqId);

        // Check if we are the target
        if (rreqInfo.getTargetAddress() == id) {
            log(2, "I am the target for RREQ from " + rreqInfo.getOriginalSource());
            sendRouteReply(message);
            return;
        }

        // Check if target is our neighbor
        for (CBRP_NeighborTableEntry entry : neighborTable.values()) {
            if (entry.getNeighborId() == rreqInfo.getTargetAddress()) {
                log(1, "target is my neighbor, forwarding RREQ");
                // Forward RREQ directly to target
                CBRP_Message forwardedRreq = new CBRP_Message(message);
                forwardedRreq.setSource(id);
                forwardedRreq.setDestination(rreqInfo.getTargetAddress());
                forwardedRreq.setMulticast(false);
                messageRouter.sendMessage(forwardedRreq);
                return;
            }
        }

        // Check if we know the target is 2 hops away
        for (Map.Entry<Integer, List<Integer>> entry : twoHopTopology.entrySet()) {
            if (entry.getValue().contains(rreqInfo.getTargetAddress())) {
                log(2, "target is 2 hops away through " + entry.getKey());
                // Forward RREQ to the intermediate node
                CBRP_Message forwardedRreq = new CBRP_Message(message);
                forwardedRreq.setSource(id);
                forwardedRreq.setDestination(entry.getKey());
                forwardedRreq.setMulticast(false);
                messageRouter.sendMessage(forwardedRreq);
                return;
            }
        }

        // If we're a cluster head, process and forward the RREQ
        if (nodeStatus == C_HEAD) {
            // Add our cluster to the cluster address list
            rreqInfo.addClusterAddress(id);

            // Prepare list of neighboring cluster heads to forward to
            List<Integer> forwardToClusterHeads = new ArrayList<>();

            // Add all bidirectionally linked adjacent clusters that aren't already in the path
            for (Map.Entry<Integer, List<CBRP_ClusterAdjacencyEntry>> entry : clusterAdjacencyTable.entrySet()) {
                int adjacentClusterHead = entry.getKey();

                // Skip if already in cluster address list
                if (rreqInfo.getClusterAddresses().contains(adjacentClusterHead)) {
                    continue;
                }

                // Check if we have a bidirectional link to this cluster
                boolean hasBidirectionalLink = false;
                int gatewayNode = -1;

                for (CBRP_ClusterAdjacencyEntry catEntry : entry.getValue()) {
                    if (catEntry.getLinkStatus() == LINK_BIDIRECTIONAL) {
                        hasBidirectionalLink = true;
                        gatewayNode = catEntry.getGateway();
                        break;
                    }
                }

                if (hasBidirectionalLink) {
                    forwardToClusterHeads.add(adjacentClusterHead);
                    // Add to neighboring cluster head list in RREQ
                    rreqInfo.addNeighboringClusterHead(adjacentClusterHead, gatewayNode);
                }
            }

            // Update the RREQ with our changes
            message.setRouteRequestInfo(rreqInfo);

            // Forward RREQ to specified gateway nodes
            for (Pair<Integer, Integer> pair : rreqInfo.getNeighboringClusterHeadGatewayPairs()) {
                int clusterHead = pair.getKey();
                int gateway = pair.getValue();

                // Only forward if this entry was added by us
                if (forwardToClusterHeads.contains(clusterHead)) {
                    CBRP_Message forwardedRreq = new CBRP_Message(message);
                    forwardedRreq.setSource(id);
                    forwardedRreq.setDestination(gateway);
                    forwardedRreq.setMulticast(false);
                    messageRouter.sendMessage(forwardedRreq);
                    log(2, "forwarded RREQ to gateway " + gateway + " for cluster " + clusterHead);
                }
            }
        } else if (nodeStatus == C_MEMBER) {
            // If we're a member node, check if we're specified as a gateway
            boolean isGateway = false;
            int targetClusterHead = -1;

            for (Pair<Integer, Integer> pair : rreqInfo.getNeighboringClusterHeadGatewayPairs()) {
                if (pair.getValue() == id) {
                    isGateway = true;
                    targetClusterHead = pair.getKey();
                    break;
                }
            }

            if (isGateway) {
                // Forward to the specified cluster head
                CBRP_Message forwardedRreq = new CBRP_Message(message);
                forwardedRreq.setSource(id);
                forwardedRreq.setDestination(targetClusterHead);
                forwardedRreq.setMulticast(false);
                messageRouter.sendMessage(forwardedRreq);
                log(2, "forwarded RREQ as gateway to cluster head " + targetClusterHead);
            }
        }
    }

    private void handleRouteReply(CBRP_Message message) {
        // Extract RREP information
        CBRP_RouteReplyInfo rrepInfo = message.getRouteReplyInfo();
        if (rrepInfo == null) {
            log(2, "received invalid RREP");
            return;
        }
        
        // Check if we are the final destination for this RREP
        if (rrepInfo.getFinalDestination() == id) {
            log(2, "received RREP for route to " + message.getSource());
            
            // Extract the calculated route
            List<Integer> route = rrepInfo.getCalculatedRoute();
            
            // Store the route in the route cache
            String routeKey = message.getSource() + "";
            routeCache.put(routeKey, new ArrayList<>(route));
            
            // Process any waiting messages that can now be sent
            processWaitingMessages();
            
            return;
        }
        
        // If we're not the final destination, forward the RREP
        if (nodeStatus == C_HEAD) {
            // We're a cluster head, calculate next hop in the cluster path
            if (rrepInfo.getClusterAddresses().isEmpty()) {
                log(2, "RREP has empty cluster address list, cannot forward");
                return;
            }
            
            // Get the next cluster head in the path
            int nextClusterHead = rrepInfo.getClusterAddresses().get(0);
            rrepInfo.removeFirstClusterAddress();
            
            // Find a gateway to the next cluster head
            int gatewayNode = -1;
            if (clusterAdjacencyTable.containsKey(nextClusterHead)) {
                for (CBRP_ClusterAdjacencyEntry entry : clusterAdjacencyTable.get(nextClusterHead)) {
                    if (entry.getLinkStatus() == LINK_BIDIRECTIONAL) {
                        gatewayNode = entry.getGateway();
                        break;
                    }
                }
            }
            
            if (gatewayNode != -1) {
                // Add ourselves to the calculated route
                rrepInfo.addToCalculatedRoute(id);
                
                // Update the RREP
                message.setRouteReplyInfo(rrepInfo);
                
                // Forward to the gateway
                CBRP_Message forwardedRrep = new CBRP_Message(message);
                forwardedRrep.setSource(id);
                forwardedRrep.setDestination(gatewayNode);
                forwardedRrep.setMulticast(false);
                messageRouter.sendMessage(forwardedRrep);
                log(2, "forwarded RREP to gateway " + gatewayNode + " for cluster " + nextClusterHead);
            } else {
                log(2, "no gateway found to next cluster head " + nextClusterHead);
            }
        } else if (nodeStatus == C_MEMBER) {
            // We're a member node, check if next hop is our neighbor
            if (rrepInfo.getClusterAddresses().isEmpty()) {
                log(2, "RREP has empty cluster address list, cannot forward");
                return;
            }
            
            int nextClusterHead = rrepInfo.getClusterAddresses().get(0);
            
            // Add ourselves to the calculated route
            rrepInfo.addToCalculatedRoute(id);
            
            // Update the RREP
            message.setRouteReplyInfo(rrepInfo);
            
            // Forward directly if next cluster head is our neighbor
            if (neighborTable.containsKey(nextClusterHead)) {
                CBRP_Message forwardedRrep = new CBRP_Message(message);
                forwardedRrep.setSource(id);
                forwardedRrep.setDestination(nextClusterHead);
                forwardedRrep.setMulticast(false);
                messageRouter.sendMessage(forwardedRrep);
                log(2, "forwarded RREP directly to cluster head " + nextClusterHead);
            } else {
                log(2, "next cluster head " + nextClusterHead + " is not my neighbor");
            }
        }
    }

    private void handleRouteError(CBRP_Message message) {
        // Extract RERR information
        CBRP_RouteErrorInfo rerrInfo = message.getRouteErrorInfo();
        if (rerrInfo == null) {
            log(2, "received invalid RERR");
            return;
        }
        
        // Get the broken link information
        int fromAddress = rerrInfo.getFromAddress();
        int toAddress = rerrInfo.getToAddress();
        
        log(2, "received RERR for broken link " + fromAddress + " -> " + toAddress);
        
        // Remove routes from cache that use this broken link
        List<String> routesToRemove = new ArrayList<>();
        
        for (Map.Entry<String, List<Integer>> entry : routeCache.entrySet()) {
            List<Integer> route = entry.getValue();
            
            for (int i = 0; i < route.size() - 1; i++) {
                if (route.get(i) == fromAddress && route.get(i + 1) == toAddress) {
                    routesToRemove.add(entry.getKey());
                    break;
                }
            }
        }
        
        for (String key : routesToRemove) {
            routeCache.remove(key);
            log(2, "removed route to " + key + " from cache due to broken link");
        }
        
        // If we're not the final destination of the RERR, forward it
        if (rerrInfo.getSourceRoute().isEmpty()) {
            log(2, "RERR has empty source route, not forwarding");
            return;
        }
        
        int nextHop = rerrInfo.getSourceRoute().get(0);
        rerrInfo.removeFirstFromSourceRoute();
        
        if (nextHop != id) {
            // Update the RERR
            message.setRouteErrorInfo(rerrInfo);
            
            // Forward the RERR
            CBRP_Message forwardedRerr = new CBRP_Message(message);
            forwardedRerr.setSource(id);
            forwardedRerr.setDestination(nextHop);
            forwardedRerr.setMulticast(false);
            messageRouter.sendMessage(forwardedRerr);
            log(2, "forwarded RERR to " + nextHop);
        }
    }

    private void handleTextMessage(CBRP_Message message) {
        // Check if we are the final destination
        if (message.getFinalDestination() == id) {
            log(3, "received text message: " + message.getText());
            return;
        }
        
        // Forward the message along the source route
        List<Integer> sourceRoute = message.getSourceRoute();
        
        if (sourceRoute == null || sourceRoute.isEmpty()) {
            log(2, "text message has no source route, cannot forward");
            return;
        }
        
        int nextHop = sourceRoute.get(0);
        sourceRoute.remove(0);
        
        // Check if next hop is reachable
        if (neighborTable.containsKey(nextHop)) {
            // Update the message
            message.setSourceRoute(sourceRoute);
            
            // Forward the message
            CBRP_Message forwardedMessage = new CBRP_Message(message);
            forwardedMessage.setSource(id);
            forwardedMessage.setDestination(nextHop);
            forwardedMessage.setMulticast(false);
            messageRouter.sendMessage(forwardedMessage);
            log(2, "forwarded text message to " + nextHop);
        } else {
            // Next hop is unreachable, try local repair
            boolean repaired = tryLocalRepair(message);
            
            if (!repaired) {
                // Send route error
                sendRouteError(message);
            }
        }
    }

    @Override
    public void sendMessage(Message message) {
        if (message instanceof CBRP_Message) {
            CBRP_Message cbrpMessage = (CBRP_Message) message;
            cbrpMessage.setSource(id);
            
            if (cbrpMessage.getMessageType() == MessageType.CBRP_RREQ) {
                // Increment broadcast ID for new RREQs
                broadcastId++;
                sequenceNumber++;
                
                // Send to all neighbors
                messageRouter.sendMessage(cbrpMessage, neighbours);
                log(2, "sent RREQ to all neighbors");
            } else {
                // Send unicast message
                messageRouter.sendMessage(cbrpMessage);
            }
        } else {
            // Handle standard message types
            if (message.getMessageType() == MessageType.TEXT) {
                int destination = message.getDestination();
                
                // Check if we have a route to the destination
                String routeKey = destination + "";
                if (routeCache.containsKey(routeKey)) {
                    // Use cached route
                    List<Integer> route = new ArrayList<>(routeCache.get(routeKey));
                    
                    // Create CBRP text message with source route
                    CBRP_Message textMessage = new CBRP_Message(
                        id, route.get(0), message.getText(), MessageType.CBRP_TEXT, false);
                    textMessage.setFinalDestination(destination);
                    
                    // Remove first hop from route as it's the direct destination
                    route.remove(0);
                    textMessage.setSourceRoute(route);
                    
                    // Send the message
                    messageRouter.sendMessage(textMessage);
                    log(2, "sent text message to " + destination + " using cached route");
                } else {
                    // No route available, initiate route discovery
                    log(2, "no route to " + destination + ", initiating route discovery");
                    waitingMessages.add(message);
                    initiateRouteDiscovery(destination);
                }
            } else {
                // For other message types, use standard handling
                super.sendMessage(message);
            }
        }
    }

    private void initiateRouteDiscovery(int targetAddress) {
        // Create route request info
        CBRP_RouteRequestInfo rreqInfo = new CBRP_RouteRequestInfo(
            id, targetAddress, sequenceNumber, -1, broadcastId);
        
        // Add our host cluster heads to the neighboring cluster head list
        for (CBRP_NeighborTableEntry entry : neighborTable.values()) {
            if (entry.getRole() == C_HEAD && entry.getLinkStatus() == LINK_BIDIRECTIONAL) {
                rreqInfo.addNeighboringClusterHead(entry.getNeighborId(), entry.getNeighborId());
            }
        }
        
        // Add adjacent clusters from CAT
        for (Map.Entry<Integer, List<CBRP_ClusterAdjacencyEntry>> entry : clusterAdjacencyTable.entrySet()) {
            int clusterHeadId = entry.getKey();
            
            for (CBRP_ClusterAdjacencyEntry catEntry : entry.getValue()) {
                if (catEntry.getLinkStatus() == LINK_BIDIRECTIONAL) {
                    rreqInfo.addNeighboringClusterHead(clusterHeadId, catEntry.getGateway());
                    break;
                }
            }
        }
        
        // Create and send RREQ message
        CBRP_Message rreqMessage = new CBRP_Message(id, -1, "", MessageType.CBRP_RREQ, true);
        rreqMessage.setRouteRequestInfo(rreqInfo);
        
        // Add this RREQ to known RREQs
        knownRREQs.add(new Pair<>(id, broadcastId));
        
        // Send the RREQ
        sendMessage(rreqMessage);
    }

    private void sendRouteReply(CBRP_Message rreqMessage) {
        CBRP_RouteRequestInfo rreqInfo = rreqMessage.getRouteRequestInfo();
        
        // Create route reply info
        CBRP_RouteReplyInfo rrepInfo = new CBRP_RouteReplyInfo(
            rreqInfo.getIdentification(), false, rreqInfo.getOriginalSource());
        
        // Copy cluster addresses from RREQ
        rrepInfo.setClusterAddresses(new ArrayList<>(rreqInfo.getClusterAddresses()));
        
        // Add ourselves to the calculated route
        rrepInfo.addToCalculatedRoute(id);
        
        // Create RREP message
        CBRP_Message rrepMessage = new CBRP_Message(id, rreqMessage.getSource(), "", MessageType.CBRP_RREP, false);
        rrepMessage.setRouteReplyInfo(rrepInfo);
        
        // Send the RREP
        messageRouter.sendMessage(rrepMessage);
        log(2, "sent RREP to " + rreqMessage.getSource());
    }

    private void sendRouteError(CBRP_Message message) {
        // Get the source route from the message
        List<Integer> sourceRoute = message.getSourceRoute();
        if (sourceRoute == null || sourceRoute.isEmpty()) {
            log(2, "cannot send RERR, no source route in message");
            return;
        }
        
        int unreachableNode = sourceRoute.get(0);
        
        // Create reverse route to source
        List<Integer> reverseRoute = new ArrayList<>();
        reverseRoute.add(message.getOriginalSource());
        
        // Create route error info
        CBRP_RouteErrorInfo rerrInfo = new CBRP_RouteErrorInfo(reverseRoute, id, unreachableNode);
        
        // Create RERR message
        CBRP_Message rerrMessage = new CBRP_Message(id, message.getOriginalSource(), "", MessageType.CBRP_RERR, false);
        rerrMessage.setRouteErrorInfo(rerrInfo);
        
        // Send the RERR
        messageRouter.sendMessage(rerrMessage);
        log(2, "sent RERR for broken link " + id + " -> " + unreachableNode);
    }

    private boolean tryLocalRepair(CBRP_Message message) {
        List<Integer> sourceRoute = message.getSourceRoute();
        if (sourceRoute == null || sourceRoute.isEmpty()) {
            return false;
        }
        
        int unreachableNode = sourceRoute.get(0);
        
        // Try to find an alternative path to the unreachable node
        for (Map.Entry<Integer, List<Integer>> entry : twoHopTopology.entrySet()) {
            if (entry.getValue().contains(unreachableNode)) {
                // Found an alternative path through entry.getKey()
                int intermediateNode = entry.getKey();
                
                if (neighborTable.containsKey(intermediateNode)) {
                    // Update source route
                    List<Integer> newSourceRoute = new ArrayList<>();
                    newSourceRoute.add(intermediateNode);
                    newSourceRoute.add(unreachableNode);
                    newSourceRoute.addAll(sourceRoute.subList(1, sourceRoute.size()));
                    
                    // Set repaired flag
                    message.setRepaired(true);
                    
                    // Update the message
                    message.setSourceRoute(newSourceRoute);
                    
                    // Forward the message
                    CBRP_Message repairedMessage = new CBRP_Message(message);
                    repairedMessage.setSource(id);
                    repairedMessage.setDestination(intermediateNode);
                    repairedMessage.setMulticast(false);
                    messageRouter.sendMessage(repairedMessage);
                    log(2, "repaired route using intermediate node " + intermediateNode);
                    
                    return true;
                }
            }
        }
        
        // Try to find a path to the node after the unreachable node
        if (sourceRoute.size() > 1) {
            int nodeAfterUnreachable = sourceRoute.get(1);
            
            // Check if nodeAfterUnreachable is our direct neighbor
            if (neighborTable.containsKey(nodeAfterUnreachable)) {
                // Skip the unreachable node
                List<Integer> newSourceRoute = new ArrayList<>(sourceRoute.subList(1, sourceRoute.size()));
                
                // Set repaired flag
                message.setRepaired(true);
                
                // Update the message
                message.setSourceRoute(newSourceRoute);
                
                // Forward the message
                CBRP_Message repairedMessage = new CBRP_Message(message);
                repairedMessage.setSource(id);
                repairedMessage.setDestination(nodeAfterUnreachable);
                repairedMessage.setMulticast(false);
                messageRouter.sendMessage(repairedMessage);
                log(2, "repaired route by skipping unreachable node " + unreachableNode);
                
                return true;
            }
            
            // Check if nodeAfterUnreachable is reachable through a 2-hop neighbor
            for (Map.Entry<Integer, List<Integer>> entry : twoHopTopology.entrySet()) {
                if (entry.getValue().contains(nodeAfterUnreachable)) {
                    // Found an alternative path through entry.getKey()
                    int intermediateNode = entry.getKey();
                    
                    if (neighborTable.containsKey(intermediateNode)) {
                        // Update source route
                        List<Integer> newSourceRoute = new ArrayList<>();
                        newSourceRoute.add(intermediateNode);
                        newSourceRoute.add(nodeAfterUnreachable);
                        newSourceRoute.addAll(sourceRoute.subList(2, sourceRoute.size()));
                        
                        // Set repaired flag
                        message.setRepaired(true);
                        
                        // Update the message
                        message.setSourceRoute(newSourceRoute);
                        
                        // Forward the message
                        CBRP_Message repairedMessage = new CBRP_Message(message);
                        repairedMessage.setSource(id);
                        repairedMessage.setDestination(intermediateNode);
                        repairedMessage.setMulticast(false);
                        messageRouter.sendMessage(repairedMessage);
                        log(2, "repaired route by finding alternative path to node after unreachable");
                        
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private void processWaitingMessages() {
        List<Message> processedMessages = new ArrayList<>();
        
        for (Message message : waitingMessages) {
            String routeKey = message.getDestination() + "";
            
            if (routeCache.containsKey(routeKey)) {
                // We have a route, send the message
                List<Integer> route = new ArrayList<>(routeCache.get(routeKey));
                
                // Create CBRP text message with source route
                CBRP_Message textMessage = new CBRP_Message(
                    id, route.get(0), message.getText(), MessageType.CBRP_TEXT, false);
                textMessage.setFinalDestination(message.getDestination());
                
                // Remove first hop from route as it's the direct destination
                route.remove(0);
                textMessage.setSourceRoute(route);
                
                // Send the message
                messageRouter.sendMessage(textMessage);
                log(2, "sent waiting text message to " + message.getDestination());
                
                // Mark for removal
                processedMessages.add(message);
            }
        }
        
        // Remove processed messages
        waitingMessages.removeAll(processedMessages);
    }

    private List<Integer> getNeighborsWithBidirectionalLinks() {
        List<Integer> result = new ArrayList<>();
        
        for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
            if (entry.getKey() != id && entry.getValue().getLinkStatus() == LINK_BIDIRECTIONAL) {
                result.add(entry.getKey());
            }
        }
        
        return result;
    }

    public Map<Integer, CBRP_NeighborTableEntry> getNeighborTable() {
        return neighborTable;
    }

    public Map<Integer, List<CBRP_ClusterAdjacencyEntry>> getClusterAdjacencyTable() {
        return clusterAdjacencyTable;
    }

    public Map<String, List<Integer>> getRouteCache() {
        return routeCache;
    }

    public ArrayList<Message> getWaitingMessages() {
        return waitingMessages;
    }

    public int getNodeStatus() {
        return nodeStatus;
    }

    public List<Integer> getHostClusters() {
        List<Integer> hostClusters = new ArrayList<>();

        // Only member nodes belong to clusters
        if (nodeStatus == C_MEMBER) {
            for (Map.Entry<Integer, CBRP_NeighborTableEntry> entry : neighborTable.entrySet()) {
                CBRP_NeighborTableEntry neighbor = entry.getValue();

                // If the neighbor is a cluster head and we have a bidirectional link
                if (neighbor.getRole() == C_HEAD && neighbor.getLinkStatus() == LINK_BIDIRECTIONAL) {
                    hostClusters.add(neighbor.getNeighborId());
                }
            }
        }

        return hostClusters;
    }

    @Override
    public String toString() {
        return "CBRP_Node{\n" +
                "  nodeStatus=" + nodeStatus + ",\n" +
                "  sequenceNumber=" + sequenceNumber + ",\n" +
                "  broadcastId=" + broadcastId + ",\n" +
                "  knownRREQs=" + knownRREQs + ",\n" +
                "  lastHelloSent=" + lastHelloSent + ",\n" +
                "  uTimer=" + uTimer + ",\n" +
                "  cTimer=" + cTimer + ",\n" +
                "  uTimerActive=" + uTimerActive + ",\n" +
                "  cTimerActive=" + cTimerActive + ",\n" +
                "  neighborTable=" + neighborTable + ",\n" +
                "  clusterAdjacencyTable=" + clusterAdjacencyTable + ",\n" +
                "  twoHopTopology=" + twoHopTopology + ",\n" +
                "  routeCache=" + routeCache + ",\n" +
                "  waitingMessages=" + waitingMessages + ",\n" +
                "  x=" + x + ",\n" +
                "  y=" + y + ",\n" +
                "  speedX=" + speedX + ",\n" +
                "  speedY=" + speedY + ",\n" +
                "  communicationRadius=" + communicationRadius + ",\n" +
                "  id=" + id + ",\n" +
                "  neighbours=" + neighbours + ",\n" +
                "  messages=" + messages + ",\n" +
                "  messageRouter=" + messageRouter + ",\n" +
                "  totalRunTime=" + totalRunTime + ",\n" +
                "  random=" + random + ",\n" +
                "  messageDelay=" + messageDelay + ",\n" +
                "  lastMessageSent=" + lastMessageSent + ",\n" +
                "  lastNeighbourDiscovery=" + lastNeighbourDiscovery + ",\n" +
                "  active=" + active + ",\n" +
                "  newNeighbours=" + newNeighbours + ",\n" +
                "  updatingNeighbours=" + updatingNeighbours + ",\n" +
                "  updatedPaths=" + updatedPaths + "\n" +
                "} \n";
    }
}