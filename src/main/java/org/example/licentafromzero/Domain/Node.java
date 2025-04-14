package org.example.licentafromzero.Domain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.random;

public class Node {
    private int x, y;
    private int speedX = 0,speedY = 0;
    private int communicationRadius;
    private int id;
    private ArrayList<Node> neighbours;
    private List<Message> messages = new LinkedList<>();
    private MessageRouter messageRouter;
    private long totalRunTime = -1;
    private Random random = new Random();
    private int messageDelay;

    public Node(int x, int y, int id){
        this.x = x;
        this.y = y;
        this.id = id;

        this.speedX = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        this.speedY = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        this.messageDelay = random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL;
        this.communicationRadius = random.nextInt(Constants.NODE_COMM_RANGE_BOUND) + Constants.NODE_COMM_RANGE_MIN_VAL;
        this.neighbours = new ArrayList<>();
    }

    public void turnOn(int runtTime, int numNodes){         //runtTime is in millis

        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime){
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            if(totalRunTime >= messageDelay || totalRunTime == -1){
//                sendMessage(new Message(id, (int) System.currentTimeMillis()%numNodes, "Hello from " + id)); //unicast random
                sendMessage(new Message(id, -1, MessageType.TEXT, true));
                totalRunTime = 0;
            }

//            move();

//            //roundrobin
//            if(id == 0 && totalRunTime == 0){
//                sendMessage(new Message(id, 1, "Pass it forward"));
//            }

            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }

//    public void discoverNeighbours(){
//        Message message = new Message(id, -1, MessageType.NEIGHBOUR_DISCOVERY, true);
//    }

    public void handleMessage(Message message){
        //TODO: Temporary, just for testing
        System.out.println("Node " + id + " received message: " + message.getText());
//        if(id < 19)
//            message.setDestination(id+1);
//        else {
//            message.setDestination(0);
//        }
//        message.setSource(id);;
//
//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        sendMessage(message);
    }

    public void move(){
        this.x = this.x + speedX;
        this.y = this.y + speedY;

        if(x <= 0 || x >= 900)
            speedX *= -1;
        if(y <= 0 || y >= 900)
            speedY *= -1;

    }

    public MessageRouter getMessageRouter() {
        return messageRouter;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public void sendMessage(Message message){
        this.messageRouter.sendMessage(message);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSpeedX() {
        return speedX;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    public int getSpeedY() {
        return speedY;
    }

    public void setSpeedY(int speedY) {
        this.speedY = speedY;
    }

    public int getCommunicationRadius() {
        return communicationRadius;
    }

    public void setCommunicationRadius(int communicationRadius) {
        this.communicationRadius = communicationRadius;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message){
        this.messages.add(message);
    }

    public void removeMessage(Message message){
        this.messages.remove(message);
    }
}
