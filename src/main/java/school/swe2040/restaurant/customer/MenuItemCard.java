package school.swe2040.restaurant.customer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import school.swe2040.restaurant.models.MenuItem;
import school.swe2040.restaurant.models.OrderItem;

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
        // At the app's fixed 1280-wide window, the menu area (after the cart
        // panel and all padding/gaps) works out to roughly 830-845px. A
        // 272px card only ever fits 2 per row there (3 would need 852px),
        // leaving close to a third of that width empty. 250px comfortably
        // fits 3 per row (786px) with margin to spare.
        setPrefWidth(250);
        setMinHeight(210);
        setMaxWidth(250);
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
