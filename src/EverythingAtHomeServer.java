import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import modules.cluster.ClusterJwt;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class EverythingAtHomeServer {
    public SharedData sharedData;
    protected SocketIOServer ioServer;
    private final ConcurrentHashMap<String, String> sessions;
    
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
        this.sessions = new ConcurrentHashMap<>();
        
        this.addListeners();
    }
    
    private void addListeners() {
        // Event for client connection
        this.ioServer.addConnectListener(client -> {
            String token = ((LinkedHashMap<String, String>)client.getHandshakeData().getAuthToken()).get("token");
            if (client.getHandshakeData().getAuthToken() == null || token == null || token.isEmpty()) {
                client.disconnect();
                return;
            } else {
                String id = Utils.decodeJwt(token, ClusterJwt.key, "cluster_id");
                if (id == null || this.sharedData.masterControlServer.clusters.get(id) == null) {
                    client.disconnect();
                    return;
                }
                else {
                    this.sessions.put(client.getSessionId().toString(), id);
                }
            }
            System.out.println("Client connected: " + client.getSessionId());
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            boolean enabled = sharedData.masterControlServer.tryEnable(this.sessions.get(client.getSessionId().toString()));
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(enabled?"enabled":"not enabled");
            }
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("disable", Object.class, (client, data, ackRequest) -> {
            this.sharedData.masterControlServer.onlineClusters.remove(
                    this.sharedData.masterControlServer.clusters.get(
                        this.sessions.get(client.getSessionId().toString())));
            this.sessions.remove(client.getSessionId().toString());
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData("disabled");
            }
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("keep-alive", Object.class, (client, data, ackRequest) -> {
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData("success");
            }
        });
        
        // Event for client disconnect
        this.ioServer.addDisconnectListener(client -> {
            this.sharedData.masterControlServer.onlineClusters.remove(
                    this.sharedData.masterControlServer.clusters.get(
                            this.sessions.get(client.getSessionId().toString())));
            this.sessions.remove(client.getSessionId().toString());
            System.out.println("Client disconnected: " + client.getSessionId());
        });
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
