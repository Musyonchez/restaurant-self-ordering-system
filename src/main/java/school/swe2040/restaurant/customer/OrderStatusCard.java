package school.swe2040.restaurant.customer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import school.swe2040.restaurant.models.Order;
import school.swe2040.restaurant.models.OrderItem;
import school.swe2040.restaurant.models.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * A small live-updating card showing one of this table's orders and its
 * current progress through PLACED -> PREPARING -> READY -> SERVED as a
 * real step-by-step progress stepper (connected circles, not just colored
 * text). The ordering screen creates one of these per order id and calls
 * {@link #updateStatus(Order)} whenever a new status event arrives for it.
 */
public final class OrderStatusCard extends VBox {

    private final Label statusBadge;
    private final List<Label> stepCircles = new ArrayList<>();
    private final List<Label> stepLabels = new ArrayList<>();
    private final List<Region> connectors = new ArrayList<>();

    public OrderStatusCard(Order order) {
        setSpacing(10);
        setPadding(new Insets(16));
        setPrefWidth(238);
        getStyleClass().add("order-status-card");

        String shortId = order.getId().length() > 6
                ? order.getId().substring(0, 6).toUpperCase()
                : order.getId().toUpperCase();
        Label idLabel = new Label("Order #" + shortId);
        idLabel.getStyleClass().add("order-status-id");

        statusBadge = new Label();
        statusBadge.getStyleClass().add("order-status-badge");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(8, idLabel, headerSpacer, statusBadge);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox itemsBox = new VBox(3);
        itemsBox.getStyleClass().add("order-status-items");
        for (OrderItem item : order.getItems()) {
            Label itemLine = new Label(item.getQuantity() + "x " + item.getMenuItem().getName());
            itemLine.getStyleClass().add("order-status-item-line");
            itemsBox.getChildren().add(itemLine);

            String summary = OrderItemFormatter.customizationSummary(item);
            if (!summary.isEmpty()) {
                Label summaryLine = new Label(summary);
                summaryLine.getStyleClass().add("order-status-item-summary");
                summaryLine.setWrapText(true);
                itemsBox.getChildren().add(summaryLine);
            }
        }

        Label totalLabel = new Label(String.format("Total: $%.2f", order.getTotal()));
        totalLabel.getStyleClass().add("order-status-total");

        HBox stepperRow = buildStepperRow();

        getChildren().addAll(headerRow, itemsBox, totalLabel, stepperRow);
        updateStatus(order);
    }

    private HBox buildStepperRow() {
        HBox row = new HBox();
        row.getStyleClass().add("stepper-row");
        row.setAlignment(Pos.TOP_CENTER);

        OrderStatus[] steps = OrderStatus.values();
        for (int i = 0; i < steps.length; i++) {
            Label circle = new Label(String.valueOf(i + 1));
            circle.getStyleClass().add("step-circle");

            Label label = new Label(shortStepLabel(steps[i]));
            label.getStyleClass().add("step-label");

            VBox stepCell = new VBox(4, circle, label);
            stepCell.setAlignment(Pos.TOP_CENTER);

            stepCircles.add(circle);
            stepLabels.add(label);
            row.getChildren().add(stepCell);

            if (i < steps.length - 1) {
                Region connector = new Region();
                connector.getStyleClass().add("step-connector");
                HBox.setHgrow(connector, Priority.ALWAYS);
                HBox.setMargin(connector, new Insets(13, -6, 0, -6));
                connectors.add(connector);
                row.getChildren().add(connector);
            }
        }
        return row;
    }

    /** Refreshes this card to reflect {@code order}'s current status. */
    public void updateStatus(Order order) {
        getStyleClass().removeIf(styleClass -> styleClass.startsWith("status-"));
        getStyleClass().add("status-" + order.getStatus().name().toLowerCase());

        statusBadge.getStyleClass().removeIf(styleClass -> styleClass.startsWith("badge-"));
        statusBadge.getStyleClass().add("badge-" + order.getStatus().name().toLowerCase());
        statusBadge.setText(statusIcon(order.getStatus()) + " " + order.getStatus().displayLabel());

        int current = order.getStatus().ordinal();
        for (int i = 0; i < stepCircles.size(); i++) {
            Label circle = stepCircles.get(i);
            Label label = stepLabels.get(i);
            circle.getStyleClass().removeAll("step-circle-done", "step-circle-current", "step-circle-upcoming");
            label.getStyleClass().removeAll("step-label-done", "step-label-current", "step-label-upcoming");
            if (i < current) {
                circle.setText("✓");
                circle.getStyleClass().add("step-circle-done");
                label.getStyleClass().add("step-label-done");
            } else if (i == current) {
                circle.setText(String.valueOf(i + 1));
                circle.getStyleClass().add("step-circle-current");
                label.getStyleClass().add("step-label-current");
            } else {
                circle.setText(String.valueOf(i + 1));
                circle.getStyleClass().add("step-circle-upcoming");
                label.getStyleClass().add("step-label-upcoming");
            }
        }
        for (int i = 0; i < connectors.size(); i++) {
            Region connector = connectors.get(i);
            connector.getStyleClass().removeAll("step-connector-done", "step-connector-upcoming");
            connector.getStyleClass().add(i < current ? "step-connector-done" : "step-connector-upcoming");
        }
    }

    private static String shortStepLabel(OrderStatus status) {
        switch (status) {
            case PLACED:
                return "Placed";
            case PREPARING:
                return "Preparing";
            case READY:
                return "Ready";
            case SERVED:
                return "Served";
            default:
                return status.name();
        }
    }

    private static String statusIcon(OrderStatus status) {
        switch (status) {
            case PLACED:
                return "📥";
            case PREPARING:
                return "🍳";
            case READY:
                return "🔔";
            case SERVED:
                return "✅";
            default:
                return "";
        }
    }
}
