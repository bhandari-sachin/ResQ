

// This is sample code for a simple JavaFX GUI application.

package fi.metropolia.simulation;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LauncherGUI extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulation GUI");

        // Create UI components
        Label welcomeLabel = new Label("Welcome to Metropolia Simu!");
        Button startButton = new Button("Start Simulation");
        Button exitButton = new Button("Exit");

        // Add button actions
        startButton.setOnAction(e -> {
            System.out.println("Starting simulation...");
            // Add your simulation logic here
        });

        exitButton.setOnAction(e -> primaryStage.close());

        // Layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(welcomeLabel, startButton, exitButton);

        // Create scene and show
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}