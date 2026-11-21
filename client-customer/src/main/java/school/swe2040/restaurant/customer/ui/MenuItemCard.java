package school.swe2040.restaurant.customer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import school.swe2040.restaurant.shared.model.MenuItem;
import school.swe2040.restaurant.shared.model.OrderItem;

import java.util.function.Consumer;

/**
 * A large, touch-friendly tile for one menu item on the browsing screen.
 * Shows the item's name, a short description, small "hint chips" for
 * whichever customizations it actually supports (size, spice, add-ons),
 * and its price, so a customer can judge an item at a glance before
 * tapping in. Tapping the add button opens {@link CustomizeDialog}; if
 * the customer confirms, the resulting {@link OrderItem} is handed to the
 * supplied callback so the ordering screen can add it to the cart.
 */
public final class MenuItemCard extends VBox {

    public MenuItemCard(MenuItem menuItem, Consumer<OrderItem> onAddToCart) {
        setSpacing(10);
        setPadding(new Insets(18));
        setPrefWidth(272);
        setMinHeight(210);
        setMaxWidth(272);
        getStyleClass().add("menu-item-card");

        Label categoryBadge = new Label(OrderItemFormatter.categoryIcon(menuItem.getCategory()));
        categoryBadge.getStyleClass().add("menu-item-category-badge");

        Label nameLabel = new Label(menuItem.getName());
        nameLabel.getStyleClass().add("menu-item-name");
        nameLabel.setWrapText(true);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        HBox topRow = new HBox(10, categoryBadge, nameLabel);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label descriptionLabel = new Label(menuItem.getDescription());
        descriptionLabel.getStyleClass().add("menu-item-description");
        descriptionLabel.setWrapText(true);

        HBox hintsRow = new HBox(6);
        hintsRow.getStyleClass().add("menu-item-hints");
        if (menuItem.isSizeCustomizable()) {
            hintsRow.getChildren().add(hintChip("↕", "Size"));
        }
        if (menuItem.isSpiceCustomizable()) {
            hintsRow.getChildren().add(hintChip("🌶", "Spice"));
        }
        if (!menuItem.getAvailableAddOns().isEmpty()) {
            hintsRow.getChildren().add(hintChip("+", "Add-ons"));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label priceLabel = new Label(String.format("$%.2f", menuItem.getBasePrice()));
        priceLabel.getStyleClass().add("menu-item-price");

        Button addButton = new Button("+  Add to Order");
        addButton.getStyleClass().addAll("secondary-button", "touch-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(event -> {
            CustomizeDialog dialog = new CustomizeDialog(menuItem);
            dialog.promptForOrderItem().ifPresent(onAddToCart);
        });

        getChildren().addAll(topRow, descriptionLabel, hintsRow, spacer, priceLabel, addButton);
    }

    private static Label hintChip(String icon, String text) {
        Label chip = new Label(icon + " " + text);
        chip.getStyleClass().add("hint-chip");
        return chip;
    }
}
