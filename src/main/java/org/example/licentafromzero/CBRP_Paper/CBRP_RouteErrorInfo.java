package org.example.licentafromzero.CBRP_Paper;

import java.util.ArrayList;
import java.util.List;

public class CBRP_RouteErrorInfo {
    private List<Integer> sourceRoute; // Route to follow to reach the source
    private int fromAddress; // Node that detected the broken link
    private int toAddress; // Unreachable next hop
    
    public CBRP_RouteErrorInfo(List<Integer> sourceRoute, int fromAddress, int toAddress) {
        this.sourceRoute = new ArrayList<>(sourceRoute);
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
    }
    
    // Copy constructor
    public CBRP_RouteErrorInfo(CBRP_RouteErrorInfo other) {
        if (other.sourceRoute != null) {
            this.sourceRoute = new ArrayList<>(other.sourceRoute);
        } else {
            this.sourceRoute = new ArrayList<>();
        }
        this.fromAddress = other.fromAddress;
        this.toAddress = other.toAddress;
    }
    
    public void removeFirstFromSourceRoute() {
        if (!sourceRoute.isEmpty()) {
            sourceRoute.remove(0);
        }
    }
    
    public List<Integer> getSourceRoute() {
        return sourceRoute;
    }
    
    public int getFromAddress() {
        return fromAddress;
    }
    
    public int getToAddress() {
        return toAddress;
    }
    
    @Override
    public String toString() {
        return "CBRP_RouteErrorInfo{" +
                "sourceRoute=" + sourceRoute +
                ", fromAddress=" + fromAddress +
                ", toAddress=" + toAddress +
                '}';
    }
}