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

    public void setExtrafield(String extrafield) {
        this.extrafield = extrafield;
    }
}
