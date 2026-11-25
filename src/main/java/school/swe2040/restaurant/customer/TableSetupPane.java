package school.swe2040.restaurant.customer;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
 *
 * <p>Laid out as two columns side by side rather than one narrow stacked
 * card: this only ever runs on a laptop or monitor-sized window, never an
 * actual phone, so there is plenty of horizontal room and no reason to
 * imitate a phone screen's shape or width.
 */
public final class TableSetupPane extends StackPane {

    public TableSetupPane(IntConsumer onTableNumberChosen) {
        getStyleClass().add("setup-pane");

        Label logo = new Label("🍽");
        logo.getStyleClass().add("setup-logo");

        Label title = new Label("Welcome");
        title.setFont(Font.font("System", FontWeight.BOLD, 48));
        title.getStyleClass().add("setup-title");

        Label subtitle = new Label("Enter this table's number to begin ordering");
        subtitle.getStyleClass().add("setup-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(380);

        TextField tableNumberField = new TextField();
        tableNumberField.setPromptText("Table number");
        tableNumberField.setMaxWidth(Double.MAX_VALUE);
        tableNumberField.setAlignment(Pos.CENTER_LEFT);
        tableNumberField.setFont(Font.font(26));
        tableNumberField.getStyleClass().add("setup-field");
        tableNumberField.setMinHeight(56);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("setup-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button startButton = new Button("Start Ordering");
        startButton.getStyleClass().addAll("primary-button", "touch-button");
        startButton.setMinHeight(68);
        startButton.setMaxWidth(Double.MAX_VALUE);

        Label footerHint = new Label("A staff member can help if you are unsure of your table number.");
        footerHint.getStyleClass().add("setup-footer-hint");
        footerHint.setWrapText(true);
        footerHint.setMaxWidth(360);

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

        VBox introColumn = new VBox(18);
        introColumn.setAlignment(Pos.CENTER_LEFT);
        introColumn.setMaxWidth(420);
        introColumn.getChildren().addAll(logo, title, subtitle);

        Region divider = new Region();
        divider.getStyleClass().add("setup-divider");
        divider.setMinWidth(1);
        divider.setMaxWidth(1);
        divider.setMaxHeight(220);

        VBox formColumn = new VBox(18);
        formColumn.setAlignment(Pos.CENTER_LEFT);
        formColumn.setMaxWidth(340);
        formColumn.getChildren().addAll(tableNumberField, errorLabel, startButton, footerHint);

        HBox layout = new HBox(64, introColumn, divider, formColumn);
        layout.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER);
        getChildren().add(layout);
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
                getClass().getResource("/customer/customer.css").toExternalForm());
        alert.showAndWait();
    }
}
