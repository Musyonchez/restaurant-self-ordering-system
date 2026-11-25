package school.swe2040.restaurant.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import school.swe2040.restaurant.NetworkConfig;
import school.swe2040.restaurant.customer.OrderingPane;
import school.swe2040.restaurant.customer.TableSetupPane;

import java.io.IOException;

/**
 * Entry point for the customer tablet app. Shows the table-number setup
 * screen first, then connects to the server and switches to the ordering
 * screen for that table. Starts windowed (press F11 to simulate the
 * permanently mounted, full-screen tablet the brief describes).
 */
public final class CustomerApp extends Application {

    private Stage stage;
    private StackPane root;
    private ServerConnection connection;

    @Override
    public void start(Stage primaryStage) {
        loadBundledFonts();

        this.stage = primaryStage;
        this.root = new StackPane();

        showTableSetup();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/customer/customer.css").toExternalForm());
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        stage.setTitle("Table Ordering");
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    /**
     * Registers the bundled Inter font files with JavaFX's font registry
     * up front. customer.css also declares them via {@code @font-face},
     * but a dialog or alert opens in its own Scene and this guarantees the
     * family names are already known application-wide the first time any
     * scene's CSS asks for them, regardless of CSS parse order. All five
     * files are local resources under src/main/resources/customer/fonts,
     * loaded from the classpath, not over the network.
     */
    private void loadBundledFonts() {
        String[] fontFiles = {
                "/customer/fonts/Inter-Regular.ttf",
                "/customer/fonts/Inter-Medium.ttf",
                "/customer/fonts/Inter-SemiBold.ttf",
                "/customer/fonts/Inter-Bold.ttf",
                "/customer/fonts/Inter-ExtraBold.ttf"
        };
        for (String fontFile : fontFiles) {
            Font.loadFont(getClass().getResourceAsStream(fontFile), 12);
        }
    }

    private void showTableSetup() {
        TableSetupPane setupPane = new TableSetupPane(this::onTableNumberChosen);
        root.getChildren().setAll(setupPane);
    }

    private void onTableNumberChosen(int tableNumber) {
        Thread connectThread = new Thread(() -> {
            try {
                ServerConnection newConnection = ServerConnection.connectAsCustomer(
                        NetworkConfig.DEFAULT_HOST, NetworkConfig.DEFAULT_PORT, tableNumber);
                javafx.application.Platform.runLater(() -> onConnected(newConnection, tableNumber));
            } catch (IOException e) {
                javafx.application.Platform.runLater(() -> onConnectFailed(e, tableNumber));
            }
        }, "customer-connect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void onConnected(ServerConnection newConnection, int tableNumber) {
        this.connection = newConnection;
        OrderingPane orderingPane = new OrderingPane(newConnection, tableNumber);
        root.getChildren().setAll(orderingPane);
    }

    private void onConnectFailed(IOException error, int tableNumber) {
        TableSetupPane setupPane = new TableSetupPane(this::onTableNumberChosen);
        root.getChildren().setAll(setupPane);
        setupPane.showConnectionError("Could not connect to the server at "
                + NetworkConfig.DEFAULT_HOST + ":" + NetworkConfig.DEFAULT_PORT
                + ".\n\nMake sure the server is running, then try again.\n\n" + error.getMessage());
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
