package school.swe2040.restaurant.shared.model;

import java.io.Serializable;

/**
 * Spice level for menu items that support the option. Does not affect price,
 * it only changes how the kitchen prepares the item.
 */
public enum SpiceLevel implements Serializable {
    MILD,
    MEDIUM,
    HOT,
    EXTRA_HOT
}
