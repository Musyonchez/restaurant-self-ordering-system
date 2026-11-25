package school.swe2040.restaurant.protocol;

import school.swe2040.restaurant.models.Order;

import java.util.Objects;

/**
 * Sent by a customer client to submit a new order. The order carried here
 * has no id yet and is always in status {@code PLACED}; the server assigns
 * the id and is the sole authority on order identity.
 */
public final class PlaceOrderMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final Order order;

    public PlaceOrderMessage(Order order) {
        this.order = Objects.requireNonNull(order, "order");
    }

    public Order getOrder() {
        return order;
    }
}
