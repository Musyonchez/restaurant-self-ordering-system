package school.swe2040.restaurant.protocol;

import school.swe2040.restaurant.models.Order;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Sent by the server to a kitchen display right after its handshake, so the
 * board shows every order currently in progress instead of staying empty
 * until the next event arrives.
 */
public final class OrderSnapshotMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final List<Order> orders;

    public OrderSnapshotMessage(List<Order> orders) {
        this.orders = Collections.unmodifiableList(Objects.requireNonNull(orders, "orders"));
    }

    public List<Order> getOrders() {
        return orders;
    }
}
