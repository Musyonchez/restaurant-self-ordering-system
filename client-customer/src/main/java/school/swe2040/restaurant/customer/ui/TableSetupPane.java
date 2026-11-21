package school.swe2040.restaurant.customer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.IntConsumer;

/**
 * The one-time setup screen shown when the app launches: asks which table
 * this tablet is mounted on, then hands control to the ordering screen.
 * A real deployment would hardcode this per device; this project runs
 * several simulated tablets on one machine, so it is asked once at launch
 * instead (see 02-requirements-and-scope.md, "Table identity").
 */
public final class TableSetupPane extends StackPane {

    public TableSetupPane(IntConsumer onTableNumberChosen) {
        getStyleClass().add("setup-pane");

        Label logo = new Label("🍽");
        logo.getStyleClass().add("setup-logo");

        Label title = new Label("Welcome");
        title.setFont(Font.font("System", FontWeight.BOLD, 40));
        title.getStyleClass().add("setup-title");

        Label subtitle = new Label("Enter this table's number to begin ordering");
        subtitle.getStyleClass().add("setup-subtitle");

        TextField tableNumberField = new TextField();
        tableNumberField.setPromptText("Table number");
        tableNumberField.setMaxWidth(260);
        tableNumberField.setAlignment(Pos.CENTER);
        tableNumberField.setFont(Font.font(26));
        tableNumberField.getStyleClass().add("setup-field");
        tableNumberField.setMinHeight(56);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("setup-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button startButton = new Button("Start Ordering");
        startButton.getStyleClass().addAll("primary-button", "touch-button");
        startButton.setMinSize(260, 68);
        startButton.setMaxWidth(260);

        Label footerHint = new Label("A staff member can help if you are unsure of your table number.");
        footerHint.getStyleClass().add("setup-footer-hint");
        footerHint.setWrapText(true);
        footerHint.setMaxWidth(320);
        footerHint.setAlignment(Pos.CENTER);

        startButton.setOnAction(event -> {
            String text = tableNumberField.getText();
            int tableNumber;
            try {
                tableNumber = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                showFieldError(errorLabel, "Please enter a valid table number.");
                return;
            }
            if (tableNumber <= 0) {
                showFieldError(errorLabel, "Table number must be positive.");
                return;
            }
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            startButton.setDisable(true);
            onTableNumberChosen.accept(tableNumber);
        });

        tableNumberField.setOnAction(event -> startButton.fire());

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(56, 64, 56, 64));
        card.setMaxWidth(460);
        card.getStyleClass().add("setup-card");
        card.getChildren().addAll(logo, title, subtitle, tableNumberField, errorLabel,
                startButton, footerHint);

        setAlignment(Pos.CENTER);
        getChildren().add(card);
    }

    private static void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Re-enables the button and shows an error if connecting to the server failed. */
    public void showConnectionError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Failed");
        alert.setHeaderText("Could not reach the restaurant server");
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/customer.css").toExternalForm());
        alert.showAndWait();
    }
}
