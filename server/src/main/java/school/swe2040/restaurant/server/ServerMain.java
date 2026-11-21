package school.swe2040.restaurant.server;

import school.swe2040.restaurant.server.net.RestaurantServer;
import school.swe2040.restaurant.shared.NetworkConfig;

import java.io.IOException;

/**
 * Entry point for the server process. Run with {@code ./gradlew :server:run}
 * from the project root, or an optional port number as the first argument.
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = NetworkConfig.DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        RestaurantServer server = new RestaurantServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
