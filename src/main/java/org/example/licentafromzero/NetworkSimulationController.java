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
import org.example.licentafromzero.Domain.*;

import java.util.ArrayList;
import java.util.List;

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
            ground.setupFromFile_SAODVNode("src/main/java/org/example/licentafromzero/Config/configuration2.txt");



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

//    private void drawConnectionWithLabel(Line line, Color color, String label, double labelOffsetY) {
//        line.setStroke(color);
//        line.setStrokeWidth(2);
//        line.getStrokeDashArray().addAll(5d, 5d); // Dotted line
//
//        canvas.getChildren().add(line);
//
//        // Position the label in the middle of the line with an additional vertical offset for each label
//        double midX = (line.getStartX() + line.getEndX()) / 2 - 30;
//        double midY = (line.getStartY() + line.getEndY()) / 2 + labelOffsetY; // Apply the vertical offset
//
//        Text labelText = new Text(midX, midY, label);
//        labelText.setFill(color);
//        labelText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
//        labelText.setTextAlignment(TextAlignment.LEFT);
//
//        // Rotate the label to match the line angle
//        double angle = Math.toDegrees(Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX()));
//
//        if (angle > 90) {
//            angle -= 180;  // Rotate the label by 180 degrees to avoid upside-down text
//        } else if (angle < -90) {
//            angle += 180;  // Rotate the label by 180 degrees to avoid upside-down text
//        }
//
//        labelText.setRotate(angle);
//
//        canvas.getChildren().add(labelText);
//    }
//
//    private void drawConnections() {
//        List<Message> messages = new ArrayList<>(ground.getMessageRouter().getMessages());
//
//        // Track how many labels have been drawn for each (source, destination) pair
//        Map<String, Integer> labelOffsets = new HashMap<>();
//
//        for (Message message : messages) {
//            Node source = ground.getNodes().get(message.getSource());
//            if (source == ground.getNodes().get(ground.getFocusedNodeIndex())) {
//                Node destination = ground.getNodeFromId(message.getDestination());
//
//                double x1 = source.getX() * (canvasX / (double) ground.getSizeX());
//                double y1 = canvasY - (source.getY() * (canvasY / (double) ground.getSizeY()));
//
//                double x2 = destination.getX() * (canvasX / (double) ground.getSizeX());
//                double y2 = canvasY - (destination.getY() * (canvasY / (double) ground.getSizeY()));
//
//                // Create the line (only once for each source-destination pair)
//                Line line = new Line(x1, y1, x2, y2);
//
//                // Create a unique key for the source and destination pair
//                String key = source.getId() + "-" + destination.getId();
//
//                // Get the number of labels already drawn for this pair (default to 0 if not yet drawn)
//                int labelCount = labelOffsets.getOrDefault(key, 0);
//
//                // Draw the line only once for this pair
//                if (labelCount == 0) {
//                    if (message.getNumberFramesShown() != 0) {
//                        if (message.isSuccessful()) {
//                            drawConnectionWithLabel(line, Color.GREEN, message.getMessageType().toString(), 0);
//                        } else {
//                            drawConnectionWithLabel(line, Color.RED, message.getMessageType().toString(), 0);
//                        }
//                        message.decreaseNumberFramesShown();
//                    }
//                }
//
//                // Draw the label with an offset if it's not the first one
//                if (message.getNumberFramesShown() != 0) {
//                    double labelOffsetY = labelCount * 15; // Increment label position by 15px for each additional label
//                    if (message.isSuccessful()) {
//                        drawConnectionWithLabel(line, Color.GREEN, message.getMessageType().toString(), labelOffsetY);
//                    } else {
//                        drawConnectionWithLabel(line, Color.RED, message.getMessageType().toString(), labelOffsetY);
//                    }
//                    message.decreaseNumberFramesShown();
//                }
//
//                // Increment the label offset for this (source, destination) pair
//                labelOffsets.put(key, labelCount + 1);
//            }
//        }
//    }


    private void drawNeighbours() {
        for (Node node: ground.getNodes()) {
            Node source = node;

//            for(Integer neighbour : source.getNeighbours()) {
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