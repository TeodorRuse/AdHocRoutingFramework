package org.example.licentafromzero.OLSR;

import javafx.util.Pair;
import org.example.licentafromzero.Domain.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class OLSR_NeighbourTable_2hop {
    private int id;
    private HashMap<Integer, Long> nextHops = new HashMap<>(); // <NextHop_id, timeRecv>

    public OLSR_NeighbourTable_2hop(int id, int hop) {
        this.id = id;
        nextHops.put(hop, System.currentTimeMillis());
    }

    public void updateNextHops() {
        nextHops.entrySet().removeIf(entry -> entry.getValue() + Constants.OLSR_NEIGHBOR_EXPIRATION_TIME <=  System.currentTimeMillis());
    }


    public boolean isExpired(){
        return nextHops.isEmpty();
    }

    public void addNextHop(int dest){
        nextHops.put(dest,System.currentTimeMillis());
    }

    public void removeNextHop(int dest){
        nextHops.remove(dest);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HashMap<Integer, Long> getNextHops() {
        return nextHops;
    }


    public int getNextHopNewest() {
        return nextHops.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1); // return -1 if no next hop is available
    }

    public void setNextHops(HashMap<Integer, Long> nextHops) {
        this.nextHops = nextHops;
    }
}
