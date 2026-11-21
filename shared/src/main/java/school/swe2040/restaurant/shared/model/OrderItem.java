package school.swe2040.restaurant.shared.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One line of an order: a menu item plus whatever customizations the
 * customer chose for it, and how many of it they want.
 */
public final class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MenuItem menuItem;
    private final int quantity;
    private final Size size;
    private final SpiceLevel spiceLevel;
    private final List<String> selectedAddOns;
    private final String note;

    public OrderItem(MenuItem menuItem, int quantity, Size size, SpiceLevel spiceLevel,
                      List<String> selectedAddOns, String note) {
        this.menuItem = Objects.requireNonNull(menuItem, "menuItem");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        this.quantity = quantity;
        this.size = size;
        this.spiceLevel = spiceLevel;
        this.selectedAddOns = Collections.unmodifiableList(
                Objects.requireNonNull(selectedAddOns, "selectedAddOns"));
        this.note = note == null ? "" : note;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public Size getSize() {
        return size;
    }

    public SpiceLevel getSpiceLevel() {
        return spiceLevel;
    }

    public List<String> getSelectedAddOns() {
        return selectedAddOns;
    }

    public String getNote() {
        return note;
    }

    /**
     * Price for one unit of this line, including the size adjustment (if
     * any) and a flat fee per selected add-on.
     */
    public double getUnitPrice() {
        double price = menuItem.getBasePrice();
        if (size != null) {
            price += size.getPriceDelta();
        }
        price += selectedAddOns.size() * MenuItem.ADD_ON_PRICE;
        return price;
    }

    /** Price for this line at its full quantity. */
    public double getLineTotal() {
        return getUnitPrice() * quantity;
    }

    @Override
    public String toString() {
        return quantity + "x " + menuItem.getName();
    }
}
