package school.swe2040.restaurant.protocol;

import school.swe2040.restaurant.models.Order;

import java.util.Objects;

/**
 * Sent by the server to every connected kitchen display when a new order
 * has been placed and should appear on the board.
 */
public final class OrderCreatedEvent implements Message {

    private static final long serialVersionUID = 1L;

    private final Order order;

    public OrderCreatedEvent(Order order) {
        this.order = Objects.requireNonNull(order, "order");
    }

    public Order getOrder() {
        return order;
    }
}
