package school.swe2040.restaurant.customer.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import school.swe2040.restaurant.customer.net.ServerConnection;
import school.swe2040.restaurant.shared.menu.MenuCatalog;
import school.swe2040.restaurant.shared.model.MenuCategory;
import school.swe2040.restaurant.shared.model.MenuItem;
import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.model.OrderItem;
import school.swe2040.restaurant.shared.model.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The main ordering screen: browse the menu, build a cart, place an order,
 * and watch every order this table has placed update its status live. This
 * class is pure UI, it only ever acts through {@link ServerConnection} and
 * only ever reacts to events the connection hands it, never touching the
 * socket itself.
 */
public final class OrderingPane extends BorderPane {

    private final ServerConnection connection;
    private final int tableNumber;
    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();
    private final Map<String, OrderStatusCard> statusCardsByOrderId = new LinkedHashMap<>();
    private final HBox myOrdersBox = new HBox(12);
    private final Label emptyOrdersHint = new Label("Your placed orders will appear here once submitted.");
    private final Label totalLabel = new Label("$0.00");
    private final Label cartCountBadge = new Label("0");

    public OrderingPane(ServerConnection connection, int tableNumber) {
        this.connection = connection;
        this.tableNumber = tableNumber;
        getStyleClass().add("ordering-pane");

        setTop(buildHeader());
        setCenter(buildMainArea());
        setBottom(buildMyOrdersArea());

        connection.setOnStatusChanged(this::onOrderStatusChanged);
        connection.setOnDisconnected(this::onDisconnected);
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        Label brandMark = new Label("🍴");
        brandMark.getStyleClass().add("brand-mark");

        Label tableLabel = new Label("Table " + tableNumber);
        tableLabel.getStyleClass().add("header-title");

        HBox tableBadge = new HBox(10, brandMark, tableLabel);
        tableBadge.setAlignment(Pos.CENTER_LEFT);
        tableBadge.getStyleClass().add("table-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("👆  Tap an item to customize and add it to your order");
        hint.getStyleClass().add("header-hint");

        header.getChildren().addAll(tableBadge, spacer, hint);
        return header;
    }

    private HBox buildMainArea() {
        HBox mainArea = new HBox(16);
        mainArea.setPadding(new Insets(0, 24, 0, 24));

        TabPane menuTabs = buildMenuTabs();
        HBox.setHgrow(menuTabs, Priority.ALWAYS);

        VBox cartPanel = buildCartPanel();
        cartPanel.setPrefWidth(340);
        cartPanel.setMinWidth(300);

        mainArea.getChildren().addAll(menuTabs, cartPanel);
        return mainArea;
    }

    private TabPane buildMenuTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("menu-tabs");
        for (MenuCategory category : MenuCategory.values()) {
            Tab tab = new Tab(categoryLabel(category));
            tab.setContent(buildCategoryGrid(category));
            tabPane.getTabs().add(tab);
        }
        return tabPane;
    }

    private ScrollPane buildCategoryGrid(MenuCategory category) {
        VBox categoryContent = new VBox(16);
        categoryContent.setPadding(new Insets(20, 16, 20, 16));
        categoryContent.getStyleClass().add("category-content");

        Label heading = new Label(categoryLabel(category));
        heading.getStyleClass().add("category-heading");

        FlowPane grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        for (MenuItem item : MenuCatalog.getItemsByCategory(category)) {
            grid.getChildren().add(new MenuItemCard(item, this::addToCart));
        }

        categoryContent.getChildren().addAll(heading, grid);

        ScrollPane scrollPane = new ScrollPane(categoryContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("menu-scroll");
        return scrollPane;
    }

    private VBox buildCartPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("cart-panel");

        Label cartIcon = new Label("🛒");
        cartIcon.getStyleClass().add("cart-icon");
        Label cartTitle = new Label("Your Order");
        cartTitle.getStyleClass().add("cart-title");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        cartCountBadge.getStyleClass().add("cart-count-badge");
        HBox titleRow = new HBox(8, cartIcon, cartTitle, titleSpacer, cartCountBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        ListView<OrderItem> cartList = new ListView<>(cartItems);
        cartList.setCellFactory(listView -> new CartItemCell());
        cartList.getStyleClass().add("cart-list");
        cartList.setFocusTraversable(false);
        Label emptyCartLabel = new Label("Your cart is empty.\nTap a menu item to add it.");
        emptyCartLabel.setWrapText(true);
        cartList.setPlaceholder(emptyCartLabel);
        VBox.setVgrow(cartList, Priority.ALWAYS);

        cartItems.addListener((javafx.collections.ListChangeListener<OrderItem>) change -> {
            updateTotal();
            cartCountBadge.setText(String.valueOf(cartItems.size()));
        });

        Label totalCaption = new Label("Total");
        totalCaption.getStyleClass().add("cart-total-caption");
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        totalLabel.getStyleClass().add("cart-total");
        HBox totalRow = new HBox(totalCaption, totalSpacer, totalLabel);
        totalRow.setAlignment(Pos.CENTER_LEFT);

        Button placeOrderButton = new Button("✓  Place Order");
        placeOrderButton.getStyleClass().addAll("primary-button", "touch-button");
        placeOrderButton.setMaxWidth(Double.MAX_VALUE);
        placeOrderButton.setMinHeight(64);
        placeOrderButton.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(cartItems));
        placeOrderButton.setOnAction(event -> placeOrder());

        VBox footer = new VBox(12, totalRow, placeOrderButton);
        footer.getStyleClass().add("cart-footer");

        panel.getChildren().addAll(titleRow, cartList, footer);
        return panel;
    }

    private VBox buildMyOrdersArea() {
        VBox area = new VBox(10);
        area.setPadding(new Insets(14, 24, 20, 24));
        area.getStyleClass().add("my-orders-area");

        Label title = new Label("📋  My Orders");
        title.getStyleClass().add("my-orders-title");

        emptyOrdersHint.getStyleClass().add("my-orders-empty-hint");
        myOrdersBox.getChildren().add(emptyOrdersHint);
        myOrdersBox.setPadding(new Insets(4));
        myOrdersBox.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scrollPane = new ScrollPane(myOrdersBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(220);
        scrollPane.getStyleClass().add("my-orders-scroll");

        area.getChildren().addAll(title, scrollPane);
        return area;
    }

    private void addToCart(OrderItem item) {
        cartItems.add(item);
    }

    private void updateTotal() {
        double total = cartItems.stream().mapToDouble(OrderItem::getLineTotal).sum();
        totalLabel.setText(String.format("$%.2f", total));
    }

    private void placeOrder() {
        Order order = new Order(UUID.randomUUID().toString(), tableNumber,
                new ArrayList<>(cartItems), OrderStatus.PLACED, Instant.now());
        connection.placeOrder(order);
        cartItems.clear();
    }

    private void onOrderStatusChanged(Order order) {
        if (order.getTableNumber() != tableNumber) {
            return;
        }
        myOrdersBox.getChildren().remove(emptyOrdersHint);
        OrderStatusCard card = statusCardsByOrderId.get(order.getId());
        if (card == null) {
            card = new OrderStatusCard(order);
            statusCardsByOrderId.put(order.getId(), card);
            myOrdersBox.getChildren().add(0, card);
        } else {
            card.updateStatus(order);
        }
    }

    private void onDisconnected() {
        Label lostLabel = new Label("⚠  Connection to the kitchen was lost.");
        lostLabel.getStyleClass().add("connection-lost-label");
        myOrdersBox.getChildren().add(0, lostLabel);
    }

    private static String categoryLabel(MenuCategory category) {
        return OrderItemFormatter.categoryIcon(category) + "  " + categoryName(category);
    }

    private static String categoryName(MenuCategory category) {
        switch (category) {
            case STARTER:
                return "Starters";
            case MAIN:
                return "Mains";
            case DRINK:
                return "Drinks";
            case DESSERT:
                return "Desserts";
            default:
                return category.name();
        }
    }

    /** Cell renderer for one cart line: quantity, name, customization summary, price and a remove button. */
    private final class CartItemCell extends ListCell<OrderItem> {
        @Override
        protected void updateItem(OrderItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label qtyBadge = new Label(item.getQuantity() + "x");
            qtyBadge.getStyleClass().add("cart-item-qty");

            Label nameLabel = new Label(item.getMenuItem().getName());
            nameLabel.getStyleClass().add("cart-item-name");
            nameLabel.setWrapText(true);
            VBox labels = new VBox(2, nameLabel);

            String summary = OrderItemFormatter.customizationSummary(item);
            if (!summary.isEmpty()) {
                Label summaryLabel = new Label(summary);
                summaryLabel.getStyleClass().add("cart-item-summary");
                summaryLabel.setWrapText(true);
                labels.getChildren().add(summaryLabel);
            }
            HBox.setHgrow(labels, Priority.ALWAYS);

            Label priceLabel = new Label(String.format("$%.2f", item.getLineTotal()));
            priceLabel.getStyleClass().add("cart-item-price");

            Button removeButton = new Button("✕");
            removeButton.getStyleClass().add("remove-button");
            Tooltip.install(removeButton, new Tooltip("Remove"));
            removeButton.setOnAction(event -> cartItems.remove(item));

            VBox priceAndRemove = new VBox(6, priceLabel, removeButton);
            priceAndRemove.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(10, qtyBadge, labels, priceAndRemove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("cart-item-row");

            setText(null);
            setGraphic(row);
        }
    }
}
