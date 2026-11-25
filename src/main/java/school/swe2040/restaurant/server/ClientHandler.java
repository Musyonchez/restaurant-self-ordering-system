package school.swe2040.restaurant.server;

import school.swe2040.restaurant.models.Order;
import school.swe2040.restaurant.protocol.ClientRole;
import school.swe2040.restaurant.protocol.HandshakeMessage;
import school.swe2040.restaurant.protocol.Message;
import school.swe2040.restaurant.protocol.OrderCreatedEvent;
import school.swe2040.restaurant.protocol.OrderSnapshotMessage;
import school.swe2040.restaurant.protocol.OrderStatusChangedEvent;
import school.swe2040.restaurant.protocol.PlaceOrderMessage;
import school.swe2040.restaurant.protocol.UpdateStatusMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;

/**
 * One client's connection to the server, running on its own thread for the
 * lifetime of the socket. This class is the only place that touches the
 * socket's object streams; it hands off to {@link OrderManager} for all
 * order/state logic and to {@link ConnectionRegistry} for delivering events
 * to other clients, so networking stays separate from business logic.
 */
public final class ClientHandler extends Thread {

    private final Socket socket;
    private final OrderManager orderManager;
    private final ConnectionRegistry registry;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile ClientRole role;
    private volatile int tableNumber = -1;

    public ClientHandler(Socket socket, OrderManager orderManager, ConnectionRegistry registry) {
        super("client-handler-" + socket.getRemoteSocketAddress());
        this.socket = socket;
        this.orderManager = orderManager;
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
            if (!handleHandshake((HandshakeMessage) first)) {
                // Rejected (invalid table number, or a duplicate connection
                // for a table that's already taken): fall through to the
                // finally block below, which closes the socket, rather
                // than entering the message loop on a connection that was
                // never actually registered with anything.
                return;
            }

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

    /**
     * Returns whether this connection was accepted. A {@code false} return
     * means it was never registered anywhere (invalid table number, or a
     * duplicate connection for a table that already has one); the caller
     * must close the connection immediately rather than entering the
     * normal message loop on it.
     */
    private boolean handleHandshake(HandshakeMessage handshake) {
        this.role = handshake.getRole();
        if (role == ClientRole.KITCHEN) {
            registry.registerKitchen(this);
            send(new OrderSnapshotMessage(orderManager.getAllOrders()));
            System.out.println("Kitchen display connected.");
            return true;
        }

        this.tableNumber = handshake.getTableNumber();
        if (tableNumber <= 0) {
            System.err.println("Rejecting customer handshake with invalid table number: " + tableNumber);
            return false;
        }
        // The customer UI already stops a second device from claiming a
        // table number that's visibly in use, but the server cannot assume
        // every client is well-behaved: without this check, a second
        // connection for the same table would silently replace the first
        // in the registry, so the original device would stop receiving
        // order updates with no indication anything went wrong for either
        // side.
        if (!registry.registerCustomer(tableNumber, this)) {
            System.err.println("Rejecting duplicate connection for table " + tableNumber
                    + ": that table is already connected.");
            return false;
        }
        System.out.println("Table " + tableNumber + " connected.");
        return true;
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
        Optional<Order> stored = orderManager.placeOrder(submitted.getTableNumber(), submitted.getItems());
        if (!stored.isPresent()) {
            // The customer UI never sends this (an empty cart disables its
            // Place Order button, and it never accepts a non-positive table
            // number), so this only fires for a client not playing by the
            // rules. There is no rejection message in the protocol, and
            // adding one for a case the real UI cannot trigger is not worth
            // the scope here; logging and dropping the message is enough.
            System.err.println("Rejected invalid order from " + describeConnection() + ": table="
                    + submitted.getTableNumber() + ", items=" + submitted.getItems().size());
            return;
        }
        registry.broadcastToKitchens(new OrderCreatedEvent(stored.get()));
        registry.sendToTable(stored.get().getTableNumber(), new OrderStatusChangedEvent(stored.get()));
    }

    private void handleUpdateStatus(UpdateStatusMessage updateStatusMessage) {
        orderManager.advanceStatus(updateStatusMessage.getOrderId(), updateStatusMessage.getNewStatus())
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
