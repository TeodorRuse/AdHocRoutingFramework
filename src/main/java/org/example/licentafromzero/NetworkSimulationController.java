package org.example.licentafromzero;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.example.licentafromzero.CBRP_Paper.CBRP_Node;
import org.example.licentafromzero.Domain.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkSimulationController {



    @FXML
    private Pane canvas;
    public Pane controlPanel;
    public Slider speedSlider;
    public Button stopButton;

    int canvasX, canvasY;
    boolean stopSimulation = false;
    int lastFrameRate;

    private Ground ground = new Ground(900,900);
    private Thread groundThread;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            canvasX = (int) canvas.getWidth();
            canvasY = (int) canvas.getHeight();
            initializeControlPanel();

//            ground.setupRandom_Standard(Constants.SIMULATION_NR_NODES);
//            ground.setupFromFile_Standard("src/main/java/org/example/licentafromzero/Config/configuration1.txt");

//            ground.setupRandom_DSRNode(Constants.SIMULATION_NR_NODES);
//            ground.setupFromFile_DSRNode("src/main/java/org/example/licentafromzero/Config/configuration1.txt");

//            ground.setupRandom_AODVNode(Constants.SIMULATION_NR_NODES);
//            ground.setupFromFile_AODVNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");

//            ground.setupRandom_SAODVNode(Constants.SIMULATION_NR_NODES);
//            ground.setupFromFile_SAODVNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");

            ground.setupRandom_CBRPNode(Constants.SIMULATION_NR_NODES +5);



            // Start async simulation and update UI on each tick
            groundThread = new Thread((() -> {
                ground.turnOnSimulationAsync(Constants.SIMULATION_TIME, () -> {
                    drawGround();
                    drawConnections();
                });
            }));

            groundThread.start();
        });
    }

    public void initializeControlPanel(){
        controlPanel.setStyle("-fx-background-color: lightblue;");
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
                -fx-control-inner-background: #cccccc;
                """
        );

        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
//            int actuual = Math.abs(1500 - newValue.intValue());
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = newValue.intValue();
        });

        stopButton.setText("Turn off");
    }


    private void drawGround() {
        canvas.getChildren().clear();

        drawClusters();

        for (Node node : ground.getNodes()) {
            double x = node.getX() * (canvasX / (double) ground.getSizeX());
            double y = canvasY - (node.getY() * (canvasY / (double) ground.getSizeY())); // Flip Y-axis

            Polygon triangle = new Polygon();
            double size = 20;
            triangle.getPoints().addAll(
                    x, y - size,
                    x - size, y + size,
                    x + size, y + size
            );
//            triangle.setFill(Color.ORANGE);
            if (ground.getOffNodes().contains(node.getId())) {
                triangle.setFill(Color.rgb(100, 80, 0)); // Darker, grayish orange
            } else {
                triangle.setFill(Color.ORANGE);
            }


            if(ground.getFocusedNodeIndex()!= -1) {
                Node focusNode;
                focusNode = ground.getNodes().get(ground.getFocusedNodeIndex());


                if(focusNode == node) {
                    Text runtimeLabel = new Text(x - 2, y + 40, String.valueOf(node.getTotalRunTime()));
                    Circle circle = new Circle(x, y, node.getCommunicationRadius());
                    circle.setFill(Color.TRANSPARENT);
                    circle.setStroke(Color.BLUE);
                    circle.setStrokeWidth(1);
                    canvas.getChildren().addAll(circle, runtimeLabel);
                }
            }

            Text idLabel = new Text(x, y + 20, String.valueOf(node.getId()));


            canvas.getChildren().addAll(triangle, idLabel);
        }
        drawNeighbours();
    }

    private void drawConnectionWithLabel(Line line, Color color, String label) {
        line.setStroke(color);
        line.setStrokeWidth(2);
        line.getStrokeDashArray().addAll(5d, 5d);

        canvas.getChildren().add(line);

        double midX = (line.getStartX() + line.getEndX()) / 2;
        double midY = (line.getStartY() + line.getEndY()) / 2;

        Text labelText = new Text(midX, midY, label);
        labelText.setFill(color);
        labelText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        labelText.setTextAlignment(TextAlignment.LEFT);

        double angle = Math.toDegrees(Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX()));
        if (angle > 90) {
            angle -= 180;  // Rotate the label by 180 degrees to avoid upside-down text
        } else if (angle < -90) {
            angle += 180;  // Rotate the label by 180 degrees to avoid upside-down text
        }
        labelText.setRotate(angle);

        canvas.getChildren().add(labelText);
    }

    private void drawClusters() {
        // Map to store cluster heads and their members
        Map<Integer, List<Integer>> clusters = new HashMap<>();

        // First pass: identify all cluster heads and initialize their member lists
        for (Node node : ground.getNodes()) {
            if (node instanceof CBRP_Node) {
                CBRP_Node cbrpNode = (CBRP_Node) node;
                if (cbrpNode.getNodeStatus() == 1) { // C_HEAD
                    clusters.put(node.getId(), new ArrayList<>());
                }
            }
        }

        // Second pass: assign members to their cluster heads
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
                }
            }
        }

        // Now draw the clusters
        int colorIndex = 0;
        Color[] clusterColors = {
                Color.rgb(255, 200, 200, 0.3), // Light red
                Color.rgb(200, 255, 200, 0.3), // Light green
                Color.rgb(200, 200, 255, 0.3), // Light blue
                Color.rgb(255, 255, 200, 0.3), // Light yellow
                Color.rgb(255, 200, 255, 0.3), // Light purple
                Color.rgb(200, 255, 255, 0.3), // Light cyan
                Color.rgb(255, 220, 180, 0.3), // Light orange
                Color.rgb(220, 180, 255, 0.3)  // Light lavender
        };

        for (Map.Entry<Integer, List<Integer>> cluster : clusters.entrySet()) {
            int headId = cluster.getKey();
            List<Integer> members = cluster.getValue();

            // Get the cluster head node
            Node headNode = ground.getNodeFromId(headId);
            double headX = headNode.getX() * (canvasX / (double) ground.getSizeX());
            double headY = canvasY - (headNode.getY() * (canvasY / (double) ground.getSizeY()));

            // Calculate the center and radius of the cluster
            double centerX = headX;
            double centerY = headY;
            double maxDistance = 0;

            // Include all members in calculation
            for (Integer memberId : members) {
                Node memberNode = ground.getNodeFromId(memberId);
                double memberX = memberNode.getX() * (canvasX / (double) ground.getSizeX());
                double memberY = canvasY - (memberNode.getY() * (canvasY / (double) ground.getSizeY()));

                // Update center (simple average)
                centerX += memberX;
                centerY += memberY;

                // Calculate distance from head to member
                double distance = Math.sqrt(Math.pow(memberX - headX, 2) + Math.pow(memberY - headY, 2));
                if (distance > maxDistance) {
                    maxDistance = distance;
                }
            }

            // Finalize center calculation
            if (!members.isEmpty()) {
                centerX /= (members.size() + 1);
                centerY /= (members.size() + 1);
            }

            // Add some padding to the radius
            maxDistance = Math.max(maxDistance, 50) + 20;

            // Draw the cluster circle
            Circle clusterCircle = new Circle(centerX, centerY, maxDistance);
            clusterCircle.setFill(clusterColors[colorIndex % clusterColors.length]);
            clusterCircle.setStroke(Color.rgb(100, 100, 100, 0.5));
            clusterCircle.setStrokeWidth(2);

            // Draw the cluster head indicator
            Circle headIndicator = new Circle(headX, headY, 15);
            headIndicator.setFill(Color.TRANSPARENT);
            headIndicator.setStroke(Color.BLACK);
            headIndicator.setStrokeWidth(3);

            // Add to canvas
            canvas.getChildren().addAll(clusterCircle, headIndicator);

            // Draw connections from head to members
            for (Integer memberId : members) {
                Node memberNode = ground.getNodeFromId(memberId);
                double memberX = memberNode.getX() * (canvasX / (double) ground.getSizeX());
                double memberY = canvasY - (memberNode.getY() * (canvasY / (double) ground.getSizeY()));

                Line line = new Line(headX, headY, memberX, memberY);
                line.setStroke(Color.rgb(100, 100, 100, 0.5));
                line.setStrokeWidth(1.5);
                line.getStrokeDashArray().addAll(5d, 5d);

                canvas.getChildren().add(line);
            }

            colorIndex++;
        }
    }

    public int classifyMessageType(Message message) {
        MessageType type = message.getMessageType();

        if (type == MessageType.TEXT || type == MessageType.DSR_TEXT ||
                type == MessageType.AODV_TEXT || type == MessageType.SAODV_TEXT)
            return 2;

        if (type == MessageType.NEIGHBOUR_SYN || type == MessageType.NEIGHBOUR_ACK)
            return 0;
        if (type == MessageType.DSR_RREQ || type == MessageType.DSR_RREP || type == MessageType.DSR_RERR)
            return 1;
        if (type == MessageType.AODV_RREQ || type == MessageType.AODV_RREP || type == MessageType.AODV_RERR)
            return 1;
        if (type == MessageType.SAODV_RREQ || type == MessageType.SAODV_RREP || type == MessageType.SAODV_RERR)
            return 1;

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

                Line line = new Line(x1, y1, x2, y2);

                if (classifyMessageType(message) >= Constants.DISPLAY_DETAILS && message.getNumberFramesShown() != 0) {
                    if (message.isSuccessful())
                        drawConnectionWithLabel(line, Color.GREEN, message.getMessageType().toString());
                    else
                        drawConnectionWithLabel(line, Color.RED, message.getMessageType().toString());
                    message.decreaseNumberFramesShown();
                }
            }
        }
    }


    private void drawNeighbours() {
        for (Node node: ground.getNodes()) {
            Node source = node;

            for (Integer neighbour : new ArrayList<>(source.getNeighbours())) {
                Node destination = ground.getNodeFromId(neighbour);

                double x1 = source.getX() * (canvasX / (double) ground.getSizeX());
                double y1 = canvasY - (source.getY() * (canvasY / (double) ground.getSizeY()));

                double x2 = destination.getX() * (canvasX / (double) ground.getSizeX());
                double y2 = canvasY - (destination.getY() * (canvasY / (double) ground.getSizeY()));

                Line line = new Line(x1, y1, x2, y2);

                drawConnectionWithLabel(line, Color.DARKGRAY, "");
            }
        }
    }

    public void handleStopButtonPressed(MouseEvent mouseEvent) {
        //TODO: when stoped, time does not stop, all timers still run. If stopped from too long simulation will reach it's end
        stopSimulation = !stopSimulation;
        System.err.println(stopSimulation);
        if(stopSimulation) {
            stopButton.setText("Turn on");
            lastFrameRate = Constants.SIMULATION_DELAY_BETWEEN_FRAMES;
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = 10000000;
        }else {
            stopButton.setText("Turn off");
            groundThread.interrupt();
            System.err.println(lastFrameRate);
            Constants.SIMULATION_DELAY_BETWEEN_FRAMES = lastFrameRate;
        }
    }
}