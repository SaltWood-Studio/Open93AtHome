import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class EverythingAtHomeServer {
    protected SocketIOServer ioServer;

    public EverythingAtHomeServer(){
        this(3000);
    }

    public EverythingAtHomeServer(int port){
        this("", port);
    }

    public EverythingAtHomeServer(String host, int port) {
        // Configuration for the server
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);

        // Create a new SocketIOServer instance
        this.ioServer = new SocketIOServer(config);

        this.addListeners();
    }

    private void addListeners() {
        // Event for client connection
        this.ioServer.addConnectListener(client -> System.out.println("Client connected: " + client.getSessionId()));

        // Event for receiving message from client
        this.ioServer.addEventListener("my_event", String.class, (client, data, ackRequest) -> {
            System.out.println("Received data from client: " + data);
            // Optionally, send a response back to the client
            client.sendEvent("response_event", "Data received: " + data);
        });

        // Event for client disconnect
        this.ioServer.addDisconnectListener(client -> System.out.println("Client disconnected: " + client.getSessionId()));
    }

    public void start(){
        // Start the server
        ioServer.start();
        System.out.println("Socket.io server started on port 3000");
    }

    public void stop(){
        // Stop the server
        ioServer.stop();
        System.out.println("Socket.io server stopped");
    }
}
