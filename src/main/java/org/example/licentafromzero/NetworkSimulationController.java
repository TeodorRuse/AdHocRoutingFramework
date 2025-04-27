package org.example.licentafromzero;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
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
                    Circle circle = new Circle(x, y, node.getCommunicationRadius());
                    circle.setFill(Color.TRANSPARENT);
                    circle.setStroke(Color.BLUE);
                    circle.setStrokeWidth(1);
                    canvas.getChildren().add(circle);
                }
            }

            Text idLabel = new Text(x - 2, y + 20, String.valueOf(node.getId()));

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

    double angle = Math.toDegrees(Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX()));
    labelText.setRotate(angle);

    canvas.getChildren().add(labelText);
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

                if (message.getNumberFramesShown() != 0) {
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