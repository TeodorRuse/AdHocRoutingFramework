package org.example.licentafromzero.Domain;

import javafx.application.Platform;

import java.util.ArrayList;
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

    public void setupRandom(int numberNodes){
        Random random = new Random();
        this.numberNodes = numberNodes;
        for(int i=0;i<numberNodes;i++){
            Node node = new Node(random.nextInt(sizeX), random.nextInt(sizeY), i);
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
            System.out.println("Neighbours: ");
            for(Node node: nodes){
                System.out.println("Node " + node.getId());
                System.out.println("\t" + node.getNeighbours());
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
