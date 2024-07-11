import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import modules.cluster.ClusterJwt;

public class EverythingAtHomeServer {
    public SharedData sharedData;
    protected SocketIOServer ioServer;
    
    public EverythingAtHomeServer() {
        this(3000);
    }
    
    public EverythingAtHomeServer(int socketioPort) {
        this("", socketioPort);
    }
    
    public EverythingAtHomeServer(String host, int socketioPort) {
        // Configuration for the server
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(socketioPort);
        
        // Create a new SocketIOServer instance
        this.ioServer = new SocketIOServer(config);
        
        this.addListeners();
    }
    
    private void addListeners() {
        // Event for client connection
        this.ioServer.addConnectListener(client -> {
            if (client.getHandshakeData().getAuthToken() == null) {
                client.disconnect();
                return;
            } else {
                boolean isValid = Utils.verifyJwt(client.getHandshakeData().getAuthToken().toString(), ClusterJwt.key);
                if (!isValid) {
                    client.disconnect();
                }
            }
            System.out.println("Client connected: " + client.getSessionId());
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData("enabled");
            }
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("disable", Object.class, (client, data, ackRequest) -> {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData("disabled");
            }
        });
        
        // Event for client disconnect
        this.ioServer.addDisconnectListener(client -> System.out.println("Client disconnected: " + client.getSessionId()));
    }
    
    public void start() {
        // Start the server
        ioServer.start();
        System.out.println("EverythingAtHome server started.");
        System.out.println("Socket.IO server started on port " + this.ioServer.getConfiguration().getPort());
    }
    
    public void stop() {
        // Stop the server
        ioServer.stop();
        System.out.println("EverythingAtHome server stopped");
    }
}
