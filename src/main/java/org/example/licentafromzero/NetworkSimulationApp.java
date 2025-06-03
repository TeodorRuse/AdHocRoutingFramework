package org.example.licentafromzero;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.example.licentafromzero.CBRP_Paper.CBRP_Node;
import org.example.licentafromzero.Domain.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkSimulationApp extends Application {

    // UI Components
    private HBox mainLayout;
    private VBox leftPanel;
    private Pane simulationCanvas;
    private VBox rightPanel;
    private HBox bottomPanel;
    private HBox topPanel;
    private ArrayList<Button> speedButtons = new ArrayList<>();

    // Simulation components
    private Ground ground;
    private Thread groundThread;
    private boolean stopSimulation = false;
    private int focusedNodeIndex = -1;
    private long startTime, simDuration;

    // Controls
    private Button playPauseButton;
    private ProgressBar timeProgressBar;
    private Label timeLabel;
    private Label protocolLabel;

    // Log components
    private TextArea logTextArea;
    private ComboBox<String> logFilterComboBox;
    private Button clearLogButton;
    private ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private LogReader logReader;
    private Thread logReaderThread;

    // Add these fields to your class
    private Message selectedMessage = null;
    private boolean isMessageSelected = false;

    // Node info panel
    private VBox nodeInfoPanel;
    private Label nodeInfoTitle;
    private TextArea nodeInfoText;

    // Animation and visualization
    private Map<Integer, Double> nodeAnimationPhases = new HashMap<>();
    private long lastUpdateTime = 0;
    private long pauseTime = 0;

    // Constants for visualization
    private static final double NODE_SIZE = 16;
    private static final double CLUSTER_HEAD_INDICATOR_SIZE = 22;
    private static final double CLUSTER_PADDING = 25;
    private static final int MAX_LOG_ENTRIES = 1000;
    private static int lastSimSpeed;

    // Color schemes
    private final Color[] NODE_COLORS = {
            Color.rgb(255, 140, 0),    // Dark Orange
            Color.rgb(50, 205, 50),    // Lime Green
            Color.rgb(30, 144, 255),   // Dodger Blue
            Color.rgb(255, 105, 180),  // Hot Pink
            Color.rgb(255, 215, 0)     // Gold
    };

    private final Color[] CLUSTER_COLORS = {
            Color.rgb(255, 200, 200, 0.35),  // Light red
            Color.rgb(200, 255, 200, 0.35),  // Light green
            Color.rgb(200, 200, 255, 0.35),  // Light blue
            Color.rgb(255, 255, 200, 0.35),  // Light yellow
            Color.rgb(255, 200, 255, 0.35),  // Light purple
            Color.rgb(200, 255, 255, 0.35),  // Light cyan
            Color.rgb(255, 220, 180, 0.35),  // Light orange
            Color.rgb(220, 180, 255, 0.35),  // Light lavender
            Color.rgb(180, 255, 220, 0.35),  // Light mint
            Color.rgb(255, 180, 180, 0.35)   // Light coral
    };

    private final Color[] MESSAGE_COLORS = {
            Color.rgb(50, 205, 50),    // Success - Lime Green
            Color.rgb(220, 20, 60),    // Error - Crimson
            Color.rgb(255, 165, 0),    // Warning - Orange
            Color.rgb(65, 105, 225)    // Info - Royal Blue
    };

    @Override
    public void start(Stage primaryStage) {

        try (FileWriter fw = new FileWriter("log.txt", false)) {
            fw.write(""); // Overwrites the file with nothing
        } catch (IOException e) {
            System.err.println("Failed to clear log.txt: " + e.getMessage());
        }

        primaryStage.setTitle("FEROX: Ad Hoc Network Simulator");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/example/licentafromzero/FEROX-logo-shield.png")));


        // Initialize the ground simulation
        ground = new Ground(Constants.SIMULATION_SIZE_X, Constants.SIMULATION_SIZE_Y);

        // Initialize all panels
        createTopPanel();
        createLeftPanel();
        createSimulationCanvas();
        createRightPanel();
        createBottomPanel();

        // Create the main layout
        createMainLayout();

        // Initialize simulation
        initializeSimulation();

        // Start log reader
        startLogReader();

        // Create and show scene
        Scene scene = new Scene(mainLayout);
        primaryStage.setScene(scene);

        primaryStage.setMaximized(true);

        primaryStage.show();

        // Start the simulation
        startSimulation();
    }

    private void createMainLayout() {
        mainLayout = new HBox();
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Wrap left
        VBox leftWrapper = new VBox();
        leftWrapper.getChildren().add(leftPanel);
        VBox.setVgrow(leftPanel, Priority.ALWAYS);

        // Wrap right
        VBox rightWrapper = new VBox();
        rightWrapper.getChildren().add(rightPanel);
        VBox.setVgrow(rightPanel, Priority.ALWAYS);

        // Center structure
        VBox centerWrapper = new VBox();
        centerWrapper.getChildren().addAll(topPanel, simulationCanvas, bottomPanel);
        VBox.setVgrow(simulationCanvas, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftWrapper, centerWrapper, rightWrapper);
        HBox.setHgrow(centerWrapper, Priority.ALWAYS);
    }

    private void createTopPanel() {
        topPanel = new HBox();
        topPanel.setPrefHeight(180);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(10, 20, 10, 20));
        topPanel.setSpacing(20);
        topPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // === Logo ===
        Image logoImage = new Image(getClass().getResourceAsStream("/org/example/licentafromzero/FEROX-logo-shield.png"));
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitHeight(160);
        logoView.setPreserveRatio(true);

        // === VBox: Title + Protocol + Speed Controls ===
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Ad-Hoc Network Simulator");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.rgb(44, 62, 80));

        protocolLabel = new Label("Protocol: " + getProtocolName());
        protocolLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        protocolLabel.setTextFill(Color.rgb(0,0,0));

        // Control buttons
        playPauseButton = createControlButton("⏸", 14, 10000000);
        playPauseButton.setOnAction(e -> togglePlayPause());

        speedButtons.add(createControlButton("▶", 16, 1500));     // fast)
        speedButtons.add(createControlButton("▶▶", 16, 800));  // faster
        speedButtons.add(createControlButton("▶▶▶", 14, 300));  // faster
        speedButtons.add(createControlButton("▶▶▶▶", 12, 10)); // fastest
        updateSpeedButtonStyles();

        HBox speedBox = new HBox(10, playPauseButton, speedButtons.get(0), speedButtons.get(1), speedButtons.get(2), speedButtons.get(3));
        speedBox.setAlignment(Pos.BOTTOM_LEFT);

        infoBox.getChildren().addAll(titleLabel, protocolLabel, speedBox);

        // === Progress bar + time ===
        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.BOTTOM_CENTER);
        progressBox.setPrefWidth(624);

        timeProgressBar = new ProgressBar(0);
        timeProgressBar.setPrefWidth(800);
        timeProgressBar.setPrefHeight(20);
        timeProgressBar.setStyle("-fx-accent: #3498db;");

        timeLabel = new Label("00:00 / " + String.format("%02d:%02d",
                Constants.SIMULATION_TIME / 60, Constants.SIMULATION_TIME % 60));
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timeLabel.setTextFill(Color.rgb(44, 62, 80));

        Pane emptySpace = new Pane();
        emptySpace.setPrefHeight(20);

        progressBox.getChildren().addAll(timeProgressBar, timeLabel, emptySpace);

        // === Exit Button ===
        VBox finalButtonsVBox = new VBox();
        finalButtonsVBox.setAlignment(Pos.BOTTOM_CENTER);
        Pane emptySpace2 = new Pane();
        emptySpace2.setPrefHeight(40);

        Button exitButton = createStyledButton("Exit", "#e74c3c");
        exitButton.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        finalButtonsVBox.getChildren().addAll(exitButton, emptySpace2);

        // === Spacer ===
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // === Final assembly ===
        topPanel.getChildren().addAll(logoView, infoBox, progressBox, spacer, finalButtonsVBox);
    }

    private void setSimDelay(int delay){
        Constants.SIMULATION_DELAY_BETWEEN_FRAMES = delay;
    }

    private void togglePlayPause() {
        stopSimulation = !stopSimulation;

        if (stopSimulation) {
            pauseTime = System.currentTimeMillis();
            playPauseButton.setStyle("-fx-background-color: rgb(8,100,187);" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 0;");

            for (Button btn : speedButtons) {
                btn.setDisable(true);
                btn.setStyle("-fx-background-color: #444444;" +
                        "-fx-text-fill: #aaaaaa;" +
                        "-fx-background-radius: 0;");
            }

            lastSimSpeed = Constants.SIMULATION_DELAY_BETWEEN_FRAMES;
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = 10000000;
            appendToLog("Simulation paused");
        } else {

            playPauseButton.setStyle("-fx-background-color: rgb(0,51,102);" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 0;");
            for (Button btn : speedButtons) {
                btn.setDisable(false);
            }
            updateSpeedButtonStyles();

            if (groundThread != null) {
                groundThread.interrupt();
            }
            Constants.SIMULATION_PAUSE_TIME += System.currentTimeMillis() - pauseTime;
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = lastSimSpeed;
            appendToLog("Simulation resumed");
        }
    }


    private Button createControlButton(String text, int textSize, int delay) {
        Button btn = new Button(text);
        btn.setPrefSize(40, 40);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, textSize));
        btn.setUserData(delay);

        btn.setStyle("-fx-background-color: rgb(0,51,102);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 0;");

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9;"));
        btn.setOnMouseClicked(e -> updateSpeedButtonStyles());


        if(!text.equals("⏸")) {
            btn.setOnAction(e -> setSimDelay(delay));
        }

        return btn;
    }

    private void updateSpeedButtonStyles() {
        for (Button btn : speedButtons) {
            int buttonDelay = (int) btn.getUserData();

            if (Constants.SIMULATION_DELAY_BETWEEN_FRAMES == buttonDelay) {
                // Selected style

                btn.setStyle("-fx-background-color: rgb(8,100,187);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0;");
            } else {
                // Normal style
                btn.setStyle("-fx-background-color: rgb(0,51,102);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0;");
            }
        }
    }



    private void createLeftPanel() {
        leftPanel = new VBox();
        leftPanel.setPrefWidth(450);
        leftPanel.setSpacing(10);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 0 0;");

        // Logs section
        Label logsTitle = new Label("Simulation Logs");
        logsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logsTitle.setTextFill(Color.rgb(44, 62, 80));

        // Filter controls
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Filter:");
        filterLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        logFilterComboBox = new ComboBox<>();
        logFilterComboBox.getItems().addAll("All Logs", "Node Status", "Messages", "Text");
        setLogFilterComboBoxValue();
        logFilterComboBox.setPrefWidth(120);
        logFilterComboBox.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 4;");

        clearLogButton = createStyledButton("Clear", "#95a5a6");
        clearLogButton.setPrefWidth(80);
        clearLogButton.setOnAction(e -> logTextArea.clear());

        filterBox.getChildren().addAll(filterLabel, logFilterComboBox, clearLogButton);

        // Log text area
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setPrefHeight(600);
        logTextArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: 12px; " +
                "-fx-background-color: #2c3e50; " +
                "-fx-text-fill: black; " +
                "-fx-border-color: #34495e; " +
                "-fx-border-radius: 4;");

        ScrollPane logScrollPane = new ScrollPane(logTextArea);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setFitToHeight(true);
        VBox.setVgrow(logScrollPane, Priority.ALWAYS);

        // Set up filter listener
        logFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            Constants.LOG_LEVEL = getFilterKeyword(newVal);
        });

        leftPanel.getChildren().addAll(logsTitle, filterBox, logScrollPane);
    }

    private void createSimulationCanvas() {
        simulationCanvas = new Pane();
        simulationCanvas.setPrefSize(700, 700);
        simulationCanvas.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 8;");

        // Clip drawing to the bounds of the simulationCanvas
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(simulationCanvas.widthProperty());
        clip.heightProperty().bind(simulationCanvas.heightProperty());
        simulationCanvas.setClip(clip);

        // Add click handler for canvas
        simulationCanvas.setOnMouseClicked(this::handleCanvasClick);
    }

    private void createRightPanel() {
        rightPanel = new VBox();
        rightPanel.setPrefWidth(250);
        rightPanel.setSpacing(15);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 0 1;");

        // Node info section
        nodeInfoTitle = new Label("Node Information");
        nodeInfoTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nodeInfoTitle.setTextFill(Color.rgb(44, 62, 80));

        nodeInfoText = new TextArea();
        nodeInfoText.setEditable(false);
        nodeInfoText.setWrapText(true);
        nodeInfoText.setPrefHeight(600);
        nodeInfoText.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: 11px; " +
                "-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #dee2e6; " +
                "-fx-border-radius: 4;");

        // Simulation stats
        Label statsTitle = new Label("Simulation Statistics");
        statsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statsTitle.setTextFill(Color.rgb(44, 62, 80));

        VBox statsBox = new VBox(8);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 4;");
        statsBox.setPrefHeight(400);

        TextFlow statsFlow = getSimulationStatsText();
        statsFlow.setPrefWidth(280); // match your layout width

        ScrollPane scrollPane = new ScrollPane(statsFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc;");
        scrollPane.setPrefViewportHeight(300); // height control

        statsBox.getChildren().add(scrollPane);

        rightPanel.getChildren().addAll(
                nodeInfoTitle,
                nodeInfoText,
                new Separator(),
                statsTitle,
                statsBox
        );
    }

//    private String getSimulationStatsText() {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append("Protocol: ").append(getProtocolName()).append("\n");
//        sb.append("Nodes: ").append(Constants.SIMULATION_NR_NODES).append("\n");
//        sb.append("Duration: ").append(Constants.SIMULATION_TIME).append(" s\n");
//        sb.append("Size: ").append(Constants.SIMULATION_SIZE_X).append(" x ")
//                .append(Constants.SIMULATION_SIZE_Y).append("\n");
//        sb.append("Frame Delay: ").append(Constants.SIMULATION_DELAY_BETWEEN_FRAMES).append(" ms\n");
//        sb.append("Node Turn-Off %: ").append(Constants.SIMULATION_PROBABILITY_NODE_TURN_OFF / 10.0).append("%\n");
//        sb.append("Node Turn-On %: ").append(Constants.SIMULATION_PROBABILITY_NODE_TURN_ON / 10.0).append("%\n");
//        sb.append("Mobility: ").append(getMobilityName(Constants.NODE_MOBILITY_TYPE)).append("\n");
//        sb.append("Comm Range: ").append(Constants.NODE_COMM_RANGE_MIN_VAL)
//                .append("–").append(Constants.NODE_COMM_RANGE_BOUND).append("\n");
//        sb.append("Node Speed: ").append(Constants.NODE_SPEED_MIN_VAL)
//                .append("–").append(Constants.NODE_SPEED_BOUND).append("\n");
//        sb.append("Pause Time: ").append(Constants.SIMULATION_PAUSE_TIME).append(" ms\n");
//        sb.append("Log Level: ").append(Constants.LOG_LEVEL).append("\n");
//
//
//        // Protocol-specific additions
//        if (getProtocolName().equals("AODV") || getProtocolName().equals("SAODV") ) { // AODV/SAODV
//            sb.append("Stale Route Timeout: ").append(Constants.NODE_AODV_STALE_ROUTE_PERIOD).append(" ms\n");
//            if (getProtocolName().equals("SAODV")) {
//                sb.append("SAODV Key Size: ").append(Constants.SIMULATION_RSA_KEY_SIZE).append(" bits\n");
//                sb.append("SAODV Fwd Buffer Size: ").append(Constants.NODE_SAODV_FORWARD_BUFFER_SIZE).append("\n");
//            }
//        } else if (getProtocolName().equals("CBRP")) { // CBRP
//            sb.append("Hello Interval: ").append(Constants.NODE_CBRP_HELLO_INTERVAL).append(" ms\n");
//            sb.append("Contention Period: ").append(Constants.NODE_CBRP_CONTENTION_PERIOD).append(" ms\n");
//            sb.append("Undecided Period: ").append(Constants.NODE_CBRP_UNDECIDED_PD).append(" ms\n");
//        } else if (getProtocolName().equals("OLSR")) { // OLSR
//            sb.append("Hello Interval: ").append(Constants.OLSR_HELLO_INTERVAL).append(" ms\n");
//            sb.append("TC Interval: ").append(Constants.OLSR_TC_INTERVAL).append(" ms\n");
//            sb.append("Neighbor Expiration: ").append(Constants.OLSR_NEIGHBOR_EXPIRATION_TIME).append(" ms\n");
//        }
//
//        return sb.toString();
//    }

    private TextFlow getSimulationStatsText() {
        TextFlow textFlow = new TextFlow();
        textFlow.setLineSpacing(5);
        textFlow.setPadding(new Insets(10));
        textFlow.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");

        addStyledLine(textFlow, "Protocol", getProtocolName());
        addStyledLine(textFlow, "Nodes", String.valueOf(Constants.SIMULATION_NR_NODES));
        addStyledLine(textFlow, "Duration", Constants.SIMULATION_TIME + " s");
        addStyledLine(textFlow, "Size", Constants.SIMULATION_SIZE_X + " x " + Constants.SIMULATION_SIZE_Y);
        addStyledLine(textFlow, "Frame Delay", Constants.SIMULATION_DELAY_BETWEEN_FRAMES + " ms");
        addStyledLine(textFlow, "Node Turn-Off %", (Constants.SIMULATION_PROBABILITY_NODE_TURN_OFF / 10.0) + "%");
        addStyledLine(textFlow, "Node Turn-On %", (Constants.SIMULATION_PROBABILITY_NODE_TURN_ON / 10.0) + "%");
        addStyledLine(textFlow, "Mobility", getMobilityName(Constants.NODE_MOBILITY_TYPE));
        addStyledLine(textFlow, "Comm Range", Constants.NODE_COMM_RANGE_MIN_VAL + "–" + Constants.NODE_COMM_RANGE_BOUND);
        addStyledLine(textFlow, "Node Speed", Constants.NODE_SPEED_MIN_VAL + "–" + Constants.NODE_SPEED_BOUND);
        addStyledLine(textFlow, "Pause Time", Constants.SIMULATION_PAUSE_TIME + " ms");
        addStyledLine(textFlow, "Log Level", String.valueOf(Constants.LOG_LEVEL));

        // Protocol-specific additions
        switch (getProtocolName()) {
            case "AODV":
                addStyledLine(textFlow, "Stale Route Timeout", Constants.NODE_AODV_STALE_ROUTE_PERIOD + " ms");
                break;

            case "SAODV":
                addStyledLine(textFlow, "Stale Route Timeout", Constants.NODE_AODV_STALE_ROUTE_PERIOD + " ms");
                addStyledLine(textFlow, "SAODV Key Size", Constants.SIMULATION_RSA_KEY_SIZE + " bits");
                addStyledLine(textFlow, "SAODV Fwd Buffer Size", String.valueOf(Constants.NODE_SAODV_FORWARD_BUFFER_SIZE));
                break;

            case "CBRP":
                addStyledLine(textFlow, "Hello Interval", Constants.NODE_CBRP_HELLO_INTERVAL + " ms");
                addStyledLine(textFlow, "Contention Period", Constants.NODE_CBRP_CONTENTION_PERIOD + " ms");
                addStyledLine(textFlow, "Undecided Period", Constants.NODE_CBRP_UNDECIDED_PD + " ms");
                break;

            case "OLSR":
                addStyledLine(textFlow, "Hello Interval", Constants.OLSR_HELLO_INTERVAL + " ms");
                addStyledLine(textFlow, "TC Interval", Constants.OLSR_TC_INTERVAL + " ms");
                addStyledLine(textFlow, "Neighbor Expiration", Constants.OLSR_NEIGHBOR_EXPIRATION_TIME + " ms");
                break;
        }

        // Extra system information
        addStyledLine(textFlow, "JVM Heap", (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " MB");
        addStyledLine(textFlow, "Free Memory", (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " MB");
        addStyledLine(textFlow, "Available CPUs", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addStyledLine(textFlow, "Java Version", System.getProperty("java.version"));
        addStyledLine(textFlow, "OS", System.getProperty("os.name"));

        return textFlow;
    }

    private void addStyledLine(TextFlow flow, String label, String value) {
        Text labelText = new Text(label + ": ");
        labelText.setStyle("-fx-font-weight: bold; -fx-fill: #222;");

        Text valueText = new Text(value + "\n");
        valueText.setStyle("-fx-fill: #444;");

        flow.getChildren().addAll(labelText, valueText);
    }




    private String getMobilityName(int type) {
        return switch (type) {
            case 0 -> "Static";
            case 1 -> "Random Direction";
            case 2 -> "Random Waypoint";
            default -> "Unknown";
        };
    }


    private void createBottomPanel() {
        bottomPanel = new HBox();
        bottomPanel.setPrefHeight(10);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(15, 20, 15, 20));
        bottomPanel.setSpacing(20);
        bottomPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format("-fx-background-color: %s; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6; " +
                "-fx-padding: 8 16 8 16;", color));

        // Add hover effect
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.9;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.9;", "")));

        return button;
    }

    private void initializeSimulation() {
        // Initialize animation phases for nodes
        for (int i = 0; i < Constants.SIMULATION_NR_NODES + 5; i++) {
            nodeAnimationPhases.put(i, Math.random() * Math.PI * 2);
        }

        // Setup ground based on simulation mode
        switch(Constants.SIMULATION_MODE) {
            case 1:
                ground.setupRandom_Standard(Constants.SIMULATION_NR_NODES);
                break;
            case 2:
                ground.setupFromFile_Standard("src/main/java/org/example/licentafromzero/Config/configuration1.txt");
                break;
            case 3:
                ground.setupRandom_DSRNode(Constants.SIMULATION_NR_NODES);
                break;
            case 4:
                ground.setupFromFile_DSRNode("src/main/java/org/example/licentafromzero/Config/configuration1.txt");
                break;
            case 5:
                ground.setupRandom_AODVNode(Constants.SIMULATION_NR_NODES);
                break;
            case 6:
                ground.setupFromFile_AODVNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");
                break;
            case 7:
                ground.setupRandom_SAODVNode(Constants.SIMULATION_NR_NODES);
                break;
            case 8:
                ground.setupFromFile_SAODVNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");
                break;
            case 9:
                ground.setupRandom_CBRPNode(Constants.SIMULATION_NR_NODES);
                break;
            case 10:
                ground.setupFromFile_CBRPNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");
                break;
            case 11:
                ground.setupRandom_OLSRNode(Constants.SIMULATION_NR_NODES);
                break;
            case 12:
                ground.setupFromFile_OLSRNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");
                break;
        }
    }

    private void startSimulation() {
        groundThread = new Thread(() -> {
            startTime = System.currentTimeMillis();
            ground.turnOnSimulationAsync(Constants.SIMULATION_TIME, () -> {
                long currentTime = System.currentTimeMillis();

                // Update animation phases
                if (lastUpdateTime > 0) {
                    double timeDelta = (currentTime - lastUpdateTime) / 1000.0;
                    for (Integer nodeId : nodeAnimationPhases.keySet()) {
                        double phase = nodeAnimationPhases.get(nodeId);
                        phase = (phase + timeDelta) % (Math.PI * 2);
                        nodeAnimationPhases.put(nodeId, phase);
                    }
                }
                lastUpdateTime = currentTime;

                // Update UI
                Platform.runLater(() -> {
                    drawSimulation();
                    updateTimeProgress();
                    updateNodeInfo();
                });
            });
        });

        groundThread.start();
    }

    private void drawSimulation() {
        simulationCanvas.getChildren().clear();

        // Draw background grid
        drawGrid();

        // Draw clusters first (so they appear behind nodes)
        if(ground.getProtocol().equals("CBRP"));
            drawClusters();

        // Draw connections between neighbors
        drawNeighbours();

        // Draw nodes
        for (Node node : ground.getNodes()) {
            double x = node.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
            double y = simulationCanvas.getHeight() - (node.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

            // Determine node type and status
            int nodeStatus = 0;
            if (node instanceof CBRP_Node) {
                nodeStatus = ((CBRP_Node) node).getNodeStatus();
            }

            drawNode(x, y, node.getId(), nodeStatus, ground.getOffNodes().contains(node.getId()));

            // Draw focus information if this node is focused
            if (ground.getFocusedNodeIndex() != -1) {
                Node focusNode = ground.getNodes().get(ground.getFocusedNodeIndex());
                if (focusNode == node) {
                    drawFocusedNodeInfo(x, y, node);
                }
            }
        }

        // Draw connections/messages
        drawConnections();
    }

    private void drawGrid() {
        double gridSpacing = 50;
        Color gridColor = Color.rgb(230, 230, 230, 0.5);

        for (double x = 0; x <= simulationCanvas.getWidth(); x += gridSpacing) {
            Line gridLine = new Line(x, 0, x, simulationCanvas.getHeight());
            gridLine.setStroke(gridColor);
            gridLine.setStrokeWidth(0.5);
            simulationCanvas.getChildren().add(gridLine);
        }

        for (double y = 0; y <= simulationCanvas.getHeight(); y += gridSpacing) {
            Line gridLine = new Line(0, y, simulationCanvas.getWidth(), y);
            gridLine.setStroke(gridColor);
            gridLine.setStrokeWidth(0.5);
            simulationCanvas.getChildren().add(gridLine);
        }
    }

    private void drawNode(double x, double y, int id, int status, boolean isOff) {
        // Create node shape based on status
        Color nodeColor = Color.ORANGE;
        String statusText = "";

        // Determine node appearance based on status
        switch(ground.getProtocol()) {
            case "CBRP":
                switch (status) {
                    case 1: // C_HEAD
                        nodeColor = Color.DARKRED;
                        statusText = "CH";
                        break;
                    case 2: // C_MEMBER
                        nodeColor = Color.FORESTGREEN;
                        statusText = "M";
                        break;
                    default: // C_UNDECIDED or other
                        nodeColor = Color.CORAL;
                        statusText = "U";
                        break;
                }
                break;
            default:
                nodeColor = Color.ORANGE;
                break;
        }

        // Apply "off" state if needed
        if (isOff) {
            nodeColor = nodeColor.deriveColor(0, 0.7, 0.5, 1.0);
        }

        // Create hexagon for node
        Polygon hexagon = createHexagon(x, y, NODE_SIZE);

        // Apply gradient fill
        Stop[] stops = new Stop[] {
                new Stop(0, nodeColor.brighter()),
                new Stop(1, nodeColor.darker())
        };
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);
        hexagon.setFill(gradient);

        // Add effects
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        hexagon.setEffect(dropShadow);

        // Create node ID label
        Text idLabel = new Text(x - 3, y + 5, String.valueOf(id));
        idLabel.setFill(Color.WHITE);
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Add pulsing animation effect for active nodes
        if (!isOff && nodeAnimationPhases.containsKey(id)) {
            double phase = nodeAnimationPhases.get(id);
            double pulseScale = 1.0 + 0.1 * Math.sin(phase * 3);
            hexagon.setScaleX(pulseScale);
            hexagon.setScaleY(pulseScale);
        }

        // Add click handler for node selection
        hexagon.setOnMouseClicked(e -> selectNode(id));
        idLabel.setOnMouseClicked(e -> selectNode(id));

        simulationCanvas.getChildren().addAll(hexagon, idLabel);

        // Add status indicator for CBRP
        if (ground.getProtocol().equals("CBRP") && !statusText.isEmpty()) {
            Circle statusIndicator = new Circle(x + NODE_SIZE, y - NODE_SIZE, 8);
            statusIndicator.setFill(Color.WHITE);
            statusIndicator.setStroke(nodeColor);
            statusIndicator.setStrokeWidth(2);

            Text statusLabel = new Text(x + NODE_SIZE - 4, y - NODE_SIZE + 4, statusText);
            statusLabel.setFill(nodeColor);
            statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 8));

            simulationCanvas.getChildren().addAll(statusIndicator, statusLabel);
        }
    }

    private Polygon createHexagon(double centerX, double centerY, double size) {
        Polygon hexagon = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = 2.0 * Math.PI / 6 * i;
            double x = centerX + size * Math.cos(angle);
            double y = centerY + size * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }
        return hexagon;
    }

    private void drawFocusedNodeInfo(double x, double y, Node node) {
        // Draw communication radius
        Circle radiusCircle = new Circle(x, y, node.getCommunicationRadius() *
                (simulationCanvas.getWidth() / (double) ground.getSizeX()));
        radiusCircle.setFill(Color.TRANSPARENT);
        radiusCircle.setStroke(Color.BLUE);
        radiusCircle.setStrokeWidth(2);
        radiusCircle.getStrokeDashArray().addAll(5d, 5d);

        // Show destination dot if using waypoint mobility
        if (Constants.NODE_MOBILITY_TYPE == 2) {
            double destX = node.getDestX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
            double destY = simulationCanvas.getHeight() - (node.getDestY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

            Circle destDot = new Circle(destX, destY, 12);
            destDot.setFill(Color.TRANSPARENT);
            destDot.setStroke(Color.DODGERBLUE);
            destDot.setStrokeWidth(2);
            destDot.getStrokeDashArray().addAll(4d, 4d);

            // Draw direction line
            double dx = destX - x;
            double dy = destY - y;
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length != 0) {
                double unitX = dx / length;
                double unitY = dy / length;
                double arrowLength = 30;

                Line directionLine = new Line(x, y, x + unitX * arrowLength, y + unitY * arrowLength);
                directionLine.setStroke(Color.DODGERBLUE);
                directionLine.setStrokeWidth(2);

                simulationCanvas.getChildren().addAll(directionLine, destDot);
            }
        }

        simulationCanvas.getChildren().add(radiusCircle);
    }

    private void drawClusters() {
        // Implementation similar to original but adapted for new canvas
        Map<Integer, List<Integer>> clusters = new HashMap<>();
        Map<Integer, Set<Integer>> clusterGateways = new HashMap<>();

        // Identify cluster heads
        for (Node node : ground.getNodes()) {
            if (node instanceof CBRP_Node) {
                CBRP_Node cbrpNode = (CBRP_Node) node;
                if (cbrpNode.getNodeStatus() == 1) { // C_HEAD
                    clusters.put(node.getId(), new ArrayList<>());
                    clusterGateways.put(node.getId(), new HashSet<>());
                }
            }
        }

        // Assign members and gateways
        for (Node node : ground.getNodes()) {
            if (node instanceof CBRP_Node) {
                CBRP_Node cbrpNode = (CBRP_Node) node;
                if (cbrpNode.getNodeStatus() == 2) { // C_MEMBER
                    List<Integer> hostClusters = cbrpNode.getHostClusters();
                    for (Integer headId : hostClusters) {
                        if (clusters.containsKey(headId)) {
                            clusters.get(headId).add(node.getId());
                        }
                    }
                } else if (cbrpNode.getNodeStatus() == 3) { // C_GATEWAY
                    List<Integer> hostClusters = cbrpNode.getHostClusters();
                    for (Integer headId : hostClusters) {
                        if (clusterGateways.containsKey(headId)) {
                            clusterGateways.get(headId).add(node.getId());
                        }
                    }
                }
            }
        }

        // Draw clusters
        int colorIndex = 0;
        for (Map.Entry<Integer, List<Integer>> cluster : clusters.entrySet()) {
            int headId = cluster.getKey();
            List<Integer> members = cluster.getValue();
            Set<Integer> gateways = clusterGateways.getOrDefault(headId, new HashSet<>());

            Node headNode = ground.getNodeFromId(headId);
            double headX = headNode.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
            double headY = simulationCanvas.getHeight() - (headNode.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

            // Calculate cluster radius
            double maxDistance = 50;
            List<Integer> allNodes = new ArrayList<>(members);
            allNodes.addAll(gateways);

            for (Integer nodeId : allNodes) {
                Node memberNode = ground.getNodeFromId(nodeId);
                double memberX = memberNode.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
                double memberY = simulationCanvas.getHeight() - (memberNode.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

                double distance = Math.sqrt(Math.pow(memberX - headX, 2) + Math.pow(memberY - headY, 2));
                if (distance > maxDistance) {
                    maxDistance = distance;
                }
            }

            maxDistance += CLUSTER_PADDING;

            // Draw cluster circle
            Circle clusterCircle = new Circle(headX, headY, maxDistance);
            Color baseColor = CLUSTER_COLORS[colorIndex % CLUSTER_COLORS.length];
            clusterCircle.setFill(baseColor);
            clusterCircle.setStroke(baseColor.deriveColor(0, 1, 0.7, 0.8));
            clusterCircle.setStrokeWidth(2.5);

            simulationCanvas.getChildren().add(clusterCircle);

            colorIndex++;
        }
    }

    private void drawNeighbours() {
        for (Node node : ground.getNodes()) {
            double x1 = node.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
            double y1 = simulationCanvas.getHeight() - (node.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

            for (Integer neighbour : new ArrayList<>(node.getNeighbours())) {
                Node destination = ground.getNodeFromId(neighbour);
                double x2 = destination.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
                double y2 = simulationCanvas.getHeight() - (destination.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

                Line line = new Line(x1, y1, x2, y2);
                line.setStroke(Color.rgb(180, 180, 180, 0.4));
                line.setStrokeWidth(1.4);

                simulationCanvas.getChildren().add(line);
            }
        }
    }

    private void drawConnections() {
        List<Message> messages = new ArrayList<>(ground.getMessageRouter().getMessages());

        for (Message message : messages) {
            Node source = ground.getNodes().get(message.getSource());
            if (source == ground.getNodes().get(ground.getFocusedNodeIndex())) {
                Node destination = ground.getNodeFromId(message.getDestination());

                double x1 = source.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
                double y1 = simulationCanvas.getHeight() - (source.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));
                double x2 = destination.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
                double y2 = simulationCanvas.getHeight() - (destination.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

                Line line = new Line(x1, y1, x2, y2);

                if (classifyMessageType(message) >= Constants.LOG_LEVEL && message.getNumberFramesShown() != 0) {
                    Color messageColor = message.isSuccessful() ? MESSAGE_COLORS[0] : MESSAGE_COLORS[1];
                    drawConnectionWithLabel(line, messageColor, message.getMessageType().toString());
                    message.decreaseNumberFramesShown();
                }
            }
        }
    }

    private void drawConnectionWithLabel(Line line, Color color, String label) {
        // Make the line more prominent
        line.setStroke(color);
        line.setStrokeWidth(3);
        line.getStrokeDashArray().clear(); // Remove dashes for cleaner look

        // Add subtle shadow effect to the line
        DropShadow lineShadow = new DropShadow();
        lineShadow.setRadius(2.0);
        lineShadow.setOffsetX(1.0);
        lineShadow.setOffsetY(1.0);
        lineShadow.setColor(Color.rgb(0, 0, 0, 0.2));
        line.setEffect(lineShadow);

        // Calculate line properties
        double dx = line.getEndX() - line.getStartX();
        double dy = line.getEndY() - line.getStartY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 20) return;

        // Create more prominent arrow head
        double arrowLength = 15;
        double arrowWidth = 8;
        double endRatio = 0.85;
        double arrowX = line.getStartX() + dx * endRatio;
        double arrowY = line.getStartY() + dy * endRatio;

        dx = dx / length;
        dy = dy / length;

        double perpX = -dy;
        double perpY = dx;

        Polygon arrowHead = new Polygon();
        arrowHead.getPoints().addAll(
                arrowX + dx * arrowLength, arrowY + dy * arrowLength,
                arrowX + perpX * arrowWidth, arrowY + perpY * arrowWidth,
                arrowX - perpX * arrowWidth, arrowY - perpY * arrowWidth
        );
        arrowHead.setFill(color);
        arrowHead.setEffect(lineShadow); // Same shadow as line

        simulationCanvas.getChildren().addAll(line, arrowHead);

        // Enhanced label with better visibility
        if (label != null && !label.isEmpty()) {
            double midX = (line.getStartX() + line.getEndX()) / 2;
            double midY = (line.getStartY() + line.getEndY()) / 2;

            // Create label text with better font
            Text labelText = new Text(midX, midY, label);
            labelText.setFill(Color.WHITE);
            labelText.setFont(Font.font("Arial", FontWeight.BOLD, 11));

            // Calculate text bounds for proper background sizing
            double textWidth = labelText.getBoundsInLocal().getWidth();
            double textHeight = labelText.getBoundsInLocal().getHeight();

            // Create rounded background with padding
            Rectangle textBg = new Rectangle(
                    midX - textWidth/2 - 6,
                    midY - textHeight/2 - 4,
                    textWidth + 12,
                    textHeight + 8
            );

            // Use the message color for background with good opacity
            textBg.setFill(color.deriveColor(0, 0.8, 0.9, 0.9));
            textBg.setStroke(color.deriveColor(0, 1.2, 0.7, 1.0));
            textBg.setStrokeWidth(1.5);
            textBg.setArcWidth(8);
            textBg.setArcHeight(8);

            // Add subtle shadow to the label background
            DropShadow labelShadow = new DropShadow();
            labelShadow.setRadius(3.0);
            labelShadow.setOffsetX(1.5);
            labelShadow.setOffsetY(1.5);
            labelShadow.setColor(Color.rgb(0, 0, 0, 0.3));
            textBg.setEffect(labelShadow);

            // Adjust text position to center it properly in the background
            labelText.setX(midX - textWidth/2);
            labelText.setY(midY + textHeight/4);

            simulationCanvas.getChildren().addAll(textBg, labelText);
        }
    }

    private void updateInfoPanel() {
        if (isMessageSelected && selectedMessage != null) {
            updateMessageInfo();
        } else if (focusedNodeIndex >= 0 && focusedNodeIndex < ground.getNodes().size()) {
            updateNodeInfo();
        } else {
            nodeInfoText.setText("No selection");
        }
    }

    private void updateMessageInfo() {
        if (selectedMessage != null) {
            StringBuilder info = new StringBuilder();
            info.append("=== MESSAGE INFORMATION ===\n\n");
            info.append("Type: ").append(selectedMessage.getMessageType()).append("\n");
            info.append("Source: Node ").append(selectedMessage.getSource()).append("\n");
            info.append("Destination: Node ").append(selectedMessage.getDestination()).append("\n");
            info.append("Status: ").append(selectedMessage.isSuccessful() ? "Successful" : "Failed").append("\n");
            info.append("Multicast: ").append(selectedMessage.isMulticast() ? "Yes" : "No").append("\n");
            info.append("Frames Shown: ").append(selectedMessage.getNumberFramesShown()).append("\n");

            // Add message content if available
            if (selectedMessage.getContent() != null) {
                info.append("Content: ").append(selectedMessage.getContent()).append("\n");
            }

            // Add source and destination node details
            Node sourceNode = ground.getNodes().get(selectedMessage.getSource());
            Node destNode = ground.getNodeFromId(selectedMessage.getDestination());

            info.append("\n=== SOURCE NODE ===\n");
            info.append("Position: (").append(sourceNode.getX()).append(", ").append(sourceNode.getY()).append(")\n");
            info.append("Active: ").append(sourceNode.isActive() ? "Yes" : "No").append("\n");

            info.append("\n=== DESTINATION NODE ===\n");
            info.append("Position: (").append(destNode.getX()).append(", ").append(destNode.getY()).append(")\n");
            info.append("Active: ").append(destNode.isActive() ? "Yes" : "No").append("\n");

            nodeInfoText.setText(info.toString());
        }
    }

    // Update the existing updateNodeInfo method name for consistency
    private void updateNodeInfo() {
        if (focusedNodeIndex >= 0 && focusedNodeIndex < ground.getNodes().size()) {
            Node node = ground.getNodes().get(focusedNodeIndex);
            nodeInfoText.setText(node.toInfo());
        }
    }

    private void selectMessage(Message message) {
        selectedMessage = message;
        isMessageSelected = true;
        focusedNodeIndex = -1; // Clear node selection
        updateInfoPanel();
    }

    private void selectNode(int nodeId) {
        focusedNodeIndex = nodeId;
        isMessageSelected = false;
        selectedMessage = null;
        updateInfoPanel();
    }

    private void clearSelection() {
        focusedNodeIndex = -1;
        isMessageSelected = false;
        selectedMessage = null;
        updateInfoPanel();
    }

    private void handleCanvasClick(MouseEvent event) {
        double clickX = event.getX();
        double clickY = event.getY();

        // First check for message clicks (higher priority since they're smaller targets)
        List<Message> messages = new ArrayList<>(ground.getMessageRouter().getMessages());
        for (Message message : messages) {
            if (message.getNumberFramesShown() > 0 &&
                    classifyMessageType(message) >= Constants.LOG_LEVEL) {

                Node source = ground.getNodes().get(message.getSource());
                Node destination = ground.getNodeFromId(message.getDestination());

                if (source != null && destination != null) {
                    double x1 = source.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
                    double y1 = simulationCanvas.getHeight() - (source.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));
                    double x2 = destination.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
                    double y2 = simulationCanvas.getHeight() - (destination.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

//                    // Calculate distance from click to message line
                    double distanceToLine = distanceFromPointToLine(clickX, clickY, x1, y1, x2, y2);

                    if (distanceToLine < 15) { // 15 pixel threshold for message selection
                        selectMessage(message);
                        return; // Exit early if message is selected
                    }

                }
            }
        }

        // If no message was clicked, check for nodes
        Node closestNode = null;
        double minDistance = Double.MAX_VALUE;

        for (Node node : ground.getNodes()) {
            double nodeX = node.getX() * (simulationCanvas.getWidth() / (double) ground.getSizeX());
            double nodeY = simulationCanvas.getHeight() - (node.getY() * (simulationCanvas.getHeight() / (double) ground.getSizeY()));

            double distance = Math.sqrt(Math.pow(clickX - nodeX, 2) + Math.pow(clickY - nodeY, 2));
            if (distance < minDistance && distance < 30) { // 30 pixel threshold
                minDistance = distance;
                closestNode = node;
            }
        }

        if (closestNode != null) {
            selectNode(closestNode.getId());
        } else {
            // Clear selection if nothing was clicked
            clearSelection();
        }
    }

    // Helper method to calculate distance from point to line
    private double distanceFromPointToLine(double px, double py, double x1, double y1, double x2, double y2) {
        double lineLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        if (lineLength == 0) return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));

        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (lineLength * lineLength)));
        double projectionX = x1 + t * (x2 - x1);
        double projectionY = y1 + t * (y2 - y1);

        return Math.sqrt(Math.pow(px - projectionX, 2) + Math.pow(py - projectionY, 2));
    }


    private void updateTimeProgress() {
        if (startTime > 0) {
            long elapsed = System.currentTimeMillis() - startTime - Constants.SIMULATION_PAUSE_TIME;
            long totalDuration = Constants.SIMULATION_TIME * 1000;

            double progress = Math.min(1.0, (double) elapsed / totalDuration);
            timeProgressBar.setProgress(progress);

            int elapsedSeconds = (int) (elapsed / 1000);
            int totalSeconds = Constants.SIMULATION_TIME;

            timeLabel.setText(String.format("%02d:%02d / %02d:%02d",
                    elapsedSeconds / 60, elapsedSeconds % 60,
                    totalSeconds / 60, totalSeconds % 60));
        }
    }

    private void startLogReader() {
        logReader = new LogReader("log.txt", this::appendToLog);
        logReaderThread = new Thread(logReader);
        logReaderThread.setDaemon(true);
        logReaderThread.start();
    }

    public void appendToLog(String logEntry) {
        logQueue.add(logEntry);
        while (logQueue.size() > MAX_LOG_ENTRIES) {
            logQueue.poll();
        }

        Platform.runLater(() -> {
            logTextArea.appendText(logEntry + "\n");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private int getFilterKeyword(String filter) {
        return switch (filter) {
            case "All Logs" -> 0;
            case "Node Status" -> 1;
            case "Messages" -> 2;
            case "Text" -> 3;
            default -> 0;
        };
    }

    private void setLogFilterComboBoxValue() {
        switch (Constants.LOG_LEVEL){
            case 0:
                logFilterComboBox.setValue("All Logs");
                break;
            case 1:
                logFilterComboBox.setValue("Node Status");
                break;
            case 2:
                logFilterComboBox.setValue("Messages");
                break;
            case 3:
                logFilterComboBox.setValue("Text");
                break;

        }

    }

    private String getProtocolName() {
        return switch (Constants.SIMULATION_MODE) {
            case 1, 2 -> "Standard";
            case 3, 4 -> "DSR";
            case 5, 6 -> "AODV";
            case 7, 8 -> "SAODV";
            case 9, 10 -> "CBRP";
            case 11, 12 -> "OLSR";
            default -> "Unknown";
        };
    }

    public int classifyMessageType(Message message) {
        MessageType type = message.getMessageType();

        if (type == MessageType.TEXT || type == MessageType.DSR_TEXT || type == MessageType.AODV_TEXT ||
                type == MessageType.SAODV_TEXT || type == MessageType.CBRP_TEXT || type == MessageType.OLSR_TEXT)
            return 3;

        if (type == MessageType.NEIGHBOUR_SYN || type == MessageType.NEIGHBOUR_ACK || type == MessageType.CBRP_NEIGHBOUR_HELLO)
            return 0;
        if (type == MessageType.DSR_RREQ || type == MessageType.DSR_RREP || type == MessageType.DSR_RERR)
            return 2;
        if (type == MessageType.AODV_RREQ || type == MessageType.AODV_RREP || type == MessageType.AODV_RERR)
            return 2;
        if (type == MessageType.SAODV_RREQ || type == MessageType.SAODV_RREP || type == MessageType.SAODV_RERR)
            return 2;
        if (type == MessageType.CBRP_RERR || type == MessageType.CBRP_RREP || type == MessageType.CBRP_RREQ)
            return 2;

        return 10;
    }

    // Log reader class
    private static class LogReader implements Runnable {
        private final String logFilePath;
        private final Consumer<String> logConsumer;
        private long lastPosition = 0;

        public LogReader(String logFilePath, Consumer<String> logConsumer) {
            this.logFilePath = logFilePath;
            this.logConsumer = logConsumer;
        }

        @Override
        public void run() {
            try {
                // Create the log file if it doesn't exist
                File logFile = new File(logFilePath);
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }

                // Initial log message
                logConsumer.accept("Log monitoring started for: " + logFilePath);

                while (!Thread.currentThread().isInterrupted()) {
                    // Check if file exists and has been modified
                    if (logFile.exists() && logFile.length() > lastPosition) {
                        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                            raf.seek(lastPosition);
                            String line;
                            while ((line = raf.readLine()) != null) {
                                if (!line.startsWith(" ")) {
                                    logConsumer.accept(line);
                                }
                            }
                            lastPosition = raf.getFilePointer();
                        }
                    }

                    // Sleep to avoid high CPU usage
                    Thread.sleep(20);
                }
            } catch (IOException | InterruptedException e) {
                logConsumer.accept("Log reader error: " + e.getMessage());
            }
        }
    }

    // Consumer interface for log handling
    @FunctionalInterface
    private interface Consumer<T> {
        void accept(T t);
    }
}