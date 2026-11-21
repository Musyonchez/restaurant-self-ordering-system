package school.swe2040.restaurant.shared.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A customer's order: which table placed it, what's in it, and where it
 * currently sits in the lifecycle defined by {@link OrderStatus}.
 *
 * <p>Order is immutable. Advancing its status does not mutate an existing
 * instance, it produces a new one via {@link #withStatus(OrderStatus)}. This
 * keeps the server's order store simple under concurrent access: a status
 * change is just one map entry being replaced, never a field being written
 * while another thread might be reading it.
 */
public final class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final int tableNumber;
    private final List<OrderItem> items;
    private final OrderStatus status;
    private final Instant placedAt;

    public Order(String id, int tableNumber, List<OrderItem> items, OrderStatus status, Instant placedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.tableNumber = tableNumber;
        this.items = Collections.unmodifiableList(Objects.requireNonNull(items, "items"));
        this.status = Objects.requireNonNull(status, "status");
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
    }

    /** Convenience factory for a brand-new order: always starts at {@code PLACED}. */
    public static Order newOrder(String id, int tableNumber, List<OrderItem> items) {
        return new Order(id, tableNumber, items, OrderStatus.PLACED, Instant.now());
    }

    /** Returns a copy of this order with its status changed to {@code newStatus}. */
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, tableNumber, items, newStatus, placedAt);
    }

    public String getId() {
        return id;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public double getTotal() {
        double total = 0.0;
        for (OrderItem item : items) {
            total += item.getLineTotal();
        }
        return total;
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", table=" + tableNumber + ", status=" + status + '}';
    }
}
