package school.swe2040.restaurant.kitchen;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import school.swe2040.restaurant.models.Order;
import school.swe2040.restaurant.models.OrderItem;
import school.swe2040.restaurant.models.OrderStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * One order's card on the kitchen board. Shows the table number, every
 * line item (including customizations and notes, since that's what the
 * kitchen needs to prepare it correctly), how long the order has been
 * sitting, and, unless the order is already SERVED, a single prominent
 * button that advances it to its next status.
 *
 * <p>The aging highlight is computed purely from {@link Order#getPlacedAt()}
 * on the client side: no new server/protocol field is involved, it is
 * strictly a display-layer read of data the card already has.
 */
public final class OrderCard extends VBox {

    private static final long WARNING_AFTER_SECONDS = Duration.ofMinutes(8).getSeconds();
    private static final long CRITICAL_AFTER_SECONDS = Duration.ofMinutes(15).getSeconds();

    public OrderCard(Order order, Consumer<OrderStatus> onAdvance) {
        setSpacing(10);
        setPadding(new Insets(14));
        setPrefWidth(270);
        getStyleClass().addAll("order-card", "status-" + order.getStatus().name().toLowerCase());
        applyAgingStyle(order);

        getChildren().addAll(
                buildHeaderRow(order),
                buildDivider(),
                buildItemsBox(order),
                buildDivider(),
                buildFooterRow(order, onAdvance));
    }

    private HBox buildHeaderRow(Order order) {
        HBox header = new HBox(8);
        header.getStyleClass().add("order-card-header");

        Circle dot = new Circle(6);
        dot.getStyleClass().addAll("order-card-dot", "dot-" + order.getStatus().name().toLowerCase());

        Label tableLabel = new Label("TABLE " + order.getTableNumber());
        tableLabel.getStyleClass().add("order-card-table");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timerLabel = new Label(formatElapsed(order.getPlacedAt()));
        timerLabel.getStyleClass().add("order-card-timer");
        applyTimerStyle(timerLabel, order);

        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(dot, tableLabel, spacer, timerLabel);
        return header;
    }

    private Region buildDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("order-card-divider");
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        return divider;
    }

    private VBox buildItemsBox(Order order) {
        VBox itemsBox = new VBox(8);
        itemsBox.getStyleClass().add("order-card-items");
        for (OrderItem item : order.getItems()) {
            itemsBox.getChildren().add(buildItemRow(item));
        }
        return itemsBox;
    }

    private VBox buildFooterRow(Order order, Consumer<OrderStatus> onAdvance) {
        HBox totalRow = new HBox(10);
        totalRow.getStyleClass().add("order-card-footer");

        Label totalLabel = new Label(String.format("$%.2f", order.getTotal()));
        totalLabel.getStyleClass().add("order-card-total");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        totalRow.getChildren().addAll(totalLabel, spacer);

        VBox footer = new VBox(10);
        footer.getChildren().add(totalRow);

        OrderStatus next = order.getStatus().next();
        if (next != null) {
            Button advanceButton = new Button(buttonLabelFor(next) + " ->");
            advanceButton.getStyleClass().addAll(
                    "advance-button", "touch-button", "advance-to-" + next.name().toLowerCase());
            advanceButton.setMaxWidth(Double.MAX_VALUE);
            advanceButton.setOnAction(event -> onAdvance.accept(next));
            footer.getChildren().add(advanceButton);
        } else {
            Label doneLabel = new Label("Served");
            doneLabel.getStyleClass().add("order-card-done");
            doneLabel.setMaxWidth(Double.MAX_VALUE);
            footer.getChildren().add(doneLabel);
        }

        return footer;
    }

    private VBox buildItemRow(OrderItem item) {
        VBox row = new VBox(2);
        row.getStyleClass().add("order-item-row");

        Label nameLabel = new Label(item.getQuantity() + "x " + item.getMenuItem().getName());
        nameLabel.getStyleClass().add("order-item-name");
        row.getChildren().add(nameLabel);

        String detail = buildDetailText(item);
        if (!detail.isEmpty()) {
            Label detailLabel = new Label(detail);
            detailLabel.getStyleClass().add("order-item-detail");
            row.getChildren().add(detailLabel);
        }

        if (!item.getNote().isBlank()) {
            Label noteLabel = new Label("\"" + item.getNote() + "\"");
            noteLabel.getStyleClass().add("order-item-note");
            row.getChildren().add(noteLabel);
        }

        return row;
    }

    private String buildDetailText(OrderItem item) {
        StringBuilder detail = new StringBuilder();
        if (item.getSize() != null) {
            detail.append(item.getSize());
        }
        if (item.getSpiceLevel() != null) {
            appendSeparated(detail, item.getSpiceLevel().toString());
        }
        if (!item.getSelectedAddOns().isEmpty()) {
            appendSeparated(detail, "+ " + String.join(", ", item.getSelectedAddOns()));
        }
        return detail.toString();
    }

    private void appendSeparated(StringBuilder builder, String text) {
        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(text);
    }

    private void applyAgingStyle(Order order) {
        if (order.getStatus() == OrderStatus.SERVED) {
            return;
        }
        long ageSeconds = Duration.between(order.getPlacedAt(), Instant.now()).getSeconds();
        if (ageSeconds >= CRITICAL_AFTER_SECONDS) {
            getStyleClass().add("age-critical");
        } else if (ageSeconds >= WARNING_AFTER_SECONDS) {
            getStyleClass().add("age-warning");
        }
    }

    private void applyTimerStyle(Label timerLabel, Order order) {
        if (order.getStatus() == OrderStatus.SERVED) {
            return;
        }
        long ageSeconds = Duration.between(order.getPlacedAt(), Instant.now()).getSeconds();
        if (ageSeconds >= CRITICAL_AFTER_SECONDS) {
            timerLabel.getStyleClass().add("timer-critical");
        } else if (ageSeconds >= WARNING_AFTER_SECONDS) {
            timerLabel.getStyleClass().add("timer-warning");
        }
    }

    private static String formatElapsed(Instant placedAt) {
        long totalSeconds = Math.max(0, Duration.between(placedAt, Instant.now()).getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static String buttonLabelFor(OrderStatus next) {
        switch (next) {
            case PREPARING:
                return "Start Preparing";
            case READY:
                return "Mark Ready";
            case SERVED:
                return "Mark Served";
            default:
                return "Advance";
        }
    }
}
