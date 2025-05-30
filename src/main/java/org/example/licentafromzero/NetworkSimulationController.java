package org.example.licentafromzero;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
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
import javafx.scene.text.TextAlignment;
import org.example.licentafromzero.CBRP_Paper.CBRP_Node;
import org.example.licentafromzero.Domain.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

//TODO: add home screen with display protoclol, and modify constants
//TODO: Add node details when clicked on node
//TODO: Add moving around the ground


//TODO: Timers don't really work after pausing. rethink all strucure.

public class NetworkSimulationController {


    @FXML
    private Pane canvas;
    @FXML
    public Pane controlPanel;
    @FXML
    public Slider speedSlider;
    @FXML
    public Button stopButton;
    public Label timeLabel;

    // New UI components for log display
    @FXML
    private VBox logContainer;
    private TextArea logTextArea;
    private ScrollPane logScrollPane;
    private Label logTitleLabel;
    private Button clearLogButton;
    private ComboBox<String> logFilterComboBox;

    // Log handling
    private ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_ENTRIES = 10000;
    private LogReader logReader;
    private Thread logReaderThread;

    int canvasX, canvasY;
    boolean stopSimulation = false;
    int lastFrameRate;

    // Node visualization constants
    private static final double NODE_SIZE = 16;
    private static final double CLUSTER_HEAD_INDICATOR_SIZE = 22;
    private static final double CLUSTER_PADDING = 25;

    // Animation tracking
    private long lastUpdateTime = 0;
    private long pauseTime = 0;
    private Map<Integer, Double> nodeAnimationPhases = new HashMap<>();

    private Ground ground = new Ground(900, 900);
    private Thread groundThread;

    private long startTime, simDuration;

    // Color schemes for better visualization
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

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // Restructure the layout to add log panel
            restructureLayout();

            canvasX = (int) canvas.getWidth();
            canvasY = (int) canvas.getHeight();
            initializeControlPanel();
            initializeLogPanel();

            // Start log reader
            startLogReader();

            // Initialize animation phases for nodes
            for (int i = 0; i < Constants.SIMULATION_NR_NODES + 5; i++) {
                nodeAnimationPhases.put(i, Math.random() * Math.PI * 2);
            }

            switch(Constants.SIMULATION_MODE){
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

            // Start async simulation and update UI on each tick
            groundThread = new Thread((() -> {
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

                    drawGround();
                    drawConnections();
                    timeLabel.setText(String.valueOf(calculateTimeLeft()));

                });
            }));

            groundThread.start();
        });
    }

    private void restructureLayout() {
        // Get the parent of the current layout
        Pane rootPane = (Pane) canvas.getParent().getParent();

        // Remove the existing VBox
        rootPane.getChildren().clear();

        // Create a new HBox as the main container
        HBox mainContainer = new HBox();
        mainContainer.setPrefSize(1300, 1000); // Wider to accommodate log panel

        // Create a VBox for the simulation part (control panel + canvas)
        VBox simulationContainer = new VBox();
        simulationContainer.setPrefSize(900, 1000);

        // Move the control panel and canvas to the simulation container
        simulationContainer.getChildren().addAll(controlPanel, canvas);

        // Create the log panel container
        logContainer = new VBox();
        logContainer.setPrefSize(500, 1000);
        logContainer.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");

        // Add both containers to the main container
        mainContainer.getChildren().addAll(simulationContainer, logContainer);

        // Add the main container to the root pane
        rootPane.getChildren().add(mainContainer);

        // Update the root pane size
        rootPane.setPrefSize(1300, 1000);
    }

    private void initializeLogPanel() {
        // Create title label
        logTitleLabel = new Label("Simulation Logs");
        logTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        logTitleLabel.setPadding(new Insets(10, 10, 5, 10));

        // Create filter controls
        HBox filterBox = new HBox(10);
        filterBox.setPadding(new Insets(5, 10, 5, 10));
        filterBox.setAlignment(Pos.CENTER_LEFT);

        logFilterComboBox = new ComboBox<>();
        logFilterComboBox.getItems().addAll("All Logs", "Node Status", "Messages", "Text");
        logFilterComboBox.setValue("All Logs");
        logFilterComboBox.setPrefWidth(150);

        clearLogButton = new Button("Clear Logs");
        clearLogButton.setOnAction(e -> logTextArea.clear());

        filterBox.getChildren().addAll(new Label("Filter:"), logFilterComboBox, clearLogButton);

        // Create log text area
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");

        // Create scroll pane for logs
        logScrollPane = new ScrollPane(logTextArea);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setFitToHeight(true);
        logScrollPane.setPrefHeight(900);
        logScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        // Add components to log container
        logContainer.getChildren().addAll(logTitleLabel, filterBox, logScrollPane);
        VBox.setVgrow(logScrollPane, Priority.ALWAYS);

        // Set up filter listener
        logFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            Util.log("Log filter changed to: " + newVal);
            Constants.LOG_LEVEL = getFilterKeyword(newVal);
        });
    }

    private void startLogReader() {
        logReader = new LogReader("log.txt", this::appendToLog);
        logReaderThread = new Thread(logReader);
        logReaderThread.setDaemon(true);
        logReaderThread.start();
    }

    public void appendToLog(String logEntry) {
        // Add to queue and trim if needed
        logQueue.add(logEntry);
        while (logQueue.size() > MAX_LOG_ENTRIES) {
            logQueue.poll();
        }

        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            logTextArea.appendText(logEntry + "\n");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private int getFilterKeyword(String filter) {
        switch (filter) {
            case "All Logs": return 0;
            case "Node Status": return 1;
            case "Messages": return 2;
            case "Text": return 3;
            default: return 0;
        }
    }

    public void initializeControlPanel() {
        // Apply a gradient background to the control panel
        String gradientStyle = "-fx-background-color: linear-gradient(to bottom, #a1c4fd, #c2e9fb);";
        controlPanel.setStyle(gradientStyle + "-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #8da9c4; -fx-border-width: 2;");

        speedSlider.setMin(0);
        speedSlider.setMax(1500);
        speedSlider.setValue(Constants.SIMULATION_DELAY_BETWEEN_FRAMES);

        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setMajorTickUnit(100);
        speedSlider.setMinorTickCount(0);

        speedSlider.setStyle(
                """
                -fx-padding: 10px;
                -fx-font-size: 16px;
                -fx-control-inner-background: #e6f2ff;
                -fx-tick-label-fill: #2c3e50;
                -fx-tick-mark-stroke: #3498db;
                """
        );

        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = newValue.intValue();
        });

        stopButton.setText("Turn off");
        stopButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
    }

    private void drawGround() {
        canvas.getChildren().clear();

        // Draw background grid for better spatial reference
        drawGrid();

        // Draw clusters first (so they appear behind nodes)
        drawClusters();

        // Draw connections between neighbors
        drawNeighbours();

        // Draw nodes
        for (Node node : ground.getNodes()) {
            double x = node.getX() * (canvasX / (double) ground.getSizeX());
            double y = canvasY - (node.getY() * (canvasY / (double) ground.getSizeY())); // Flip Y-axis

            // Determine node type and status
            int nodeStatus = 0; // Default
            if (node instanceof CBRP_Node) {
                nodeStatus = ((CBRP_Node) node).getNodeStatus();
            }

            // Draw the node with appropriate styling based on status
            drawNode(x, y, node.getId(), nodeStatus, ground.getOffNodes().contains(node.getId()));

            // Draw focus information if this node is focused
            if (ground.getFocusedNodeIndex() != -1) {
                Node focusNode = ground.getNodes().get(ground.getFocusedNodeIndex());
                if (focusNode == node) {
                    drawFocusedNodeInfo(x, y, node);
                }
            }
        }
    }

    private void drawGrid() {
        // Draw a subtle grid for better spatial reference
        double gridSpacing = 50;
        Color gridColor = Color.rgb(230, 230, 230);

        for (double x = 0; x <= canvasX; x += gridSpacing) {
            Line gridLine = new Line(x, 0, x, canvasY);
            gridLine.setStroke(gridColor);
            gridLine.setStrokeWidth(0.5);
            canvas.getChildren().add(gridLine);
        }

        for (double y = 0; y <= canvasY; y += gridSpacing) {
            Line gridLine = new Line(0, y, canvasX, y);
            gridLine.setStroke(gridColor);
            gridLine.setStrokeWidth(0.5);
            canvas.getChildren().add(gridLine);
        }
    }

    private void drawNode(double x, double y, int id, int status, boolean isOff) {
        // Create node shape based on status
        Color nodeColor;
        String nodeLabel = String.valueOf(id);
        String statusText = "";

        // Determine node appearance based on status
        nodeColor = Color.ORANGE;
        statusText = "";
        switch(ground.getProtocol()){
            case "STANDARD": break;
            case "DSR": break;
            case "AODV": break;
            case "SAODV": break;
            case "OLSR": break;
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
        }


        // Apply "off" state if needed
        if (isOff) {
            nodeColor = nodeColor.deriveColor(0, 0.7, 0.5, 1.0); // Darker, desaturated version
        }

        // Create hexagon for node
        Polygon hexagon = createHexagon(x, y, NODE_SIZE);

        // Apply gradient fill for better 3D effect
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
        Text idLabel = new Text(x - 3, y + 5, nodeLabel);
        idLabel.setFill(Color.WHITE);
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Create status indicator
        Circle statusIndicator = new Circle(x + NODE_SIZE, y - NODE_SIZE, 8);
        statusIndicator.setFill(Color.WHITE);
        statusIndicator.setStroke(nodeColor);
        statusIndicator.setStrokeWidth(2);

        Text statusLabel = new Text(x + NODE_SIZE - 4, y - NODE_SIZE + 4, statusText);
        statusLabel.setFill(nodeColor);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 8));

        // Add pulsing animation effect for active nodes
        if (!isOff && nodeAnimationPhases.containsKey(id)) {
            double phase = nodeAnimationPhases.get(id);
            double pulseScale = 1.0 + 0.1 * Math.sin(phase * 3);
            hexagon.setScaleX(pulseScale);
            hexagon.setScaleY(pulseScale);

            // Add glow effect for cluster heads
            if (status == 1) {
                Glow glow = new Glow();
                glow.setLevel(0.5 + 0.3 * Math.sin(phase * 2));
                hexagon.setEffect(glow);
            }
        }

        canvas.getChildren().addAll(hexagon, idLabel);
        if(ground.getProtocol().equals("CBRP"))
            canvas.getChildren().addAll(statusIndicator, statusLabel);
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
        Circle radiusCircle = new Circle(x, y, node.getCommunicationRadius());
        radiusCircle.setFill(Color.TRANSPARENT);
        radiusCircle.setStroke(Color.BLUE);
        radiusCircle.setStrokeWidth(2);
        radiusCircle.getStrokeDashArray().addAll(5d, 5d);

        // Create a background for the runtime text
        Text runtimeLabel = new Text(x - 50, y + 45, "Runtime: " + node.getTotalRunTime() + " ms");
        runtimeLabel.setFill(Color.WHITE);
        runtimeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Show destination dot if using waypoint mobility
        if (Constants.NODE_MOBILITY_TYPE == 2) {
            Circle destDot = new Circle(node.getDestX(), node.getDestY(), 12); // Slightly larger for visibility
            destDot.setFill(Color.TRANSPARENT);
            destDot.setStroke(Color.DODGERBLUE);
            destDot.setStrokeWidth(2);
            destDot.getStrokeDashArray().addAll(4d, 4d); // Dotted outline
            // Calculate unit direction vector from node to destination

            double dx = node.getDestX() - x;
            double dy = node.getDestY() - y;
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length != 0) {
                double unitX = dx / length;
                double unitY = dy / length;

                // Draw a small line (vector) from the node towards the destination
                double arrowLength = 30;
                Line directionLine = new Line(
                        x, y,
                        x + unitX * arrowLength,
                        y + unitY * arrowLength
                );
                directionLine.setStroke(Color.DODGERBLUE);
                directionLine.setStrokeWidth(2);

                Text destinationLabel = new Text(node.getDestX() - 25, node.getDestY() + 25, "Destination");
                destinationLabel.setFill(Color.GRAY);
                destinationLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));


                canvas.getChildren().addAll(directionLine, destinationLabel);
            }

            canvas.getChildren().add(destDot);
        }

        // Add a background rectangle for better readability
        Rectangle textBg = new Rectangle(
                x - 60, y + 32,
                runtimeLabel.getBoundsInLocal().getWidth() + 10,
                runtimeLabel.getBoundsInLocal().getHeight() + 2
        );
        textBg.setFill(Color.rgb(0, 0, 0, 0.7));
        textBg.setArcWidth(10);
        textBg.setArcHeight(10);

        // Add additional info for CBRP nodes
        if (node instanceof CBRP_Node) {
            CBRP_Node cbrpNode = (CBRP_Node) node;
            String statusText = "Status: ";
            switch (cbrpNode.getNodeStatus()) {
                case 0: statusText += "Undecided"; break;
                case 1: statusText += "Cluster Head"; break;
                case 2: statusText += "Member"; break;
                case 3: statusText += "Gateway"; break;
                default: statusText += "Unknown"; break;
            }

            Text statusLabel = new Text(x - 30, y + 65, statusText);
            statusLabel.setFill(Color.WHITE);
            statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

            Rectangle statusBg = new Rectangle(
                    x - 35, y + 52,
                    statusLabel.getBoundsInLocal().getWidth() + 10,
                    statusLabel.getBoundsInLocal().getHeight() + 2
            );
            statusBg.setFill(Color.rgb(0, 0, 0, 0.7));
            statusBg.setArcWidth(10);
            statusBg.setArcHeight(10);

            canvas.getChildren().addAll(statusBg, statusLabel);
        }

        canvas.getChildren().addAll(radiusCircle, textBg, runtimeLabel);
    }

    private void drawConnectionWithLabel(Line line, Color color, String label) {
        // Ensure the line stays within the canvas bounds
        if (line.getStartY() < 0 || line.getEndY() < 0) {
            return; // Skip lines that go above the canvas (into control panel)
        }

        line.setStroke(color);
        line.setStrokeWidth(2);
        line.getStrokeDashArray().addAll(5d, 5d);

        // Add arrow head to show direction
        double arrowLength = 10;
        double arrowWidth = 5;

        double dx = line.getEndX() - line.getStartX();
        double dy = line.getEndY() - line.getStartY();
        double length = Math.sqrt(dx * dx + dy * dy);

        // Skip if line is too short
        if (length < 20) return;

        // Calculate the position of the arrow (slightly before the end point)
        double endRatio = 0.9; // Position at 90% of the line
        double arrowX = line.getStartX() + dx * endRatio;
        double arrowY = line.getStartY() + dy * endRatio;

        // Normalize direction vector
        dx = dx / length;
        dy = dy / length;

        // Calculate perpendicular vector
        double perpX = -dy;
        double perpY = dx;

        // Create arrow head
        Polygon arrowHead = new Polygon();
        arrowHead.getPoints().addAll(
                arrowX + dx * arrowLength, arrowY + dy * arrowLength,
                arrowX + perpX * arrowWidth, arrowY + perpY * arrowWidth,
                arrowX - perpX * arrowWidth, arrowY - perpY * arrowWidth
        );
        arrowHead.setFill(color);

        canvas.getChildren().addAll(line, arrowHead);

        if (label != null && !label.isEmpty()) {
            double midX = (line.getStartX() + line.getEndX()) / 2;
            double midY = (line.getStartY() + line.getEndY()) / 2;

            // Create background for better readability
            Text labelText = new Text(midX, midY, label);
            labelText.setFill(Color.DARKGOLDENROD);
            labelText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            labelText.setTextAlignment(TextAlignment.CENTER);

            // Calculate the bounds of the text for the background
            double textWidth = labelText.getBoundsInLocal().getWidth();
            double textHeight = labelText.getBoundsInLocal().getHeight();

            Rectangle textBg = new Rectangle(
                    midX - textWidth/2 - 3, midY - textHeight/2 - 3,
                    textWidth + 6, textHeight + 6
            );

            textBg.setFill(color.deriveColor(0, 1, 1, 0.7));
            textBg.setArcWidth(5);
            textBg.setArcHeight(5);

            double angle = Math.toDegrees(Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX()));
            if (angle > 90 || angle < -90) {
                angle += 180;  // Rotate the label by 180 degrees to avoid upside-down text
            }

            // Apply rotation to both background and text
            textBg.setRotate(angle);
            labelText.setRotate(angle);

            canvas.getChildren().addAll(textBg, labelText);
        }
    }

    private void drawClusters() {
        // Map to store cluster heads and their members
        Map<Integer, List<Integer>> clusters = new HashMap<>();
        Map<Integer, Set<Integer>> clusterGateways = new HashMap<>();

        // First pass: identify all cluster heads and initialize their member lists
        for (Node node : ground.getNodes()) {
            if (node instanceof CBRP_Node) {
                CBRP_Node cbrpNode = (CBRP_Node) node;
                if (cbrpNode.getNodeStatus() == 1) { // C_HEAD
                    clusters.put(node.getId(), new ArrayList<>());
                    clusterGateways.put(node.getId(), new HashSet<>());
                }
            }
        }

        // Second pass: assign members and gateways to their cluster heads
        for (Node node : ground.getNodes()) {
            if (node instanceof CBRP_Node) {
                CBRP_Node cbrpNode = (CBRP_Node) node;
                if (cbrpNode.getNodeStatus() == 2) { // C_MEMBER
                    // Get the cluster head(s) this node belongs to
                    List<Integer> hostClusters = cbrpNode.getHostClusters();
                    for (Integer headId : hostClusters) {
                        if (clusters.containsKey(headId)) {
                            clusters.get(headId).add(node.getId());
                        }
                    }
                } else if (cbrpNode.getNodeStatus() == 3) { // C_GATEWAY
                    // Get the cluster head(s) this gateway connects
                    List<Integer> hostClusters = cbrpNode.getHostClusters();
                    for (Integer headId : hostClusters) {
                        if (clusterGateways.containsKey(headId)) {
                            clusterGateways.get(headId).add(node.getId());
                        }
                    }
                }
            }
        }

        // Now draw the clusters with enhanced visualization
        int colorIndex = 0;

        for (Map.Entry<Integer, List<Integer>> cluster : clusters.entrySet()) {
            int headId = cluster.getKey();
            List<Integer> members = cluster.getValue();
            Set<Integer> gateways = clusterGateways.getOrDefault(headId, new HashSet<>());

            // Get the cluster head node
            Node headNode = ground.getNodeFromId(headId);
            double headX = headNode.getX() * (canvasX / (double) ground.getSizeX());
            double headY = canvasY - (headNode.getY() * (canvasY / (double) ground.getSizeY()));

            // Skip if the head is above the canvas (in control panel area)
            if (headY < 0) {
                continue;
            }

            // Calculate the maximum distance from head to any member
            double maxDistance = 0;

            // Include all members and gateways in calculation
            List<Integer> allNodes = new ArrayList<>(members);
            allNodes.addAll(gateways);

            for (Integer nodeId : allNodes) {
                Node memberNode = ground.getNodeFromId(nodeId);
                double memberX = memberNode.getX() * (canvasX / (double) ground.getSizeX());
                double memberY = canvasY - (memberNode.getY() * (canvasY / (double) ground.getSizeY()));

                // Calculate distance from head to member
                double distance = Math.sqrt(Math.pow(memberX - headX, 2) + Math.pow(memberY - headY, 2));
                if (distance > maxDistance) {
                    maxDistance = distance;
                }
            }

            // Add some padding to the radius
            maxDistance = Math.max(maxDistance, 50) + CLUSTER_PADDING;

            // Ensure the cluster doesn't extend above the canvas
            if (headY - maxDistance < 0) {
                // Adjust the radius to stay within canvas
                maxDistance = Math.max(headY - 10, 30);
            }

            // Draw the cluster circle centered on the head
            Circle clusterCircle = new Circle(headX, headY, maxDistance);

            // Create a radial gradient for the cluster fill
            Color baseColor = CLUSTER_COLORS[colorIndex % CLUSTER_COLORS.length];
            Stop[] stops = new Stop[] {
                    new Stop(0, baseColor.deriveColor(0, 1, 1.2, 0.4)),
                    new Stop(1, baseColor.deriveColor(0, 1, 0.8, 0.2))
            };

            javafx.scene.paint.RadialGradient gradient = new javafx.scene.paint.RadialGradient(
                    0, 0, headX, headY, maxDistance, false, CycleMethod.NO_CYCLE, stops
            );

            clusterCircle.setFill(gradient);
            clusterCircle.setStroke(baseColor.deriveColor(0, 1, 0.7, 0.8));
            clusterCircle.setStrokeWidth(2.5);

            // Add inner glow effect
            InnerShadow innerGlow = new InnerShadow();
            innerGlow.setRadius(4.0);
            innerGlow.setColor(baseColor.deriveColor(0, 1, 1.5, 0.3));
            clusterCircle.setEffect(innerGlow);

            // Draw the cluster head indicator
            Circle headIndicator = new Circle(headX, headY, CLUSTER_HEAD_INDICATOR_SIZE);
            headIndicator.setFill(Color.TRANSPARENT);
            headIndicator.setStroke(Color.BLACK);
            headIndicator.setStrokeWidth(3);

            // Add cluster label
            Text clusterLabel = new Text(headX - 40, headY - maxDistance + 20,
                    "Cluster " + headId + " (" + (members.size() + 1) + " nodes)");
            clusterLabel.setFill(Color.rgb(50, 50, 50, 0.8));
            clusterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

            // Add to canvas
            canvas.getChildren().addAll(clusterCircle, headIndicator, clusterLabel);

            // Draw connections from head to members with different styles
            for (Integer memberId : members) {
                Node memberNode = ground.getNodeFromId(memberId);
                double memberX = memberNode.getX() * (canvasX / (double) ground.getSizeX());
                double memberY = canvasY - (memberNode.getY() * (canvasY / (double) ground.getSizeY()));

                // Skip connections that go into the control panel
                if (memberY < 0) continue;

                Line line = new Line(headX, headY, memberX, memberY);
                line.setStroke(baseColor.deriveColor(0, 1, 0.7, 0.7));
                line.setStrokeWidth(1.5);
                line.getStrokeDashArray().addAll(5d, 5d);

                canvas.getChildren().add(line);
            }

            // Draw gateway connections with different style
            for (Integer gatewayId : gateways) {
                Node gatewayNode = ground.getNodeFromId(gatewayId);
                double gatewayX = gatewayNode.getX() * (canvasX / (double) ground.getSizeX());
                double gatewayY = canvasY - (gatewayNode.getY() * (canvasY / (double) ground.getSizeY()));

                // Skip connections that go into the control panel
                if (gatewayY < 0) continue;

                Line line = new Line(headX, headY, gatewayX, gatewayY);
                line.setStroke(Color.PURPLE);
                line.setStrokeWidth(2.0);
                line.getStrokeDashArray().addAll(2d, 2d);

                canvas.getChildren().add(line);

                // Add gateway label
                Text gwLabel = new Text(gatewayX + 15, gatewayY - 5, "GW");
                gwLabel.setFill(Color.PURPLE);
                gwLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                canvas.getChildren().add(gwLabel);
            }

            colorIndex++;
        }
}

    public int classifyMessageType(Message message) {
        MessageType type = message.getMessageType();

        if (type == MessageType.TEXT || type == MessageType.DSR_TEXT || type == MessageType.AODV_TEXT ||
                type == MessageType.SAODV_TEXT || type == MessageType.CBRP_TEXT || type == MessageType.OLSR_TEXT )
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

    private void drawConnections() {
        List<Message> messages = new ArrayList<>(ground.getMessageRouter().getMessages());

        for (Message message : messages) {
            Node source = ground.getNodes().get(message.getSource());
            if (source == ground.getNodes().get(ground.getFocusedNodeIndex())) {
                Node destination = ground.getNodeFromId(message.getDestination());

                double x1 = source.getX() * (canvasX / (double) ground.getSizeX());
                double y1 = canvasY - (source.getY() * (canvasY / (double) ground.getSizeY()));
                double x2 = destination.getX() * (canvasX / (double) ground.getSizeX());
                double y2 = canvasY - (destination.getY() * (canvasY / (double) ground.getSizeY()));

                // Skip connections that go into the control panel
                if (y1 < 0 || y2 < 0) continue;

                Line line = new Line(x1, y1, x2, y2);

                if (classifyMessageType(message) >= Constants.LOG_LEVEL && message.getNumberFramesShown() != 0) {
                    if (message.isSuccessful()) {
                        drawConnectionWithLabel(line, MESSAGE_COLORS[0], message.getMessageType().toString());

                        // Add animation for successful messages
                        Circle packetAnimation = new Circle(x1, y1, 5);
                        packetAnimation.setFill(MESSAGE_COLORS[0]);

                        // Add glow effect
                        Glow glow = new Glow();
                        glow.setLevel(0.7);
                        packetAnimation.setEffect(glow);

                        canvas.getChildren().add(packetAnimation);

                        // Calculate animation path
                        double animX = x1 + (x2 - x1) * 0.5; // Position at 50% of path
                        double animY = y1 + (y2 - y1) * 0.5;

                        // Set the animation position
                        packetAnimation.setCenterX(animX);
                        packetAnimation.setCenterY(animY);
                    } else {
                        drawConnectionWithLabel(line, MESSAGE_COLORS[1], message.getMessageType().toString());
                    }
                    message.decreaseNumberFramesShown();
                }
            }
        }
    }

    private void drawNeighbours() {
        for (Node node : ground.getNodes()) {
            Node source = node;
            double x1 = source.getX() * (canvasX / (double) ground.getSizeX());
            double y1 = canvasY - (source.getY() * (canvasY / (double) ground.getSizeY()));

            // Skip nodes that are in the control panel area
            if (y1 < 0) continue;

            for (Integer neighbour : new ArrayList<>(source.getNeighbours())) {
                Node destination = ground.getNodeFromId(neighbour);
                double x2 = destination.getX() * (canvasX / (double) ground.getSizeX());
                double y2 = canvasY - (destination.getY() * (canvasY / (double) ground.getSizeY()));

                // Skip connections that go into the control panel
                if (y2 < 0) continue;

                // Draw a subtle line for neighbor connections
                Line line = new Line(x1, y1, x2, y2);
                line.setStroke(Color.rgb(180, 180, 180, 0.4));
                line.setStrokeWidth(1.4);

                // Add a small circle at the midpoint to indicate bidirectional link
                double midX = (x1 + x2) / 2;
                double midY = (y1 + y2) / 2;
                Circle linkIndicator = new Circle(midX, midY, 3);
                linkIndicator.setFill(Color.rgb(100, 100, 100, 0.5));

                canvas.getChildren().addAll(line, linkIndicator);
            }
        }
    }

    public void handleStopButtonPressed(MouseEvent mouseEvent) {
        stopSimulation = !stopSimulation;
        System.err.println(stopSimulation);
        if (stopSimulation) {
            stopButton.setText("Turn on");
            stopButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
            lastFrameRate = Constants.SIMULATION_DELAY_BETWEEN_FRAMES;
            pauseTime = System.currentTimeMillis();
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = 10000000;
            appendToLog("Simulation paused");
        } else {
            stopButton.setText("Turn off");
            stopButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
            groundThread.interrupt();
            Constants.SIMULATION_PAUSE_TIME += System.currentTimeMillis() - pauseTime;
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = lastFrameRate;
            appendToLog("Simulation resumed");
        }
    }

    private long calculateTimeLeft(){
        long timeLeft = startTime + (Constants.SIMULATION_TIME*1000) - System.currentTimeMillis() + Constants.SIMULATION_PAUSE_TIME;

        if(timeLeft < 0)
            timeLeft = 0;

        return timeLeft;
    }

    // Log reader class to monitor the log file
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
                    Thread.sleep(500);
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