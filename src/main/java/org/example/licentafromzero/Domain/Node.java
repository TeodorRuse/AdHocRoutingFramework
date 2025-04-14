package org.example.licentafromzero.Domain;

import java.util.*;

public class Node {
    protected int x, y;
    protected int speedX = 0,speedY = 0;
    protected int communicationRadius;
    protected int id;
    protected HashSet<Integer> neighbours;
    protected List<Message> messages = new LinkedList<>();
    protected MessageRouter messageRouter;
    protected long totalRunTime = -1;
    protected Random random = new Random();
    protected int messageDelay;

    public Node(int x, int y, int id){
        this.x = x;
        this.y = y;
        this.id = id;

        this.speedX = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        this.speedY = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        this.messageDelay = random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL;
        this.communicationRadius = random.nextInt(Constants.NODE_COMM_RANGE_BOUND) + Constants.NODE_COMM_RANGE_MIN_VAL;
        this.neighbours = new HashSet<>();
    }

    public Node(int x, int y, int id, int communicationRadius){
        this.x = x;
        this.y = y;
        this.id = id;

        this.speedX = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        this.speedY = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        this.messageDelay = random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL;
        this.communicationRadius = communicationRadius;
        this.neighbours = new HashSet<>();
    }

    public void turnOn(int runtTime){         //runtTime is in millis

        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime){
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            if(totalRunTime == -1){
                discoverNeighbours();
            }


            if(totalRunTime >= messageDelay){
//                sendMessage(new Message(id, (int) System.currentTimeMillis()%numNodes, "Hello from " + id)); //unicast random
                sendMessage(new Message(id, -1, "Random Hello!" , MessageType.TEXT, true));
                totalRunTime = 0;

            }

//            //roundrobin
//            if(id == 0 && totalRunTime == 0){
//                sendMessage(new Message(id, 1, "Pass it forward"));
//            }

//            move();

            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }

    public void discoverNeighbours(){
        Message message = new Message(id, -1, MessageType.NEIGHBOUR_SYN, true);
        sendMessage(message);
    }

    public void handleMessage(Message message){

        switch (message.getMessageType()){
            case TEXT:
                System.out.println("Node " + id + " received message TEXT: " + message.getText());
                break;
            case NEIGHBOUR_SYN:
                System.out.println("Node " + id + " received NEIGHBOUR_SYN from: " + message.getSource());
                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
                break;
            case NEIGHBOUR_ACK:
                System.out.println("Node " + id + " received NEIGHBOUR_ACK from: " + message.getSource());
                neighbours.add(message.getSource());
                break;
        }


//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
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

    public HashSet<Integer> getNeighbours() {
        return neighbours;
    }
}
