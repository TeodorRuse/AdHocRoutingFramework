package org.example.licentafromzero.Domain;

public class NodeExtra extends Node{
    private String extrafield;
    public NodeExtra(int x, int y, int id, int commRadius, String extrafield) {
        super(x, y, id, commRadius);
        this.extrafield = extrafield;
    }

    public NodeExtra(int x, int y, int id, String extrafield) {
        super(x, y, id);
        this.extrafield = extrafield;
    }

    public String getExtrafield() {
        return extrafield;
    }

    @Override
    public void turnOn(int runtTime){         //runtTime is in millis

        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime){
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            if(totalRunTime == -1){
                discoverNeighbours();
            }

//            move();

            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }

    @Override
    public void discoverNeighbours(){
        Message message = new MessageExtra(id, -1, MessageType.NEIGHBOUR_SYN, true, "Wow");
        sendMessage(message);
    }

    @Override
    public void handleMessage(Message message) {
        if(message instanceof MessageExtra messageExtra)
            System.out.println(messageExtra.getExtra());
        switch (message.getMessageType()){
            case TEXT:
                System.out.println("Node " + id + " received message TEXT: " + message.getText());
                break;
            case NEIGHBOUR_SYN:
                System.out.println("Node " + id + " received NEIGHBOUR_SYN from: " + message.getSource());
                sendMessage(new MessageExtra(id, message.getSource(), MessageType.NEIGHBOUR_ACK, false, "Wow"));
                break;
            case NEIGHBOUR_ACK:
                System.out.println("Node " + id + " received NEIGHBOUR_ACK from: " + message.getSource());
                neighbours.add(message.getSource());
                break;
        }
    }

    public void setExtrafield(String extrafield) {
        this.extrafield = extrafield;
    }
}
