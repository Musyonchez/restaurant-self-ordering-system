package school.swe2040.restaurant.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Runs the connection-accept loop on the calling thread. Each accepted
 * socket is handed off to a new {@link ClientHandler}, which runs on its
 * own thread for the rest of that client's lifetime, so one slow or busy
 * client can never block another from connecting or being served.
 */
public final class RestaurantServer {

    private final int port;
    private final OrderManager orderManager;
    private final ConnectionRegistry registry;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public RestaurantServer(int port) {
        this.port = port;
        this.orderManager = new OrderManager();
        this.registry = new ConnectionRegistry();
    }

    /** Opens the listening socket and runs the accept loop until {@link #stop()} is called. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Restaurant server listening on port " + port + ".");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler connection = new ClientHandler(clientSocket, orderManager, registry);
                connection.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
                // If !running, this is the expected exception from closing serverSocket in stop().
            }
        }
    }

    /** Stops the accept loop and closes the listening socket. */
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Nothing more to do; the server is shutting down anyway.
            }
        }
    }

    /** Entry point for the server process, with an optional port number as the first argument. */
    public static void main(String[] args) throws IOException {
        int port = school.swe2040.restaurant.NetworkConfig.DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        RestaurantServer server = new RestaurantServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
