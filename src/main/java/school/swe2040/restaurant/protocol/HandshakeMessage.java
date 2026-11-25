package school.swe2040.restaurant.protocol;

/**
 * First message sent by any client right after connecting. Identifies the
 * connection as a customer tablet (with its table number) or a kitchen
 * display (table number is unused, always {@code -1}).
 */
public final class HandshakeMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final ClientRole role;
    private final int tableNumber;

    public static HandshakeMessage forCustomer(int tableNumber) {
        return new HandshakeMessage(ClientRole.CUSTOMER, tableNumber);
    }

    public static HandshakeMessage forKitchen() {
        return new HandshakeMessage(ClientRole.KITCHEN, -1);
    }

    public HandshakeMessage(ClientRole role, int tableNumber) {
        this.role = role;
        this.tableNumber = tableNumber;
    }

    public ClientRole getRole() {
        return role;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}
