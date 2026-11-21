package school.swe2040.restaurant.server.store;

import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.model.OrderItem;
import school.swe2040.restaurant.shared.model.OrderStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server's live, in-memory order queue. Backed by a {@link ConcurrentHashMap}
 * so a table placing a new order and the kitchen advancing another order's
 * status can both happen at the same time, on different threads, without
 * corrupting the board or blocking each other.
 *
 * <p>This class holds only domain logic, no networking and no UI. It is
 * exercised directly by unit tests with no socket or JavaFX involved.
 */
public final class OrderStore {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    /**
     * Creates and stores a new order for {@code tableNumber}, starting in
     * status {@link OrderStatus#PLACED}. The store assigns the order id.
     */
    public Order placeOrder(int tableNumber, List<OrderItem> items) {
        String id = UUID.randomUUID().toString();
        Order order = Order.newOrder(id, tableNumber, items);
        orders.put(id, order);
        return order;
    }

    /**
     * Attempts to move the order identified by {@code orderId} to
     * {@code requestedStatus}. Succeeds only if {@code requestedStatus} is
     * exactly the next step in the lifecycle from the order's current
     * status, matching {@link OrderStatus#canAdvanceTo(OrderStatus)}; this
     * is what stops a request from skipping a step or moving backwards.
     *
     * @return the updated order if the transition was applied, otherwise
     *         empty (unknown order id, or an invalid transition)
     */
    public Optional<Order> advanceStatus(String orderId, OrderStatus requestedStatus) {
        // `applied` is local to this call (each thread's invocation gets its own array), so it
        // records whether THIS call performed the transition, not merely whether the order
        // currently happens to be in the requested status. ConcurrentHashMap.computeIfPresent
        // runs its remapping function atomically per key, so under concurrent calls for the same
        // order exactly one of them observes canAdvanceTo(...) == true and sets applied[0] = true.
        boolean[] applied = {false};
        Order result = orders.computeIfPresent(orderId, (id, existing) -> {
            if (existing.getStatus().canAdvanceTo(requestedStatus)) {
                applied[0] = true;
                return existing.withStatus(requestedStatus);
            }
            return existing;
        });
        if (applied[0]) {
            return Optional.of(result);
        }
        return Optional.empty();
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    /** A snapshot of every order currently held, oldest first. */
    public List<Order> getAllOrders() {
        List<Order> snapshot = new ArrayList<>(orders.values());
        snapshot.sort(Comparator.comparing(Order::getPlacedAt));
        return snapshot;
    }
}
