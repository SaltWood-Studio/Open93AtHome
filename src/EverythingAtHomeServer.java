import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class EverythingAtHomeServer {
    protected SocketIOServer ioServer;
    protected SimpleHttpServer httpServer;

    public EverythingAtHomeServer(){
        this(3000, 8080);
    }

    public EverythingAtHomeServer(int socketioPort, int httpPort){
        this("", socketioPort, httpPort);
    }

    public EverythingAtHomeServer(String host, int socketioPort, int httpPort) {
        // Configuration for the server
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(socketioPort);

        // Create a new SocketIOServer instance
        this.ioServer = new SocketIOServer(config);
        this.httpServer = new SimpleHttpServer(httpPort);

        this.addListeners();
    }

    private void addListeners() {
        // Event for client connection
        this.ioServer.addConnectListener(client -> System.out.println("Client connected: " + client.getSessionId()));

        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            System.out.println("Received data from client: " + data);
            if (ackRequest.isAckRequested()){
                ackRequest.sendAckData("enabled");
            }
        });

        // Event for client disconnect
        this.ioServer.addDisconnectListener(client -> System.out.println("Client disconnected: " + client.getSessionId()));
    }

    public void start(){
        // Start the server
        httpServer.start(true);
        ioServer.start();
        System.out.println("EverythingAtHome server started.");
        System.out.println("Socket.IO server started on port " + this.ioServer.getConfiguration().getPort());
    }

    public void stop(){
        // Stop the server
        httpServer.stop();
        ioServer.stop();
        System.out.println("EverythingAtHome server stopped");
    }
}
