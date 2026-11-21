package school.swe2040.restaurant.customer.net;

import javafx.application.Platform;
import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.protocol.HandshakeMessage;
import school.swe2040.restaurant.shared.protocol.Message;
import school.swe2040.restaurant.shared.protocol.OrderStatusChangedEvent;
import school.swe2040.restaurant.shared.protocol.PlaceOrderMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * The customer client's only point of contact with the server. This is the
 * networking layer: it owns the socket and the object streams, runs a
 * background thread reading events, and hands every event to the UI layer
 * through a listener invoked via {@link Platform#runLater}, since JavaFX
 * requires UI updates to happen on its own application thread. The UI layer
 * never touches the socket directly, it only calls {@link #placeOrder(Order)}
 * and reads events through the listener.
 */
public final class ServerConnection {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile Consumer<Order> statusListener = order -> {
    };
    private volatile Runnable disconnectListener = () -> {
    };

    public static ServerConnection connect(String host, int port, int tableNumber) throws IOException {
        Socket socket = new Socket(host, port);
        ServerConnection connection = new ServerConnection(socket);
        connection.send(HandshakeMessage.forCustomer(tableNumber));
        connection.startListening();
        return connection;
    }

    private ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /** Called from the UI layer when the customer taps "Place Order". */
    public void placeOrder(Order order) {
        send(new PlaceOrderMessage(order));
    }

    /** Registers the callback the UI layer wants invoked whenever an order's status changes. */
    public void setOnStatusChanged(Consumer<Order> listener) {
        this.statusListener = listener;
    }

    /** Registers the callback the UI layer wants invoked if the connection to the server drops. */
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
        Thread listener = new Thread(this::listenLoop, "customer-server-listener");
        listener.setDaemon(true);
        listener.start();
    }

    private void listenLoop() {
        try {
            while (true) {
                Object message = in.readObject();
                if (message instanceof OrderStatusChangedEvent) {
                    Order order = ((OrderStatusChangedEvent) message).getOrder();
                    Platform.runLater(() -> statusListener.accept(order));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(disconnectListener);
        }
    }
}
