package school.swe2040.restaurant.protocol;

import java.io.Serializable;

/**
 * Marker interface for every object sent over the wire between a client and
 * the server. Every message is sent as a single {@code writeObject} call on
 * an {@link java.io.ObjectOutputStream}, read back with a matching
 * {@link java.io.ObjectInputStream#readObject()} on the other end.
 */
public interface Message extends Serializable {
}
