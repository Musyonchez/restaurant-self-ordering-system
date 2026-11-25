package school.swe2040.restaurant.server;

import org.junit.jupiter.api.Test;
import school.swe2040.restaurant.models.MenuCatalog;
import school.swe2040.restaurant.models.MenuItem;
import school.swe2040.restaurant.models.Order;
import school.swe2040.restaurant.models.OrderItem;
import school.swe2040.restaurant.models.OrderStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the server's order manager: no sockets, no JavaFX, just the
 * state machine and thread-safety guarantees described in
 * 03-architecture-and-protocol.md.
 */
class OrderManagerTest {

    private List<OrderItem> oneBurger() {
        MenuItem burger = MenuCatalog.findById("main-burger").orElseThrow();
        return Collections.singletonList(new OrderItem(burger, 1, null, null, Collections.emptyList(), ""));
    }

    @Test
    void placingAnOrderStartsItAtPlacedStatus() {
        OrderManager manager = new OrderManager();
        Order order = manager.placeOrder(4, oneBurger()).orElseThrow();

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(4, order.getTableNumber());
        assertTrue(manager.findById(order.getId()).isPresent());
    }

    @Test
    void placingAnOrderWithNoItemsIsRejected() {
        OrderManager manager = new OrderManager();

        Optional<Order> result = manager.placeOrder(4, Collections.emptyList());

        assertFalse(result.isPresent());
        assertTrue(manager.getAllOrders().isEmpty());
    }

    @Test
    void placingAnOrderWithANonPositiveTableNumberIsRejected() {
        OrderManager manager = new OrderManager();

        assertFalse(manager.placeOrder(0, oneBurger()).isPresent());
        assertFalse(manager.placeOrder(-1, oneBurger()).isPresent());
        assertTrue(manager.getAllOrders().isEmpty());
    }

    @Test
    void validTransitionsAdvanceOneStepAtATime() {
        OrderManager manager = new OrderManager();
        Order order = manager.placeOrder(1, oneBurger()).orElseThrow();

        Optional<Order> afterPreparing = manager.advanceStatus(order.getId(), OrderStatus.PREPARING);
        assertTrue(afterPreparing.isPresent());
        assertEquals(OrderStatus.PREPARING, afterPreparing.get().getStatus());

        Optional<Order> afterReady = manager.advanceStatus(order.getId(), OrderStatus.READY);
        assertTrue(afterReady.isPresent());
        assertEquals(OrderStatus.READY, afterReady.get().getStatus());

        Optional<Order> afterServed = manager.advanceStatus(order.getId(), OrderStatus.SERVED);
        assertTrue(afterServed.isPresent());
        assertEquals(OrderStatus.SERVED, afterServed.get().getStatus());
    }

    @Test
    void skippingAStepIsRejected() {
        OrderManager manager = new OrderManager();
        Order order = manager.placeOrder(2, oneBurger()).orElseThrow();

        Optional<Order> result = manager.advanceStatus(order.getId(), OrderStatus.READY);

        assertFalse(result.isPresent());
        assertEquals(OrderStatus.PLACED, manager.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void movingBackwardsIsRejected() {
        OrderManager manager = new OrderManager();
        Order order = manager.placeOrder(3, oneBurger()).orElseThrow();
        manager.advanceStatus(order.getId(), OrderStatus.PREPARING);

        Optional<Order> result = manager.advanceStatus(order.getId(), OrderStatus.PLACED);

        assertFalse(result.isPresent());
        assertEquals(OrderStatus.PREPARING, manager.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void unknownOrderIdIsRejected() {
        OrderManager manager = new OrderManager();
        Optional<Order> result = manager.advanceStatus("no-such-order", OrderStatus.PREPARING);
        assertFalse(result.isPresent());
    }

    @Test
    void concurrentAdvanceAttemptsOnTheSameOrderOnlySucceedOnce() throws InterruptedException {
        OrderManager manager = new OrderManager();
        Order order = manager.placeOrder(5, oneBurger()).orElseThrow();

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (manager.advanceStatus(order.getId(), OrderStatus.PREPARING).isPresent()) {
                    successCount.incrementAndGet();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, successCount.get());
        assertEquals(OrderStatus.PREPARING, manager.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void getAllOrdersReturnsEveryStoredOrder() {
        OrderManager manager = new OrderManager();
        manager.placeOrder(1, oneBurger());
        manager.placeOrder(2, oneBurger());

        List<Order> all = manager.getAllOrders();

        assertEquals(2, all.size());
    }
}
