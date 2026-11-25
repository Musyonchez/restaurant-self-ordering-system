package school.swe2040.restaurant.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the order lifecycle state machine defined on OrderStatus. */
class OrderStatusTest {

    @Test
    void lifecycleMovesForwardOneStepAtATime() {
        assertEquals(OrderStatus.PREPARING, OrderStatus.PLACED.next());
        assertEquals(OrderStatus.READY, OrderStatus.PREPARING.next());
        assertEquals(OrderStatus.SERVED, OrderStatus.READY.next());
        assertNull(OrderStatus.SERVED.next());
    }

    @Test
    void servedIsTerminal() {
        assertFalse(OrderStatus.SERVED.canAdvance());
        assertTrue(OrderStatus.PLACED.canAdvance());
        assertTrue(OrderStatus.PREPARING.canAdvance());
        assertTrue(OrderStatus.READY.canAdvance());
    }

    @Test
    void cannotSkipAStep() {
        assertFalse(OrderStatus.PLACED.canAdvanceTo(OrderStatus.READY));
        assertFalse(OrderStatus.PLACED.canAdvanceTo(OrderStatus.SERVED));
        assertTrue(OrderStatus.PLACED.canAdvanceTo(OrderStatus.PREPARING));
    }

    @Test
    void cannotMoveBackwards() {
        assertFalse(OrderStatus.PREPARING.canAdvanceTo(OrderStatus.PLACED));
        assertFalse(OrderStatus.READY.canAdvanceTo(OrderStatus.PREPARING));
        assertFalse(OrderStatus.SERVED.canAdvanceTo(OrderStatus.READY));
    }
}
