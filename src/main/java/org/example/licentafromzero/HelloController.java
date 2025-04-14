package org.example.licentafromzero;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import org.example.licentafromzero.Domain.*;

import java.util.ArrayList;

public class HelloController {
    @FXML
    private Pane canvas;

    int canvasX, canvasY;

    private Ground ground = new Ground(900,900);

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            canvasX = (int) canvas.getWidth();
            canvasY = (int) canvas.getHeight();

            ground.setupRandom(Constants.SIMULATION_NR_NODES);

            // Start async simulation and update UI on each tick
            ground.turnOnSimulationAsync(Constants.SIMULATION_TIME, () -> {
                drawGround();
                drawConnections();
            });
        });
    }


    private void drawGround() {
        canvas.getChildren().clear(); // Optional: Clear if redrawing

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
            triangle.setFill(Color.ORANGE);



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
    }

    private void drawLineWithFade(Line line, Color color) {
        line.setStroke(color);
        line.setStrokeWidth(3);

        canvas.getChildren().add(line);
    }

    private void drawConnections() {
        for (Message message : ground.getMessageRouter().getMessages()) {
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
                        drawLineWithFade(line, Color.GREEN);
                    else
                        drawLineWithFade(line, Color.RED);
                    message.decreaseNumberFramesShown();
                }
            }
        }
    }

}