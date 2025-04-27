package org.example.licentafromzero.Domain;

public final class Constants {
    private Constants(){};

    //General values
    public static final int SIMULATION_TIME = 30; //seconds
    public static final int SIMULATION_NR_NODES = 10;
    public static int SIMULATION_DELAY_BETWEEN_FRAMES = 10;
    public static final int SIMULATION_EXEC_TIME_NODE = 100; // NO modify
    public static final int SIMULATION_PROBABILITY_NODE_TURN_OFF = 0; // %%1000
    public static final int SIMULATION_PROBABILITY_NODE_TURN_ON = 0;
    public static final int SIMULATION_RSA_KEY_SIZE = 1024;
    public static final int DISPLAY_DETAILS = 2;
    public static int LOG_DETAILS = 1;
    /*
       0 = everything
       1 = only node messages
       2 = only relevant node receiving messages
       3 = only texts
     */



    //Node values
    public static final int NODE_DELAY = 10; // NO modify
    public static final int NODE_SPEED_BOUND = 2;
    public static final int NODE_SPEED_MIN_VAL = -NODE_SPEED_BOUND;
    public static final int NODE_MESSAGE_DELAY_BOUND = 1000;
    public static final int NODE_MESSAGE_DELAY_MIN_VAL = 1000;
    public static final int NODE_COMM_RANGE_BOUND = 500;
    public static final int NODE_COMM_RANGE_MIN_VAL = 300;
    public static final int NODE_STARTUP_TIME = 1500;
    public static final int NODE_NEIGHBOUR_DISCOVERY_PERIOD = 2000;
    public static final int NODE_NEIGHBOUR_DISCOVERY_DURATION = 700;

    public static final int NODE_AODV_STALE_ROUTE_PERIOD = 3000;
    public static final int NODE_SAODV_FORWARD_BUFFER_SIZE = 10;

    //Message values
    public static final int MESSAGE_NUMBER_FRAMES_SHOWN = 1;
}
