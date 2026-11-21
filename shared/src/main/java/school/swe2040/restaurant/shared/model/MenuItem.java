package school.swe2040.restaurant.shared.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single item on the fixed restaurant menu. Instances only ever come from
 * {@link school.swe2040.restaurant.shared.menu.MenuCatalog}, so the customer
 * client and the server always agree on ids, names and prices.
 *
 * <p>Not every item supports every customization. {@code sizeCustomizable}
 * and {@code spiceCustomizable} say whether the size and spice level pickers
 * apply to this item, and {@code availableAddOns} lists the add-ons this
 * item offers (an empty list means none). A free-text note is always allowed
 * on any item.
 */
public final class MenuItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final double ADD_ON_PRICE = 0.75;

    private final String id;
    private final String name;
    private final String description;
    private final MenuCategory category;
    private final double basePrice;
    private final boolean sizeCustomizable;
    private final boolean spiceCustomizable;
    private final List<String> availableAddOns;

    public MenuItem(String id, String name, String description, MenuCategory category,
                     double basePrice, boolean sizeCustomizable, boolean spiceCustomizable,
                     List<String> availableAddOns) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.basePrice = basePrice;
        this.sizeCustomizable = sizeCustomizable;
        this.spiceCustomizable = spiceCustomizable;
        this.availableAddOns = Collections.unmodifiableList(
                Objects.requireNonNull(availableAddOns, "availableAddOns"));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public boolean isSizeCustomizable() {
        return sizeCustomizable;
    }

    public boolean isSpiceCustomizable() {
        return spiceCustomizable;
    }

    public List<String> getAvailableAddOns() {
        return availableAddOns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MenuItem)) {
            return false;
        }
        MenuItem other = (MenuItem) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
