package org.example.licentafromzero.Domain;

public final class Constants {
    private Constants(){};

    //General values
    public static final int SIMULATION_TIME = 10; //seconds
    public static final int SIMULATION_NR_NODES = 10;
    public static final int SIMULATION_DELAY_BETWEEN_FRAMES = 300;
    public static final int SIMULATION_EXEC_TIME_NODE = 100; // NO modify


    //Node values
    public static final int NODE_DELAY = 10; // NO modify
    public static final int NODE_SPEED_BOUND = 10;
    public static final int NODE_SPEED_MIN_VAL = -2;
    public static final int NODE_MESSAGE_DELAY_BOUND = 100;
    public static final int NODE_MESSAGE_DELAY_MIN_VAL = 1000;
    public static final int NODE_COMM_RANGE_BOUND = 500;
    public static final int NODE_COMM_RANGE_MIN_VAL = 100;

    //Message values
    public static final int MESSAGE_NUMBER_FRAMES_SHOWN = 1;
}
