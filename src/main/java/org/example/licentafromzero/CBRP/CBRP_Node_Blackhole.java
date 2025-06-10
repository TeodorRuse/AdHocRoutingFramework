package org.example.licentafromzero.CBRP;

import org.example.licentafromzero.Domain.Message;

public class CBRP_Node_Blackhole extends CBRP_Node{

    public CBRP_Node_Blackhole(int x, int y, int id) {
        super(x, y, id);
    }

    public CBRP_Node_Blackhole(int x, int y, int id, int communicationRadius) {
        super(x, y, id, communicationRadius);
    }

    @Override
    public void sendMessage(Message message) {
        if (id != 3) {
            super.sendMessage(message);
        }
    }
}
