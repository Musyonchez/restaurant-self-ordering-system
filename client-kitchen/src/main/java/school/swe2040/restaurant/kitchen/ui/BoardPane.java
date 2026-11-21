package school.swe2040.restaurant.kitchen.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import school.swe2040.restaurant.kitchen.net.KitchenConnection;
import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.model.OrderStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The kitchen status board: one column per {@link OrderStatus}, each
 * listing every order currently in that state, oldest first. This class is
 * pure UI, it only ever acts through {@link KitchenConnection} and only
 * ever reacts to events the connection hands it, never touching the socket
 * itself.
 *
 * <p>A one-second UI tick re-renders the board so each card's elapsed-time
 * readout and aging highlight stay current even when no new server event
 * arrives for a while; it does not touch the network layer or the order
 * data itself, it only recomputes derived display state from the already
 * known {@link Order#getPlacedAt()} timestamp.
 */
public final class BoardPane extends BorderPane {

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final KitchenConnection connection;
    private final Map<String, Order> ordersById = new LinkedHashMap<>();
    private final Map<OrderStatus, VBox> columnContents = new LinkedHashMap<>();
    private final Map<OrderStatus, Label> columnCounts = new LinkedHashMap<>();

    private Label clockLabel;
    private Timeline tickTimeline;

    public BoardPane(KitchenConnection connection) {
        this.connection = connection;
        getStyleClass().add("board-pane");

        setTop(buildHeader());
        setCenter(buildColumns());

        connection.setOnSnapshot(this::onSnapshot);
        connection.setOnOrderCreated(this::onOrderEvent);
        connection.setOnOrderStatusChanged(this::onOrderEvent);
        connection.setOnDisconnected(this::onDisconnected);

        startTicking();
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        Circle logoDot = new Circle(7);
        logoDot.getStyleClass().add("header-logo-dot");

        VBox titleBlock = new VBox(1);
        Label title = new Label("Kitchen Display");
        title.getStyleClass().add("header-title");
        Label subtitle = new Label("Live expo board");
        subtitle.getStyleClass().add("header-subtitle");
        titleBlock.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        clockLabel = new Label(CLOCK_FORMAT.format(Instant.now()));
        clockLabel.getStyleClass().add("header-clock");

        Region hintDivider = new Region();
        hintDivider.getStyleClass().add("header-divider");
        hintDivider.setPrefWidth(1);
        hintDivider.setPrefHeight(28);

        Label hint = new Label("Tap an order's button to advance it to its next status");
        hint.getStyleClass().add("header-hint");

        header.getChildren().addAll(logoDot, titleBlock, spacer, clockLabel, hintDivider, hint);
        return header;
    }

    private HBox buildColumns() {
        HBox columns = new HBox(16);
        columns.setPadding(new Insets(16, 24, 24, 24));
        for (OrderStatus status : OrderStatus.values()) {
            VBox column = buildColumn(status);
            HBox.setHgrow(column, Priority.ALWAYS);
            columns.getChildren().add(column);
        }
        return columns;
    }

    private VBox buildColumn(OrderStatus status) {
        VBox column = new VBox();
        column.getStyleClass().addAll("board-column", "column-" + status.name().toLowerCase());

        HBox columnHeader = buildColumnHeader(status);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        columnContents.put(status, content);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("board-column-scroll");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        column.getChildren().addAll(columnHeader, scrollPane);
        return column;
    }

    private HBox buildColumnHeader(OrderStatus status) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("board-column-header");

        Circle dot = new Circle(5);
        dot.getStyleClass().addAll("column-status-dot", "dot-" + status.name().toLowerCase());

        Label columnTitle = new Label(status.displayLabel().toUpperCase());
        columnTitle.getStyleClass().add("board-column-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label("0");
        countLabel.getStyleClass().add("board-column-count");
        columnCounts.put(status, countLabel);

        header.getChildren().addAll(dot, columnTitle, spacer, countLabel);
        return header;
    }

    private void startTicking() {
        tickTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> onTick()));
        tickTimeline.setCycleCount(Animation.INDEFINITE);
        tickTimeline.play();
    }

    private void onTick() {
        if (clockLabel != null) {
            clockLabel.setText(CLOCK_FORMAT.format(Instant.now()));
        }
        renderAllColumns();
    }

    private void onSnapshot(List<Order> orders) {
        ordersById.clear();
        for (Order order : orders) {
            ordersById.put(order.getId(), order);
        }
        renderAllColumns();
    }

    private void onOrderEvent(Order order) {
        ordersById.put(order.getId(), order);
        renderAllColumns();
    }

    private void onDisconnected() {
        if (tickTimeline != null) {
            tickTimeline.stop();
        }
        Label lostLabel = new Label("Connection to the server was lost.");
        lostLabel.getStyleClass().add("connection-lost-label");
        setTop(lostLabel);
    }

    private void renderAllColumns() {
        for (OrderStatus status : OrderStatus.values()) {
            renderColumn(status);
        }
    }

    private void renderColumn(OrderStatus status) {
        VBox content = columnContents.get(status);
        content.getChildren().clear();

        List<Order> ordersInStatus = ordersById.values().stream()
                .filter(order -> order.getStatus() == status)
                .sorted(Comparator.comparing(Order::getPlacedAt, Comparator.naturalOrder()))
                .collect(Collectors.toList());

        columnCounts.get(status).setText(String.valueOf(ordersInStatus.size()));

        if (ordersInStatus.isEmpty()) {
            Label emptyLabel = new Label("No orders");
            emptyLabel.getStyleClass().add("board-column-empty");
            content.getChildren().add(emptyLabel);
            return;
        }

        for (Order order : ordersInStatus) {
            content.getChildren().add(new OrderCard(order,
                    nextStatus -> connection.advanceOrder(order.getId(), nextStatus)));
        }
    }
}
