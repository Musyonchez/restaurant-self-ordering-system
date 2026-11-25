package school.swe2040.restaurant.client;

import javafx.application.Platform;
import school.swe2040.restaurant.models.Order;
import school.swe2040.restaurant.models.OrderStatus;
import school.swe2040.restaurant.protocol.HandshakeMessage;
import school.swe2040.restaurant.protocol.Message;
import school.swe2040.restaurant.protocol.OrderCreatedEvent;
import school.swe2040.restaurant.protocol.OrderSnapshotMessage;
import school.swe2040.restaurant.protocol.OrderStatusChangedEvent;
import school.swe2040.restaurant.protocol.PlaceOrderMessage;
import school.swe2040.restaurant.protocol.UpdateStatusMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Either client's only point of contact with the server. This is the
 * networking layer: it owns the socket and the object streams, runs a
 * background thread reading events, and hands every event to the UI layer
 * through a listener invoked via {@link Platform#runLater}, since JavaFX
 * requires UI updates to happen on its own application thread. The UI layer
 * never touches the socket directly.
 *
 * <p>The customer app and the kitchen app previously had their own,
 * near-identical connection classes (same socket/stream setup, same
 * send/listen-loop shape), differing only in which handshake they sent and
 * which event types they cared about. They are merged into this one class:
 * {@link #connectAsCustomer(String, int, int)} and
 * {@link #connectAsKitchen(String, int)} pick the handshake, and the UI
 * layer only registers the listeners relevant to its role (a customer
 * screen only ever calls {@link #setOnStatusChanged}; the kitchen board
 * also uses {@link #setOnSnapshot} and {@link #setOnOrderCreated}).
 */
public final class ServerConnection {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    private volatile Consumer<List<Order>> snapshotListener = orders -> {
    };
    private volatile Consumer<Order> orderCreatedListener = order -> {
    };
    private volatile Consumer<Order> statusChangedListener = order -> {
    };
    private volatile Runnable disconnectListener = () -> {
    };
    // A failed send() and the read-side listenLoop() can both independently
    // observe the same broken connection; this guarantees the UI is only
    // ever told about it once, instead of the error label piling up.
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(false);

    /** Connects as a customer tablet for the given table number. */
    public static ServerConnection connectAsCustomer(String host, int port, int tableNumber) throws IOException {
        Socket socket = new Socket(host, port);
        ServerConnection connection = new ServerConnection(socket);
        connection.send(HandshakeMessage.forCustomer(tableNumber));
        connection.startListening();
        return connection;
    }

    /** Connects as the kitchen display. */
    public static ServerConnection connectAsKitchen(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        ServerConnection connection = new ServerConnection(socket);
        connection.send(HandshakeMessage.forKitchen());
        connection.startListening();
        return connection;
    }

    private ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Called from the customer UI when the customer taps "Place Order".
     * Returns whether the order actually reached the server: the caller
     * must not clear the cart or otherwise assume success on a false
     * return, since the order was never sent.
     */
    public boolean placeOrder(Order order) {
        return send(new PlaceOrderMessage(order));
    }

    /**
     * Called from the kitchen UI when staff tap the button to advance an
     * order. Returns whether the update actually reached the server.
     */
    public boolean advanceOrder(String orderId, OrderStatus newStatus) {
        return send(new UpdateStatusMessage(orderId, newStatus));
    }

    /** Registers the callback invoked with the full order list right after connecting (kitchen only). */
    public void setOnSnapshot(Consumer<List<Order>> listener) {
        this.snapshotListener = listener;
    }

    /** Registers the callback invoked whenever a new order is placed (kitchen only). */
    public void setOnOrderCreated(Consumer<Order> listener) {
        this.orderCreatedListener = listener;
    }

    /** Registers the callback invoked whenever an order's status changes. */
    public void setOnStatusChanged(Consumer<Order> listener) {
        this.statusChangedListener = listener;
    }

    /** Registers the callback invoked if the connection to the server drops. */
    public void setOnDisconnected(Runnable listener) {
        this.disconnectListener = listener;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Already shutting down; nothing more to do.
        }
    }

    /**
     * Returns whether the message was actually written to the socket. A
     * failed send means the connection is already broken, so this also
     * closes the socket and notifies the disconnect listener itself,
     * rather than relying on the read-side listen loop to eventually
     * notice: that loop only fails on its next incoming read, which may
     * never come, leaving the UI never told the connection died.
     */
    private boolean send(Message message) {
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to send message to server: " + e.getMessage());
            close();
            notifyDisconnected();
            return false;
        }
    }

    private void notifyDisconnected() {
        if (disconnectNotified.compareAndSet(false, true)) {
            Platform.runLater(disconnectListener);
        }
    }

    private void startListening() {
        Thread listener = new Thread(this::listenLoop, "server-listener");
        listener.setDaemon(true);
        listener.start();
    }

    private void listenLoop() {
        try {
            while (true) {
                Object message = in.readObject();
                if (message instanceof OrderSnapshotMessage) {
                    List<Order> orders = ((OrderSnapshotMessage) message).getOrders();
                    Platform.runLater(() -> snapshotListener.accept(orders));
                } else if (message instanceof OrderCreatedEvent) {
                    Order order = ((OrderCreatedEvent) message).getOrder();
                    Platform.runLater(() -> orderCreatedListener.accept(order));
                } else if (message instanceof OrderStatusChangedEvent) {
                    Order order = ((OrderStatusChangedEvent) message).getOrder();
                    Platform.runLater(() -> statusChangedListener.accept(order));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            notifyDisconnected();
        }
    }
}
