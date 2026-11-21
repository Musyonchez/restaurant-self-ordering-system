package school.swe2040.restaurant.server;

import org.junit.jupiter.api.Test;
import school.swe2040.restaurant.server.store.OrderStore;
import school.swe2040.restaurant.shared.menu.MenuCatalog;
import school.swe2040.restaurant.shared.model.MenuItem;
import school.swe2040.restaurant.shared.model.Order;
import school.swe2040.restaurant.shared.model.OrderItem;
import school.swe2040.restaurant.shared.model.OrderStatus;

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
 * Unit tests for the server's order store: no sockets, no JavaFX, just the
 * state machine and thread-safety guarantees described in
 * 03-architecture-and-protocol.md.
 */
class OrderStoreTest {

    private List<OrderItem> oneBurger() {
        MenuItem burger = MenuCatalog.findById("main-burger").orElseThrow();
        return Collections.singletonList(new OrderItem(burger, 1, null, null, Collections.emptyList(), ""));
    }

    @Test
    void placingAnOrderStartsItAtPlacedStatus() {
        OrderStore store = new OrderStore();
        Order order = store.placeOrder(4, oneBurger());

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(4, order.getTableNumber());
        assertTrue(store.findById(order.getId()).isPresent());
    }

    @Test
    void validTransitionsAdvanceOneStepAtATime() {
        OrderStore store = new OrderStore();
        Order order = store.placeOrder(1, oneBurger());

        Optional<Order> afterPreparing = store.advanceStatus(order.getId(), OrderStatus.PREPARING);
        assertTrue(afterPreparing.isPresent());
        assertEquals(OrderStatus.PREPARING, afterPreparing.get().getStatus());

        Optional<Order> afterReady = store.advanceStatus(order.getId(), OrderStatus.READY);
        assertTrue(afterReady.isPresent());
        assertEquals(OrderStatus.READY, afterReady.get().getStatus());

        Optional<Order> afterServed = store.advanceStatus(order.getId(), OrderStatus.SERVED);
        assertTrue(afterServed.isPresent());
        assertEquals(OrderStatus.SERVED, afterServed.get().getStatus());
    }

    @Test
    void skippingAStepIsRejected() {
        OrderStore store = new OrderStore();
        Order order = store.placeOrder(2, oneBurger());

        Optional<Order> result = store.advanceStatus(order.getId(), OrderStatus.READY);

        assertFalse(result.isPresent());
        assertEquals(OrderStatus.PLACED, store.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void movingBackwardsIsRejected() {
        OrderStore store = new OrderStore();
        Order order = store.placeOrder(3, oneBurger());
        store.advanceStatus(order.getId(), OrderStatus.PREPARING);

        Optional<Order> result = store.advanceStatus(order.getId(), OrderStatus.PLACED);

        assertFalse(result.isPresent());
        assertEquals(OrderStatus.PREPARING, store.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void unknownOrderIdIsRejected() {
        OrderStore store = new OrderStore();
        Optional<Order> result = store.advanceStatus("no-such-order", OrderStatus.PREPARING);
        assertFalse(result.isPresent());
    }

    @Test
    void concurrentAdvanceAttemptsOnTheSameOrderOnlySucceedOnce() throws InterruptedException {
        OrderStore store = new OrderStore();
        Order order = store.placeOrder(5, oneBurger());

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
                if (store.advanceStatus(order.getId(), OrderStatus.PREPARING).isPresent()) {
                    successCount.incrementAndGet();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, successCount.get());
        assertEquals(OrderStatus.PREPARING, store.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void getAllOrdersReturnsEveryStoredOrder() {
        OrderStore store = new OrderStore();
        store.placeOrder(1, oneBurger());
        store.placeOrder(2, oneBurger());

        List<Order> all = store.getAllOrders();

        assertEquals(2, all.size());
    }
}
