package org.example.licentafromzero.Domain;

public enum MessageType {
    TEXT, NEIGHBOUR_SYN, NEIGHBOUR_ACK,
    DSR_RREQ, DSR_RREP, DSR_TEXT, DSR_RERR,
    AODV_RREQ, AODV_RREP, AODV_RERR, AODV_TEXT,
    SAODV_RREQ, SAODV_RREP, SAODV_RERR, SAODV_TEXT;
}
