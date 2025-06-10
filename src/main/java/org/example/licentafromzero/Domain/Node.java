package org.example.licentafromzero.Domain;

import java.util.*;
import java.util.stream.Collectors;

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
    protected long lastMessageSent;
    protected long lastNeighbourDiscovery;
    protected boolean active = true;
    protected HashSet<Integer> newNeighbours;
    protected boolean updatingNeighbours = false;
    protected boolean updatedPaths = true; //not for this one, but children have it
    protected ArrayList<Message> waitingMessages = new ArrayList<>(); //not for this one, but children have it

    protected Timer randomMessageTimer;
    private long pauseEndTime = 0; // Pause time for waypoint
    private double destX, destY; // For RANDOM_WAYPOINT

    public Node(int x, int y, int id){
        this.x = x;
        this.y = y;
        this.id = id;

        this.messageDelay = random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL;
        this.communicationRadius = random.nextInt(Constants.NODE_COMM_RANGE_BOUND) + Constants.NODE_COMM_RANGE_MIN_VAL;
        this.neighbours = new HashSet<>();
        this.newNeighbours = new HashSet<>();

        this.randomMessageTimer = new Timer(messageDelay);

        //initialize for mobility
        if(Constants.NODE_MOBILITY_TYPE == 1) {
            this.speedX = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
            this.speedY = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        }else if(Constants.NODE_MOBILITY_TYPE==2){
            destX = random.nextInt(Constants.SIMULATION_SIZE_X);
            destY = random.nextInt(Constants.SIMULATION_SIZE_Y);
        }
    }

    public Node(int x, int y, int id, int communicationRadius){
        this.x = x;
        this.y = y;
        this.id = id;

        this.messageDelay = random.nextInt(Constants.NODE_MESSAGE_DELAY_BOUND) + Constants.NODE_MESSAGE_DELAY_MIN_VAL;
        this.communicationRadius = communicationRadius;
        this.neighbours = new HashSet<>();
        this.newNeighbours = new HashSet<>();

        this.randomMessageTimer = new Timer(messageDelay);

        if(Constants.NODE_MOBILITY_TYPE == 1) {
            this.speedX = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
            this.speedY = random.nextInt(Constants.NODE_SPEED_BOUND) + Constants.NODE_SPEED_MIN_VAL;
        }else if(Constants.NODE_MOBILITY_TYPE==2){
            destX = random.nextInt(Constants.SIMULATION_SIZE_X);
            destY = random.nextInt(Constants.SIMULATION_SIZE_Y);
        }
    }

    public void turnOn(int runtTime) {         //runtTime is in millis

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            discoverNeighbours();

            if(randomMessageTimer.tick(totalRunTime)){
                List<Integer> neighbourList = new ArrayList<>(neighbours);
                if (!neighbourList.isEmpty()) {
                    Integer randomNeighbour = neighbourList.get(random.nextInt(neighbourList.size()));
                    sendMessage(new Message(id, randomNeighbour, "Hello from " + id)); //unicast random
                }
                lastMessageSent = totalRunTime;
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

    public void handleMessage(Message message){

        log(Constants.LOG_LEVEL_NODE, " received " + message.getMessageType() + " from: " + message.getSource());
        switch (message.getMessageType()){
            case TEXT:
                log(3, "received: " + message.getText());
                break;
            case NEIGHBOUR_SYN:
                sendMessage(new Message(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false));
                break;
            case NEIGHBOUR_ACK:
                newNeighbours.add(message.getSource());
                break;
        }


//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void discoverNeighbours(){

        if(totalRunTime == -1 || totalRunTime - lastNeighbourDiscovery >= Constants.NODE_NEIGHBOUR_DISCOVERY_PERIOD){
            Message message = new Message(id, -1, MessageType.NEIGHBOUR_SYN, true);
            sendMessage(message);
            lastNeighbourDiscovery = totalRunTime;
            updatingNeighbours = true;
            log(1,  " discovering neighbours");
        }

        if(totalRunTime - lastNeighbourDiscovery >= Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION && updatingNeighbours){
            neighbours = new HashSet<>(newNeighbours);
            neighbours.add(id);
            updatedPaths = false;
            newNeighbours.clear();
            updatingNeighbours = false;
        }
    }

    public void log(int level, String text){
        Util.log(level, id, text);
    }

    public void move(){

        switch (Constants.NODE_MOBILITY_TYPE){
            case 1: //randomDirection
                this.x = this.x + speedX;
                this.y = this.y + speedY;

                if(x <= 0 || x >= 900)
                    speedX *= -1;
                if(y <= 0 || y >= 900)
                    speedY *= -1;
                break;
            case 2: //randomWaypoint
                moveRandomWaypoint();
                break;
            default:
                break;
        }


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

    public void moveRandomWaypoint(){
        long currentTime = System.currentTimeMillis();

        // If reached destination or paused
        if ((x == destX && y == destY) || currentTime < pauseEndTime) {
            if (currentTime >= pauseEndTime) {
                // Choose new random destination
                destX = random.nextInt(Constants.SIMULATION_SIZE_X);
                destY = random.nextInt(Constants.SIMULATION_SIZE_Y);
                log(0, " chose new destination (" + destX + " " + destY + ")");
            }
            return;
        }

        // Movement vector
        double dx = destX - x;
        double dy = destY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 5) {
            log(0, " reached destination, resting");
            // Close enough to stop
            x = (int) destX;
            y = (int) destY;
            pauseEndTime = currentTime + Constants.NODE_MOBILITY_WAYPOINT_PAUSE;
            return;
        }

        // Normalize speed
        double stepSize = 2.0; // constant speed
        double stepX = (dx / distance) * stepSize;
        double stepY = (dy / distance) * stepSize;

        x += stepX;
        y += stepY;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;

        if(!active){
            this.neighbours.clear();
        }
    }

    public long getTotalRunTime() {
        return totalRunTime;
    }

    public ArrayList<Message> getWaitingMessages() {
        return waitingMessages;
    }

    public void setWaitingMessages(ArrayList<Message> waitingMessages) {
        this.waitingMessages = waitingMessages;
    }

    public double getDestX() {
        return destX;
    }

    public void setDestX(double destX) {
        this.destX = destX;
    }

    public double getDestY() {
        return destY;
    }

    public void setDestY(double destY) {
        this.destY = destY;
    }

    public String toInfo() {
        return "Node Info\n" +
                "----------\n" +
                "ID: " + id + "\n" +
                "Position: (" + x + ", " + y + ")\n" +
                "Speed: (" + speedX + ", " + speedY + ")\n" +
                "Destination: (" + (int) destX + ", " + (int) destY + ")\n" +
                "Communication Radius: " + communicationRadius + "\n" +
                "Active: " + active + "\n" +
                "Neighbours: " + neighbours.size() + "\n" +
                "Unprocessed messages: " + messages.size() + "\n" +
                "Total Runtime: " + totalRunTime + " ms\n" +
                "Message Delay: " + messageDelay + " ms\n" +
                "Waiting Messages: " + waitingMessages.size() + "\n";
    }
}
