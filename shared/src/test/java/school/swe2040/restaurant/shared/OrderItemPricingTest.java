package school.swe2040.restaurant.shared;

import org.junit.jupiter.api.Test;
import school.swe2040.restaurant.shared.model.MenuCategory;
import school.swe2040.restaurant.shared.model.MenuItem;
import school.swe2040.restaurant.shared.model.OrderItem;
import school.swe2040.restaurant.shared.model.Size;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for order line pricing, including size and add-on adjustments. */
class OrderItemPricingTest {

    private final MenuItem burger = new MenuItem("main-burger", "House Burger", "desc",
            MenuCategory.MAIN, 15.00, false, true, Arrays.asList("Extra cheese", "Bacon"));

    @Test
    void basePriceWithNoCustomizationsOrQuantityOne() {
        OrderItem item = new OrderItem(burger, 1, null, null, Collections.emptyList(), "");
        assertEquals(15.00, item.getUnitPrice(), 0.001);
        assertEquals(15.00, item.getLineTotal(), 0.001);
    }

    @Test
    void addOnsAddAFlatFeeEach() {
        OrderItem item = new OrderItem(burger, 1, null, null,
                Arrays.asList("Extra cheese", "Bacon"), "");
        assertEquals(15.00 + 2 * MenuItem.ADD_ON_PRICE, item.getUnitPrice(), 0.001);
    }

    @Test
    void quantityMultipliesTheUnitPrice() {
        OrderItem item = new OrderItem(burger, 3, null, null, Collections.emptyList(), "");
        assertEquals(15.00 * 3, item.getLineTotal(), 0.001);
    }

    @Test
    void sizeAdjustsPricePerUnit() {
        MenuItem soup = new MenuItem("starter-soup", "Soup", "desc", MenuCategory.STARTER,
                5.50, true, false, Collections.emptyList());
        OrderItem small = new OrderItem(soup, 1, Size.SMALL, null, Collections.emptyList(), "");
        OrderItem large = new OrderItem(soup, 1, Size.LARGE, null, Collections.emptyList(), "");
        assertEquals(5.50 + Size.SMALL.getPriceDelta(), small.getUnitPrice(), 0.001);
        assertEquals(5.50 + Size.LARGE.getPriceDelta(), large.getUnitPrice(), 0.001);
    }
}
