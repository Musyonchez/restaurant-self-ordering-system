package school.swe2040.restaurant.server.net;

import school.swe2040.restaurant.server.store.OrderStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Runs the connection-accept loop on the calling thread. Each accepted
 * socket is handed off to a new {@link ClientConnection}, which runs on its
 * own thread for the rest of that client's lifetime, so one slow or busy
 * client can never block another from connecting or being served.
 */
public final class RestaurantServer {

    private final int port;
    private final OrderStore orderStore;
    private final ConnectionRegistry registry;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public RestaurantServer(int port) {
        this.port = port;
        this.orderStore = new OrderStore();
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
                ClientConnection connection = new ClientConnection(clientSocket, orderStore, registry);
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
}
