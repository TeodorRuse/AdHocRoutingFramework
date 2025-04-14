package org.example.licentafromzero.Domain;

import java.util.ArrayList;

public class Message {
    private int source, destination;
    private String text;
    private Object payload;
    private boolean isSuccessful = false;
    private int numberFramesShown = Constants.MESSAGE_NUMBER_FRAMES_SHOWN;
    private MessageType messageType;
    private boolean isMulticast;

    public Message(int source, int destination, String text) {
        this.source = source;
        this.destination = destination;
        this.text = text;
        this.isMulticast = false;
        this.messageType = MessageType.TEXT;
    }

    public Message(int source, int destination, String text, MessageType messageType, boolean isMulticast) {
        this.source = source;
        this.destination = destination;
        this.text = text;
        this.messageType = messageType;
        this.isMulticast = isMulticast;
    }

    public Message(int source, int destination, MessageType messageType, boolean isMulticast) {
        this.source = source;
        this.destination = destination;
        this.text = "";
        this.messageType = messageType;
        this.isMulticast = isMulticast;
    }

    public Message(Message message){
        this.source = message.source;
        this.destination = message.destination;
        this.text = message.text;
        this.payload = message.payload;
        this.isSuccessful = message.isSuccessful;
        this.numberFramesShown = message.numberFramesShown;
        this.messageType = message.messageType;
        this.isMulticast = message.isMulticast;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public int getNumberFramesShown() {
        return numberFramesShown;
    }

    public void decreaseNumberFramesShown() {
        this.numberFramesShown--;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public boolean isMulticast() {
        return isMulticast;
    }

    public void setMulticast(boolean multicast) {
        isMulticast = multicast;
    }

    @Override
    public String toString() {
        return "Message{" +
                "source=" + source +
                ", destination=" + destination +
                ", text='" + text + '\'' +
                ", payload=" + payload +
                ", isSuccessful=" + isSuccessful +
                ", numberFramesShown=" + numberFramesShown +
                ", messageType=" + messageType +
                ", isMulticast=" + isMulticast +
                '}';
    }
}
