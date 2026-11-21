package school.swe2040.restaurant.server.net;

import school.swe2040.restaurant.server.store.OrderStore;
import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.protocol.ClientRole;
import school.swe2040.restaurant.shared.protocol.HandshakeMessage;
import school.swe2040.restaurant.shared.protocol.Message;
import school.swe2040.restaurant.shared.protocol.OrderCreatedEvent;
import school.swe2040.restaurant.shared.protocol.OrderSnapshotMessage;
import school.swe2040.restaurant.shared.protocol.OrderStatusChangedEvent;
import school.swe2040.restaurant.shared.protocol.PlaceOrderMessage;
import school.swe2040.restaurant.shared.protocol.UpdateStatusMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * One client's connection to the server, running on its own thread for the
 * lifetime of the socket. This class is the only place that touches the
 * socket's object streams; it hands off to {@link OrderStore} for all
 * order/state logic and to {@link ConnectionRegistry} for delivering events
 * to other clients, so networking stays separate from business logic.
 */
public final class ClientConnection extends Thread {

    private final Socket socket;
    private final OrderStore orderStore;
    private final ConnectionRegistry registry;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile ClientRole role;
    private volatile int tableNumber = -1;

    public ClientConnection(Socket socket, OrderStore orderStore, ConnectionRegistry registry) {
        super("client-connection-" + socket.getRemoteSocketAddress());
        this.socket = socket;
        this.orderStore = orderStore;
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object first = in.readObject();
            if (!(first instanceof HandshakeMessage)) {
                System.err.println("Expected a handshake message first, got: " + first);
                return;
            }
            handleHandshake((HandshakeMessage) first);

            while (true) {
                Object message = in.readObject();
                handleMessage(message);
            }
        } catch (EOFException | SocketException e) {
            // Client closed the connection; this is a normal disconnect, not an error.
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Connection error for " + describeConnection() + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleHandshake(HandshakeMessage handshake) {
        this.role = handshake.getRole();
        if (role == ClientRole.KITCHEN) {
            registry.registerKitchen(this);
            send(new OrderSnapshotMessage(orderStore.getAllOrders()));
            System.out.println("Kitchen display connected.");
        } else {
            this.tableNumber = handshake.getTableNumber();
            registry.registerCustomer(tableNumber, this);
            System.out.println("Table " + tableNumber + " connected.");
        }
    }

    private void handleMessage(Object message) {
        if (message instanceof PlaceOrderMessage) {
            handlePlaceOrder((PlaceOrderMessage) message);
        } else if (message instanceof UpdateStatusMessage) {
            handleUpdateStatus((UpdateStatusMessage) message);
        } else {
            System.err.println("Unexpected message type from " + describeConnection() + ": " + message);
        }
    }

    private void handlePlaceOrder(PlaceOrderMessage placeOrderMessage) {
        Order submitted = placeOrderMessage.getOrder();
        Order stored = orderStore.placeOrder(submitted.getTableNumber(), submitted.getItems());
        registry.broadcastToKitchens(new OrderCreatedEvent(stored));
        registry.sendToTable(stored.getTableNumber(), new OrderStatusChangedEvent(stored));
    }

    private void handleUpdateStatus(UpdateStatusMessage updateStatusMessage) {
        orderStore.advanceStatus(updateStatusMessage.getOrderId(), updateStatusMessage.getNewStatus())
                .ifPresent(updated -> {
                    registry.broadcastToKitchens(new OrderStatusChangedEvent(updated));
                    registry.sendToTable(updated.getTableNumber(), new OrderStatusChangedEvent(updated));
                });
    }

    /** Sends a message to this client. Safe to call concurrently from any thread. */
    public synchronized void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send to " + describeConnection() + ": " + e.getMessage());
        }
    }

    private void cleanup() {
        if (role == ClientRole.KITCHEN) {
            registry.unregisterKitchen(this);
        } else if (role == ClientRole.CUSTOMER) {
            registry.unregisterCustomer(tableNumber, this);
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Already closing; nothing more to do.
        }
        System.out.println(describeConnection() + " disconnected.");
    }

    private String describeConnection() {
        if (role == ClientRole.KITCHEN) {
            return "Kitchen display";
        }
        if (role == ClientRole.CUSTOMER) {
            return "Table " + tableNumber;
        }
        return "Unidentified client";
    }
}
