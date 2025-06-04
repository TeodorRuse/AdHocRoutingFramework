package org.example.licentafromzero.Domain;

public final class Constants {
    private Constants(){};

    //TODO: Fix message line hitbox
    //TODO: Handle when more messages have same path (so they don;t overlap visually: helps with blocking
    //TODO: create config files where this info is stored

    //General values
    public static final int SIMULATION_TIME = 30; //seconds
    public static final int SIMULATION_MODE = 6; // 1 = normal | 3 = DSR | 5 = AODV | 7 = SAODV | 9 = CBRP | 11 = OLSR | even = file
    public static final int SIMULATION_NR_NODES = 10;
    public static final int SIMULATION_SIZE_X = 900;
    public static final int SIMULATION_SIZE_Y = 900;
    public static int SIMULATION_DELAY_BETWEEN_FRAMES = 10;
    public static final int SIMULATION_EXEC_TIME_NODE = 100; // NO modify
    public static final int SIMULATION_PROBABILITY_NODE_TURN_OFF = 0; // %%1000
    public static final int SIMULATION_PROBABILITY_NODE_TURN_ON = 0;
    public static final int SIMULATION_RSA_KEY_SIZE = 1024;
    public static long SIMULATION_PAUSE_TIME = 0;

    public static final int LOG_LEVEL_ALL             = 0; // Log everything
    public static final int LOG_LEVEL_NODE            = 1; // Display all nodes actions/messages
    public static final int LOG_LEVEL_MESSAGE         = 2; // Only relevant messages
    public static final int LOG_LEVEL_TEXT            = 3; // Only text messages

    public static int LOG_LEVEL = 1;


    //Node values
    public static final int NODE_DELAY = 10; // NO modify
    public static final int NODE_SPEED_BOUND = 10;
    public static final int NODE_SPEED_MIN_VAL = -NODE_SPEED_BOUND;
    public static final int NODE_MOBILITY_TYPE = 0; // 0 = static | 1 = randomDirection | 2 = randomWaypoint
    public static final int NODE_MOBILITY_WAYPOINT_PAUSE = 2000; //ms to pause after reached waypoint

    public static final int NODE_MESSAGE_DELAY_BOUND = 1000;
    public static final int NODE_MESSAGE_DELAY_MIN_VAL = 1000;
    public static final int NODE_COMM_RANGE_BOUND = 500;
    public static final int NODE_COMM_RANGE_MIN_VAL = 300;
    public static final int NODE_STARTUP_TIME = 1500;
    public static final int NODE_NEIGHBOUR_DISCOVERY_PERIOD = 2000;
    public static final int NODE_NEIGHBOUR_DISCOVERY_DURATION = 700;

    //AODV / SAODV
    public static final int NODE_AODV_STALE_ROUTE_PERIOD = 3000;
    public static final int NODE_SAODV_FORWARD_BUFFER_SIZE = 10;

    //CBRP
    public static final long NODE_CBRP_HELLO_INTERVAL = 1500; // 2 seconds
    public static final int NODE_CBRP_HELLO_LOSS = 1;
    public static final long NODE_CBRP_CONTENTION_PERIOD = 1000; // 1.5 seconds
    public static final long NODE_CBRP_UNDECIDED_PD = 1000; // 1 second

    //OLSR
    public static final int OLSR_HELLO_INTERVAL = 1000;
    public static final int OLSR_TC_INTERVAL = 2500;
    public static final int OLSR_NEIGHBOR_EXPIRATION_TIME = OLSR_HELLO_INTERVAL*2;
    public static final int OLSR_RESEND_TIME = OLSR_TC_INTERVAL*2;


    //Message values
    public static final int MESSAGE_NUMBER_FRAMES_SHOWN = 1;
}
