package school.swe2040.restaurant;

/**
 * Connection defaults shared by the server and both clients, so none of
 * them need to be told the port separately in normal use.
 */
public final class NetworkConfig {

    public static final int DEFAULT_PORT = 5050;
    public static final String DEFAULT_HOST = "localhost";

    private NetworkConfig() {
    }
}
