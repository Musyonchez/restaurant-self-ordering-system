package school.swe2040.restaurant.shared.protocol;

import java.io.Serializable;

/** Identifies which kind of client a socket connection belongs to. */
public enum ClientRole implements Serializable {
    CUSTOMER,
    KITCHEN
}
