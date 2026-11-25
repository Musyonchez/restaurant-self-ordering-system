package school.swe2040.restaurant.models;

import java.io.Serializable;

/**
 * The four states an order moves through, in exactly one direction:
 * {@code PLACED -> PREPARING -> READY -> SERVED}. There is no reject,
 * cancel or skip step, matching the scope decision in
 * 02-requirements-and-scope.md.
 */
public enum OrderStatus implements Serializable {
    PLACED,
    PREPARING,
    READY,
    SERVED;

    /**
     * The status that follows this one in the lifecycle, or {@code null}
     * if this is the terminal state.
     */
    public OrderStatus next() {
        switch (this) {
            case PLACED:
                return PREPARING;
            case PREPARING:
                return READY;
            case READY:
                return SERVED;
            case SERVED:
            default:
                return null;
        }
    }

    /** Whether this status has a next step to advance to. */
    public boolean canAdvance() {
        return next() != null;
    }

    /**
     * Whether {@code target} is the single valid next step from this
     * status. Used by the order store to reject any attempt to skip a
     * step or move backwards.
     */
    public boolean canAdvanceTo(OrderStatus target) {
        return next() == target;
    }

    public String displayLabel() {
        switch (this) {
            case PLACED:
                return "Order Received";
            case PREPARING:
                return "Preparing";
            case READY:
                return "Ready";
            case SERVED:
                return "Served";
            default:
                return name();
        }
    }
}
