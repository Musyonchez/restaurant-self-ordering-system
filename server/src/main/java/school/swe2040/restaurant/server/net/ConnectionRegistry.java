package school.swe2040.restaurant.server.net;

import school.swe2040.restaurant.shared.protocol.Message;

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

    private final List<ClientConnection> kitchenConnections = new CopyOnWriteArrayList<>();
    private final Map<Integer, ClientConnection> customerConnectionsByTable = new ConcurrentHashMap<>();

    public void registerKitchen(ClientConnection connection) {
        kitchenConnections.add(connection);
    }

    public void unregisterKitchen(ClientConnection connection) {
        kitchenConnections.remove(connection);
    }

    public void registerCustomer(int tableNumber, ClientConnection connection) {
        customerConnectionsByTable.put(tableNumber, connection);
    }

    public void unregisterCustomer(int tableNumber, ClientConnection connection) {
        customerConnectionsByTable.remove(tableNumber, connection);
    }

    /** Sends {@code message} to every currently connected kitchen display. */
    public void broadcastToKitchens(Message message) {
        for (ClientConnection kitchen : kitchenConnections) {
            kitchen.send(message);
        }
    }

    /** Sends {@code message} to the table's customer client, if it is connected. */
    public void sendToTable(int tableNumber, Message message) {
        ClientConnection connection = customerConnectionsByTable.get(tableNumber);
        if (connection != null) {
            connection.send(message);
        }
    }
}
