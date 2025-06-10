package org.example.licentafromzero.OLSR;

import org.example.licentafromzero.Domain.Constants;

public class OLSR_Node_Overflow extends OLSR_Node{
    public OLSR_Node_Overflow(int x, int y, int id) {
        super(x, y, id);
    }

    public OLSR_Node_Overflow(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
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

//          Overflow attack
            if(id == 7 && routingTable.containsKey(5)){
                log(3,"Overflowing node 5, sending 100 random messages");
                OLSR_Message_TEXT overflowMessage = new OLSR_Message_TEXT(id, 5, "Denial Of Service", 5);
                for(int i=0;i<10;i++)
                    sendMessage(overflowMessage);
            }


            try {
                Thread.sleep(Constants.NODE_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            totalRunTime += System.currentTimeMillis() - startTime;
        }
    }
}
