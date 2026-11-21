package school.swe2040.restaurant.kitchen.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import school.swe2040.restaurant.kitchen.net.KitchenConnection;
import school.swe2040.restaurant.kitchen.ui.BoardPane;
import school.swe2040.restaurant.shared.NetworkConfig;

import java.io.IOException;

/**
 * Entry point for the kitchen display app. Connects to the server on
 * launch and shows the full-screen status board once connected.
 */
public final class KitchenApp extends Application {

    private Stage stage;
    private StackPane root;
    private KitchenConnection connection;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.root = new StackPane();

        Label connectingLabel = new Label("Connecting to server...");
        connectingLabel.getStyleClass().add("connecting-label");
        StackPane.setAlignment(connectingLabel, Pos.CENTER);
        root.getChildren().add(connectingLabel);

        Scene scene = new Scene(root, 1400, 850);
        scene.getStylesheets().add(getClass().getResource("/kitchen.css").toExternalForm());

        stage.setTitle("Kitchen Display");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();

        connectToServer();
    }

    private void connectToServer() {
        Thread connectThread = new Thread(() -> {
            try {
                KitchenConnection newConnection = KitchenConnection.connect(
                        NetworkConfig.DEFAULT_HOST, NetworkConfig.DEFAULT_PORT);
                Platform.runLater(() -> onConnected(newConnection));
            } catch (IOException e) {
                Platform.runLater(() -> onConnectFailed(e));
            }
        }, "kitchen-connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void onConnected(KitchenConnection newConnection) {
        this.connection = newConnection;
        BoardPane boardPane = new BoardPane(newConnection);
        root.getChildren().setAll(boardPane);
    }

    private void onConnectFailed(IOException error) {
        Label errorLabel = new Label("Could not connect to the server at "
                + NetworkConfig.DEFAULT_HOST + ":" + NetworkConfig.DEFAULT_PORT
                + ".\nMake sure the server is running.\n\n" + error.getMessage());
        errorLabel.getStyleClass().add("connecting-label");
        root.getChildren().setAll(errorLabel);
    }

    @Override
    public void stop() {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
