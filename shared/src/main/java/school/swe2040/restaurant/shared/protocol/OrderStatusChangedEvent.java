package school.swe2040.restaurant.shared.protocol;

import school.swe2040.restaurant.shared.model.Order;

import java.util.Objects;

/**
 * Sent by the server whenever an order's status changes: to every kitchen
 * display, so their board refreshes that card, and to the owning table's
 * customer client, so it can update its own status screen.
 */
public final class OrderStatusChangedEvent implements Message {

    private static final long serialVersionUID = 1L;

    private final Order order;

    public OrderStatusChangedEvent(Order order) {
        this.order = Objects.requireNonNull(order, "order");
    }

    public Order getOrder() {
        return order;
    }
}
