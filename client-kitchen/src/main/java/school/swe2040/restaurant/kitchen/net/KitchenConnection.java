package school.swe2040.restaurant.kitchen.net;

import javafx.application.Platform;
import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.model.OrderStatus;
import school.swe2040.restaurant.shared.protocol.HandshakeMessage;
import school.swe2040.restaurant.shared.protocol.Message;
import school.swe2040.restaurant.shared.protocol.OrderCreatedEvent;
import school.swe2040.restaurant.shared.protocol.OrderSnapshotMessage;
import school.swe2040.restaurant.shared.protocol.OrderStatusChangedEvent;
import school.swe2040.restaurant.shared.protocol.UpdateStatusMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

/**
 * The kitchen display's only point of contact with the server. Like the
 * customer client's connection class, this owns the socket and streams and
 * hands every event to the UI layer through listeners invoked via
 * {@link Platform#runLater}. The UI layer only calls
 * {@link #advanceOrder(String, OrderStatus)} and reacts to the listeners,
 * it never touches the socket directly.
 */
public final class KitchenConnection {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    private volatile Consumer<List<Order>> snapshotListener = orders -> {
    };
    private volatile Consumer<Order> orderCreatedListener = order -> {
    };
    private volatile Consumer<Order> orderStatusChangedListener = order -> {
    };
    private volatile Runnable disconnectListener = () -> {
    };

    public static KitchenConnection connect(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        KitchenConnection connection = new KitchenConnection(socket);
        connection.send(HandshakeMessage.forKitchen());
        connection.startListening();
        return connection;
    }

    private KitchenConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /** Called from the UI layer when staff tap the button to advance an order. */
    public void advanceOrder(String orderId, OrderStatus newStatus) {
        send(new UpdateStatusMessage(orderId, newStatus));
    }

    public void setOnSnapshot(Consumer<List<Order>> listener) {
        this.snapshotListener = listener;
    }

    public void setOnOrderCreated(Consumer<Order> listener) {
        this.orderCreatedListener = listener;
    }

    public void setOnOrderStatusChanged(Consumer<Order> listener) {
        this.orderStatusChangedListener = listener;
    }

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

    private void send(Message message) {
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to send message to server: " + e.getMessage());
        }
    }

    private void startListening() {
        Thread listener = new Thread(this::listenLoop, "kitchen-server-listener");
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
                    Platform.runLater(() -> orderStatusChangedListener.accept(order));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(disconnectListener);
        }
    }
}
