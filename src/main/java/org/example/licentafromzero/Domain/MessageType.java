package org.example.licentafromzero.Domain;

public enum MessageType {
    TEXT, NEIGHBOUR_SYN, NEIGHBOUR_ACK,
    DSR_TEXT, DSR_RREQ, DSR_RREP, DSR_RERR,
    AODV_TEXT, AODV_RREQ, AODV_RREP, AODV_RERR,
    SAODV_TEXT, SAODV_RREQ, SAODV_RREP, SAODV_RERR;
}
