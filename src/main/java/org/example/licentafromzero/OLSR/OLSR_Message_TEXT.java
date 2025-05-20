package org.example.licentafromzero.OLSR;

import org.example.licentafromzero.Domain.Message;
import org.example.licentafromzero.Domain.MessageType;

public class OLSR_Message_TEXT extends Message {
    private int finalDestination;

    public OLSR_Message_TEXT(OLSR_Message_TEXT message) {
        super(message);
        this.finalDestination = message.getFinalDestination();
    }

    public OLSR_Message_TEXT(int source, int destination, String text, int finalDestination) {
        super(source, destination, text, MessageType.OLSR_TEXT, false);
        this.finalDestination = finalDestination;
    }

    @Override
    public Message copy() {
        return new OLSR_Message_TEXT(this);
    }

    public int getFinalDestination() {
        return finalDestination;
    }

    public void setFinalDestination(int finalDestination) {
        this.finalDestination = finalDestination;
    }
}
