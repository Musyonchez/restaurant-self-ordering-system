package school.swe2040.restaurant.shared.protocol;

import school.swe2040.restaurant.shared.model.OrderStatus;

import java.util.Objects;

/**
 * Sent by a kitchen display when staff tap the button to advance an order
 * to its next status. The server is the sole authority on whether the
 * requested transition is actually valid.
 */
public final class UpdateStatusMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final OrderStatus newStatus;

    public UpdateStatusMessage(String orderId, OrderStatus newStatus) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus");
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }
}
