package org.example.licentafromzero.Domain;

public class Timer {
    private long lastActivation, waitTime;

    public Timer(long waitTime) {
        this.waitTime = waitTime;
        this.lastActivation = -1;
    }

    public boolean tick(long currentTime){
        if(currentTime - Constants.SIMULATION_PAUSE_TIME - lastActivation > waitTime){
            lastActivation = currentTime;
            return true;
        }
        return false;
    }
}
