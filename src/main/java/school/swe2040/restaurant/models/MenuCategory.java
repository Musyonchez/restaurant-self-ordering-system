package school.swe2040.restaurant.models;

import java.io.Serializable;

/**
 * The four sections of the fixed menu. Used to group items on the customer
 * client's browsing screen.
 */
public enum MenuCategory implements Serializable {
    STARTER,
    MAIN,
    DRINK,
    DESSERT
}
