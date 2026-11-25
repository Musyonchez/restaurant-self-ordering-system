package school.swe2040.restaurant.server;

import school.swe2040.restaurant.protocol.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks every connected client so the server can broadcast to all kitchen
 * displays and send targeted updates to the one customer table that owns an
 * order. Backed by thread-safe collections, since connections are added,
 * removed and iterated from different client threads concurrently.
 */
public final class ConnectionRegistry {

    private final List<ClientHandler> kitchenConnections = new CopyOnWriteArrayList<>();
    private final Map<Integer, ClientHandler> customerConnectionsByTable = new ConcurrentHashMap<>();

    public void registerKitchen(ClientHandler connection) {
        kitchenConnections.add(connection);
    }

    public void unregisterKitchen(ClientHandler connection) {
        kitchenConnections.remove(connection);
    }

    /**
     * Registers {@code connection} for {@code tableNumber}, unless another
     * connection already holds that table number. Returns whether
     * registration succeeded: a {@code false} return means {@code
     * connection} was never stored here, so the caller must not treat it
     * as this table's active connection (in particular, must not route
     * events to it or let it silently replace and orphan the real one).
     */
    public boolean registerCustomer(int tableNumber, ClientHandler connection) {
        return customerConnectionsByTable.putIfAbsent(tableNumber, connection) == null;
    }

    public void unregisterCustomer(int tableNumber, ClientHandler connection) {
        customerConnectionsByTable.remove(tableNumber, connection);
    }

    /** Sends {@code message} to every currently connected kitchen display. */
    public void broadcastToKitchens(Message message) {
        for (ClientHandler kitchen : kitchenConnections) {
            kitchen.send(message);
        }
    }

    /** Sends {@code message} to the table's customer client, if it is connected. */
    public void sendToTable(int tableNumber, Message message) {
        ClientHandler connection = customerConnectionsByTable.get(tableNumber);
        if (connection != null) {
            connection.send(message);
        }
    }
}
