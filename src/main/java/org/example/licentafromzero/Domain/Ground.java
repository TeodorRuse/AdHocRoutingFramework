package org.example.licentafromzero.Domain;

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ground {
    private int sizeX, sizeY;
    private ArrayList<Node> nodes;
    private MessageRouter messageRouter;
    private int numberNodes;
    private int focusedNodeIndex = -1;

    public Ground(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        nodes = new ArrayList<>();
        messageRouter = new MessageRouter();
    }

    public void setupStandardRandom(int numberNodes){
        Random random = new Random();
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
            Node node = new Node(random.nextInt(sizeX), random.nextInt(sizeY), i);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }
    }

    public void setupStandardFromFile(String filePath) {
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

    public void setupExtraNodeFromFile(String filePath) {
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
                Node node = new NodeExtra(x, y, i, commRadius, "I am special " + i);
                node.setMessageRouter(messageRouter);
                messageRouter.addNode(node);
                nodes.add(node);
            }

        } catch (IOException e) {
            e.printStackTrace(); // Or handle more gracefully
        }
    }


    public void setupRandomExtraNodes(int numberNodes){
        Random random = new Random();
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
            Node node = new NodeExtra(random.nextInt(sizeX), random.nextInt(sizeY), i, "I am special " + i);
            node.setMessageRouter(messageRouter);
            messageRouter.addNode(node);
            nodes.add(node);
        }
    }

    public void turnOnSimulationAsync(int simTimeInSeconds, Runnable uiCallback) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long simDuration = simTimeInSeconds * 1000;

            while (System.currentTimeMillis() < startTime + simDuration) {
                for(int i=0;i<numberNodes; i++){
                    focusedNodeIndex = i;
                    nodes.get(i).turnOn(Constants.SIMULATION_EXEC_TIME_NODE);

                    try {
                        Thread.sleep(Constants.SIMULATION_DELAY_BETWEEN_FRAMES);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Platform.runLater(uiCallback);
                }
            }

            // Final update
            Platform.runLater(uiCallback);
            System.out.println("Simulation finished");
            System.out.println("Messages sent: " + messageRouter.getMessages().size());
            for(Node node: nodes){
                if (node instanceof NodeExtra nodeExtra) {
                    System.out.println("Extra: " + nodeExtra.getExtrafield());
                }
            }
        }).start();
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
}
