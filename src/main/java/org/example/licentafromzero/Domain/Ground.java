package org.example.licentafromzero.Domain;

import javafx.application.Platform;
import org.example.licentafromzero.AODV.AODV_Node;
import org.example.licentafromzero.AODV.AODV_RoutingTableEntry;
import org.example.licentafromzero.CBRP_Paper.CBRP_Message;
import org.example.licentafromzero.CBRP_Paper.CBRP_Node;
import org.example.licentafromzero.DSR.DSR_Node;
import org.example.licentafromzero.SAODV.SAODV_Message;
import org.example.licentafromzero.SAODV.SAODV_Node;
import org.example.licentafromzero.SAODV.SAODV_RoutingTableEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

public class Ground {
    private int sizeX, sizeY;
    private ArrayList<Node> nodes;
    private HashSet<Integer> offNodes;
    private MessageRouter messageRouter;
    private int numberNodes;
    private int focusedNodeIndex = -1;
    private Random random = new Random();

    public Ground(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        nodes = new ArrayList<>();
        offNodes = new HashSet<>();
        messageRouter = new MessageRouter();
    }

    public void setupRandom_Standard(int numberNodes){
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
            Node node = new Node(random.nextInt(sizeX), random.nextInt(sizeY), i);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }
    }

    public void setupFromFile_Standard(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            numberNodes = lines.size();

            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+"); // split by space(s)
                if (parts.length < 3) continue; // skip if not enough data

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int commRadius = Integer.parseInt(parts[2]);

                Node node = new Node(x, y, i, commRadius);
                node.setMessageRouter(messageRouter);
                messageRouter.addNode(node);
                nodes.add(node);
            }

        } catch (IOException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }

    public void setupFromFile_DSRNode(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            numberNodes = lines.size();

            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+"); // split by space(s)
                if (parts.length < 3) continue; // skip if not enough data

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int commRadius = Integer.parseInt(parts[2]);

                //Node node = new Node(x, y, i, commRadius);
                Node node = new DSR_Node(x, y, i, commRadius);
                node.setMessageRouter(messageRouter);
                messageRouter.addNode(node);
                nodes.add(node);
            }

        } catch (IOException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }
    public void setupRandom_DSRNode(int numberNodes){
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
//            Node node = new NodeExtra(random.nextInt(sizeX), random.nextInt(sizeY), i, "I am special " + i);
            Node node = new DSR_Node(random.nextInt(sizeX), random.nextInt(sizeY), i);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }
    }

    public void setupFromFile_AODVNode(String filePath){
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            numberNodes = lines.size();

            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+"); // split by space(s)
                if (parts.length < 3) continue; // skip if not enough data

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int commRadius = Integer.parseInt(parts[2]);

                //Node node = new Node(x, y, i, commRadius);
                Node node = new AODV_Node(x, y, i, commRadius);
                node.setMessageRouter(messageRouter);
                messageRouter.addNode(node);
                nodes.add(node);
            }

        } catch (IOException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }

    public void setupRandom_AODVNode(int numberNodes){
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
            Node node = new AODV_Node(random.nextInt(sizeX), random.nextInt(sizeY), i);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }
    }

    public void setupFromFile_SAODVNode(String filePath){
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            Map<Integer, PublicKey> keyChain = new HashMap<>();
            numberNodes = lines.size();

            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+"); // split by space(s)
                if (parts.length < 3) continue; // skip if not enough data

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int commRadius = Integer.parseInt(parts[2]);

                KeyPair keyPair = generateKeyPair();
                keyChain.put(i,keyPair.getPublic());

                //Node node = new Node(x, y, i, commRadius);
                Node node = new SAODV_Node(x, y, i, commRadius, keyPair);
                node.setMessageRouter(messageRouter);
                messageRouter.addNode(node);
                nodes.add(node);
            }

            for(Node node: nodes){
                if(node instanceof SAODV_Node saodvNode)
                    saodvNode.setKeyChain(keyChain);
            }

        } catch (IOException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }

    public void setupRandom_SAODVNode(int numberNodes){
        this.numberNodes = numberNodes;
        Map<Integer, PublicKey> keyChain = new HashMap<>();
        for(int i=0;i<numberNodes;i++){
            KeyPair keyPair = generateKeyPair();
            keyChain.put(i,keyPair.getPublic());

            Node node = new SAODV_Node(random.nextInt(sizeX), random.nextInt(sizeY), i, keyPair);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }

        for(Node node: nodes){
            if(node instanceof SAODV_Node saodvNode)
                saodvNode.setKeyChain(keyChain);
        }
    }

    public void setupRandom_CBRPNode(int numberNodes){
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
            Node node = new CBRP_Node(random.nextInt(sizeX), random.nextInt(sizeY), i);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }
    }

    public void setupFromFile_CBRPNode(String filePath){
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            Map<Integer, PublicKey> keyChain = new HashMap<>();
            numberNodes = lines.size();

            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+"); // split by space(s)
                if (parts.length < 3) continue; // skip if not enough data

                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int commRadius = Integer.parseInt(parts[2]);

                Node node = new CBRP_Node(x, y, i, commRadius);
                node.setMessageRouter(messageRouter);
                messageRouter.addNode(node);
                nodes.add(node);
            }

            for(Node node: nodes){
                if(node instanceof SAODV_Node saodvNode)
                    saodvNode.setKeyChain(keyChain);
            }

        } catch (IOException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }

    public void turnOnSimulationAsync(int simTimeInSeconds, Runnable uiCallback) {
//        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long simDuration = simTimeInSeconds * 1000;
            int chance;

            while (System.currentTimeMillis() < startTime + simDuration) {
                for(int i=0;i<numberNodes; i++){

                    chance = random.nextInt(1000);

                    if(nodes.get(i).isActive() && chance < Constants.SIMULATION_PROBABILITY_NODE_TURN_OFF){
                        if(Constants.LOG_DETAILS == 0)
                            System.out.println("Node " + nodes.get(i).getId() + " turning off");
                        deactivateNode(i);
                    }
                    else if(!nodes.get(i).isActive() && chance < Constants.SIMULATION_PROBABILITY_NODE_TURN_ON){
                        if(Constants.LOG_DETAILS == 0)
                            System.out.println("Node " + nodes.get(i).getId() + " turning on");
                        activateNode(i);
                    }

                    if(!offNodes.contains(i)) {
                        focusedNodeIndex = i;
                        nodes.get(i).turnOn(Constants.SIMULATION_EXEC_TIME_NODE);

                        try {
                            Thread.sleep(Constants.SIMULATION_DELAY_BETWEEN_FRAMES);
                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
                            System.err.println("Woke up");;
                        }
                        Platform.runLater(uiCallback);
                    }
                }
            }

            // Final update
            Platform.runLater(uiCallback);
            System.out.println("Simulation finished");
            System.out.println("Messages sent: " + messageRouter.getMessages().size());
            System.out.println("Text messages sent: " + messageRouter.getNumberTextsSent());
            System.out.println("Text message success rate: " + messageRouter.getProcentSuccessfulTexts() + "%");
            for(Node node: nodes){
                if(node instanceof DSR_Node dsrNode){
                    System.out.println(dsrNode.getId() + ": " + dsrNode.getKnownRoutes());
                }
                if(node instanceof AODV_Node aodvNode){
//                    System.out.println(aodvNode.getId() + ": " + aodvNode.getRoutingTable());
                    prettyPrintRoutingTable(aodvNode);
                    System.out.println("Undelivered text messages (" + aodvNode.getWaitingMessages().size() + ") :" + aodvNode.getWaitingMessages());
                    System.out.println("Undelivered control messages (" + aodvNode.getWaitingControlMessages().size() + ") :" + aodvNode.getWaitingControlMessages());
                }
                if(node instanceof SAODV_Node aodvNode){
//                    System.out.println(aodvNode.getId() + ": " + aodvNode.getRoutingTable());
                    prettyPrintRoutingTable(aodvNode);
                    System.out.println("Undelivered text messages (" + aodvNode.getWaitingMessages().size() + ") :" + aodvNode.getWaitingMessages());
                    System.out.println("Undelivered control messages (" + aodvNode.getWaitingControlMessages().size() + ") :" + aodvNode.getWaitingControlMessages());
                }
                if(node instanceof CBRP_Node cbrpNode){
//                    System.out.println(cbrpNode);
                }
            }
//        }).start();
    }

    public ArrayList<Node> getNodesFromIds(ArrayList<Integer> nodeIds){
        ArrayList<Node> list = new ArrayList<>();
        for(Node node: nodes){
            if(nodeIds.contains(node.getId()))
                list.add(node);
        }
        return list;
    }

    public Node getNodeFromId(Integer id){
        for(Node node: nodes){
            if(node.getId() == id)
                return node;
        }
        return null;
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = null;
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(Constants.SIMULATION_RSA_KEY_SIZE);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void prettyPrintRoutingTable(AODV_Node aodvNode) {
        System.out.println("\nRouting Table for Node " + aodvNode.getId());
        System.out.println("+------------+----------+---------------+----------+---------------------+");
        System.out.println("| Dest Addr  | Next Hop | Dest Seq Num  | Hop Count| Last Received Time  |");
        System.out.println("+------------+----------+---------------+----------+---------------------+");

        for (AODV_RoutingTableEntry entry : aodvNode.getRoutingTable().values()) {
            System.out.printf("| %-10d | %-8d | %-13d | %-8d | %-19s |%n",
                    entry.getDestAddr(),
                    entry.getNextHop(),
                    entry.getDestSeqNum(),
                    entry.getHopCount(),
                    entry.getReceivedTime());
        }

        System.out.println("+------------+----------+---------------+----------+---------------------+");
    }

    public static void prettyPrintRoutingTable(SAODV_Node aodvNode) {
        System.out.println("\nRouting Table for Node " + aodvNode.getId());
        System.out.println("+------------+----------+---------------+----------+---------------------+");
        System.out.println("| Dest Addr  | Next Hop | Dest Seq Num  | Hop Count| Last Received Time  |");
        System.out.println("+------------+----------+---------------+----------+---------------------+");

        for (SAODV_RoutingTableEntry entry : aodvNode.getRoutingTable().values()) {
            System.out.printf("| %-10d | %-8d | %-13d | %-8d | %-19s |%n",
                    entry.getDestAddr(),
                    entry.getNextHop(),
                    entry.getDestSeqNum(),
                    entry.getHopCount(),
                    entry.getReceivedTime());
        }

        System.out.println("+------------+----------+---------------+----------+---------------------+");
    }

    public int getSizeX() {
        return sizeX;
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    public MessageRouter getMessageRouter() {
        return messageRouter;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public int getFocusedNodeIndex() {
        return focusedNodeIndex;
    }

    public int getNumberNodes() {
        return numberNodes;
    }

    public HashSet<Integer> getOffNodes() {
        return offNodes;
    }

    public void deactivateNode(int id){
        this.offNodes.add(id);
        getNodeFromId(id).setActive(false);
    }

    public void activateNode(int id){
        this.offNodes.remove(id);
        getNodeFromId(id).setActive(true);
    }
}
