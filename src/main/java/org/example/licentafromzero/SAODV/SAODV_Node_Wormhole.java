package org.example.licentafromzero.SAODV;

import org.example.licentafromzero.Domain.Constants;
import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

import java.security.KeyPair;

public class SAODV_Node_Wormhole extends SAODV_Node{
    public SAODV_Node_Wormhole(int x, int y, int id, KeyPair keyPair) {
        super(x, y, id, keyPair);
    }

    public SAODV_Node_Wormhole(int x, int y, int id, int communicationRadius, KeyPair keyPair) {
        super(x, y, id, communicationRadius, keyPair);
    }

    @Override
    public void turnOn(int runtTime) {
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() < startTime + runtTime) {
            while (!messages.isEmpty()) {
                handleMessage(messages.remove(0));
            }

            discoverNeighbours();

            if(totalRunTime > Constants.NODE_STARTUP_TIME && totalRunTime - lastMessageSent >= messageDelay){
                int destination = random.nextInt(Constants.SIMULATION_NR_NODES);
//                int destination = 9;
                while (destination == id)
                    destination = random.nextInt(Constants.SIMULATION_NR_NODES);
                sendMessage(new Message(id, destination, "Hello from " + id));
                lastMessageSent = totalRunTime;
            }

            if(totalRunTime > Constants.NODE_NEIGHBOUR_DISCOVERY_DURATION + lastNeighbourDiscovery && !updatedPaths){
                updateRoutes();
                updatedPaths = true;
            }



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

}
