package school.swe2040.restaurant.shared.model;

import java.io.Serializable;

/**
 * Portion size for menu items that support the option. Each size carries a
 * price adjustment relative to a menu item's base price.
 */
public enum Size implements Serializable {
    SMALL(-1.00),
    REGULAR(0.00),
    LARGE(1.50);

    private final double priceDelta;

    Size(double priceDelta) {
        this.priceDelta = priceDelta;
    }

    public double getPriceDelta() {
        return priceDelta;
    }
}
