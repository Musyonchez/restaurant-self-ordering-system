package school.swe2040.restaurant.customer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import school.swe2040.restaurant.shared.model.MenuItem;
import school.swe2040.restaurant.shared.model.OrderItem;
import school.swe2040.restaurant.shared.model.Size;
import school.swe2040.restaurant.shared.model.SpiceLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modal dialog for customizing one menu item before it's added to the cart.
 * Only shows the fields that actually apply to the item (size, spice level,
 * add-ons), quantity and a free-text note are always available. Size and
 * spice options show a friendlier label (and a chili-pepper icon per spice
 * level) instead of the raw enum name.
 */
public final class CustomizeDialog extends Dialog<OrderItem> {

    public CustomizeDialog(MenuItem menuItem) {
        setTitle("Customize " + menuItem.getName());
        setHeaderText(menuItem.getName() + "  ·  $" + String.format("%.2f", menuItem.getBasePrice()));

        VBox content = new VBox(14);
        content.setPadding(new Insets(22));
        content.setPrefWidth(420);
        content.getStyleClass().add("customize-dialog");

        Label description = new Label(menuItem.getDescription());
        description.setWrapText(true);
        content.getChildren().add(description);

        ComboBox<Size> sizeBox = new ComboBox<>();
        if (menuItem.isSizeCustomizable()) {
            sizeBox.getItems().addAll(Size.values());
            sizeBox.setValue(Size.REGULAR);
            sizeBox.setMaxWidth(Double.MAX_VALUE);
            applySizeCellFactory(sizeBox);
            content.getChildren().addAll(fieldLabel("Size"), sizeBox);
        }

        ComboBox<SpiceLevel> spiceBox = new ComboBox<>();
        if (menuItem.isSpiceCustomizable()) {
            spiceBox.getItems().addAll(SpiceLevel.values());
            spiceBox.setValue(SpiceLevel.MEDIUM);
            spiceBox.setMaxWidth(Double.MAX_VALUE);
            applySpiceCellFactory(spiceBox);
            content.getChildren().addAll(fieldLabel("Spice Level"), spiceBox);
        }

        List<CheckBox> addOnChecks = new ArrayList<>();
        if (!menuItem.getAvailableAddOns().isEmpty()) {
            content.getChildren().add(fieldLabel("Add-ons  (+$"
                    + String.format("%.2f", MenuItem.ADD_ON_PRICE) + " each)"));
            VBox addOnsBox = new VBox(8);
            for (String addOn : menuItem.getAvailableAddOns()) {
                CheckBox checkBox = new CheckBox(addOn);
                addOnChecks.add(checkBox);
                addOnsBox.getChildren().add(checkBox);
            }
            content.getChildren().add(addOnsBox);
        }

        Spinner<Integer> quantitySpinner = new Spinner<>();
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        quantitySpinner.setEditable(true);
        quantitySpinner.setMaxWidth(140);
        content.getChildren().addAll(fieldLabel("Quantity"), quantitySpinner);

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("Special instructions (optional)");
        noteArea.setPrefRowCount(2);
        noteArea.setWrapText(true);
        content.getChildren().addAll(fieldLabel("Note"), noteArea);

        getDialogPane().setContent(content);
        getDialogPane().getStylesheets().add(getClass().getResource("/customer.css").toExternalForm());
        getDialogPane().getStyleClass().add("customize-dialog-pane");
        getDialogPane().setPrefWidth(460);

        ButtonType addButtonType = new ButtonType("Add to Order", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType != addButtonType) {
                return null;
            }
            Size size = menuItem.isSizeCustomizable() ? sizeBox.getValue() : null;
            SpiceLevel spiceLevel = menuItem.isSpiceCustomizable() ? spiceBox.getValue() : null;
            List<String> selectedAddOns = new ArrayList<>();
            for (CheckBox checkBox : addOnChecks) {
                if (checkBox.isSelected()) {
                    selectedAddOns.add(checkBox.getText());
                }
            }
            int quantity = quantitySpinner.getValue();
            return new OrderItem(menuItem, quantity, size, spiceLevel, selectedAddOns, noteArea.getText());
        });
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static void applySizeCellFactory(ComboBox<Size> sizeBox) {
        Callback<ListView<Size>, ListCell<Size>> factory = listView -> new ListCell<Size>() {
            @Override
            protected void updateItem(Size item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : OrderItemFormatter.sizeLabel(item));
            }
        };
        sizeBox.setCellFactory(factory);
        sizeBox.setButtonCell(factory.call(null));
    }

    private static void applySpiceCellFactory(ComboBox<SpiceLevel> spiceBox) {
        Callback<ListView<SpiceLevel>, ListCell<SpiceLevel>> factory = listView -> new ListCell<SpiceLevel>() {
            @Override
            protected void updateItem(SpiceLevel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : OrderItemFormatter.spiceIcon(item) + " " + OrderItemFormatter.spiceLabel(item));
            }
        };
        spiceBox.setCellFactory(factory);
        spiceBox.setButtonCell(factory.call(null));
    }

    /** Convenience wrapper over {@link #showAndWait()} for call sites that just want the result. */
    public Optional<OrderItem> promptForOrderItem() {
        return showAndWait();
    }
}
