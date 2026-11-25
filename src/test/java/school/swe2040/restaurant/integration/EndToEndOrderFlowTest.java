package school.swe2040.restaurant.integration;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import school.swe2040.restaurant.client.ServerConnection;
import school.swe2040.restaurant.models.MenuCatalog;
import school.swe2040.restaurant.models.MenuItem;
import school.swe2040.restaurant.models.Order;
import school.swe2040.restaurant.models.OrderItem;
import school.swe2040.restaurant.models.OrderStatus;
import school.swe2040.restaurant.server.RestaurantServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real server and client networking classes over an actual TCP
 * socket, the same code path a running RestaurantServer plus CustomerApp
 * and KitchenApp use, just without the JavaFX windows on top. This is the
 * one thing the unit tests elsewhere in this suite can't catch: that the
 * customer -> server -> kitchen -> server -> customer round trip still
 * works after any refactor to ServerConnection, ClientHandler,
 * ConnectionRegistry or the protocol classes.
 */
class EndToEndOrderFlowTest {

    // Distinct from NetworkConfig.DEFAULT_PORT so this suite never collides
    // with a RestaurantServer a developer happens to already have running.
    private static final int TEST_PORT = 5099;
    private static final String HOST = "localhost";

    private RestaurantServer server;
    private Thread serverThread;

    @BeforeAll
    static void initJavaFxToolkit() {
        // ServerConnection dispatches every incoming event through
        // Platform.runLater, so the toolkit must be live even though no
        // Stage or Scene is ever created in this test.
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // Fine: another test class in the same JVM already started it.
        }
    }

    @BeforeEach
    void startServer() throws InterruptedException {
        server = new RestaurantServer(TEST_PORT);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();
        // No ready-callback on RestaurantServer; give the accept loop a
        // moment to actually bind before the first client tries to connect.
        Thread.sleep(200);
    }

    @AfterEach
    void stopServer() throws InterruptedException {
        server.stop();
        serverThread.join(2000);
    }

    @Test
    @Timeout(10)
    void placingAnOrderReachesTheKitchenAndStatusUpdatesReachTheCustomer() throws Exception {
        int tableNumber = 7;
        BlockingQueue<Order> kitchenOrderCreated = new LinkedBlockingQueue<>();
        BlockingQueue<Order> customerStatusUpdates = new LinkedBlockingQueue<>();

        ServerConnection kitchen = ServerConnection.connectAsKitchen(HOST, TEST_PORT);
        kitchen.setOnOrderCreated(kitchenOrderCreated::add);

        ServerConnection customer = ServerConnection.connectAsCustomer(HOST, TEST_PORT, tableNumber);
        customer.setOnStatusChanged(customerStatusUpdates::add);

        MenuItem menuItem = MenuCatalog.getAllItems().get(0);
        OrderItem orderItem = new OrderItem(menuItem, 2, null, null, List.of(), "");
        Order placedOrder = new Order(
                java.util.UUID.randomUUID().toString(), tableNumber, List.of(orderItem),
                OrderStatus.PLACED, java.time.Instant.now());

        customer.placeOrder(placedOrder);

        Order seenByKitchen = kitchenOrderCreated.poll(5, TimeUnit.SECONDS);
        assertNotNull(seenByKitchen, "kitchen never received the new order");
        assertEquals(tableNumber, seenByKitchen.getTableNumber());
        assertEquals(OrderStatus.PLACED, seenByKitchen.getStatus());
        assertEquals(1, seenByKitchen.getItems().size());
        assertTrue(!seenByKitchen.getId().isEmpty(), "server must assign a real order id");

        // The server also confirms the newly placed order back to the
        // customer (status still PLACED, but now carrying the server's
        // assigned id) before any status change happens. Drain that first.
        Order placeConfirmation = customerStatusUpdates.poll(5, TimeUnit.SECONDS);
        assertNotNull(placeConfirmation, "customer never received place-order confirmation");
        assertEquals(seenByKitchen.getId(), placeConfirmation.getId());
        assertEquals(OrderStatus.PLACED, placeConfirmation.getStatus());

        kitchen.advanceOrder(seenByKitchen.getId(), OrderStatus.PREPARING);

        Order seenByCustomer = customerStatusUpdates.poll(5, TimeUnit.SECONDS);
        assertNotNull(seenByCustomer, "customer never received the status update");
        assertEquals(seenByKitchen.getId(), seenByCustomer.getId());
        assertEquals(OrderStatus.PREPARING, seenByCustomer.getStatus());

        kitchen.close();
        customer.close();
    }

    @Test
    @Timeout(10)
    void kitchenConnectingLaterStillGetsAFullSnapshotOfExistingOrders() throws Exception {
        int tableNumber = 3;
        ServerConnection customer = ServerConnection.connectAsCustomer(HOST, TEST_PORT, tableNumber);

        MenuItem menuItem = MenuCatalog.getAllItems().get(0);
        OrderItem orderItem = new OrderItem(menuItem, 1, null, null, List.of(), "");
        Order placedOrder = new Order(
                java.util.UUID.randomUUID().toString(), tableNumber, List.of(orderItem),
                OrderStatus.PLACED, java.time.Instant.now());
        customer.placeOrder(placedOrder);
        // Give the server a moment to record the order before a kitchen
        // client connects and asks for the current snapshot.
        Thread.sleep(300);

        BlockingQueue<List<Order>> snapshots = new LinkedBlockingQueue<>();
        ServerConnection lateKitchen = ServerConnection.connectAsKitchen(HOST, TEST_PORT);
        lateKitchen.setOnSnapshot(snapshots::add);

        List<Order> snapshot = snapshots.poll(5, TimeUnit.SECONDS);
        assertNotNull(snapshot, "late-connecting kitchen never received an order snapshot");
        assertEquals(1, snapshot.size());
        assertEquals(tableNumber, snapshot.get(0).getTableNumber());

        lateKitchen.close();
        customer.close();
    }
}
