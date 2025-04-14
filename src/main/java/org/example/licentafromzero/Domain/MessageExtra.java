package org.example.licentafromzero.Domain;

public class MessageExtra extends Message{
    private String extra;

    public MessageExtra(int source, int destination, MessageType messageType, boolean isMulticast, String extra) {
        super(source, destination, messageType, isMulticast);
        this.extra = extra;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
