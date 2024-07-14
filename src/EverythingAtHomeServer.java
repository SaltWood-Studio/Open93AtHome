import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import modules.cluster.ClusterJwt;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EverythingAtHomeServer {
    private final ConcurrentHashMap<String, String> sessions;
    public SharedData sharedData;
    protected SocketIOServer ioServer;
    
    public EverythingAtHomeServer() {
        this(9300);
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
            String token = ((LinkedHashMap<String, String>) client.getHandshakeData().getAuthToken()).get("token");
            if (client.getHandshakeData().getAuthToken() == null || token == null || token.isEmpty()) {
                client.disconnect();
                return;
            } else {
                String id = Utils.decodeJwt(token, ClusterJwt.key, "cluster_id");
                if (id == null || this.sharedData.masterControlServer.clusters.get(id) == null) {
                    client.disconnect();
                    return;
                } else {
                    this.sessions.put(client.getSessionId().toString(), id);
                }
            }
            System.out.println("Client connected: " + client.getSessionId());
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            Map<String, Object> dictionary = (Map<String, Object>) data;
            String host = (dictionary.get("host") != null) ? dictionary.get("host").toString() : "";
            int port = (dictionary.get("port") != null) ? Integer.parseInt(dictionary.get("port").toString()) : 0;
            Cluster cluster = sharedData.masterControlServer.clusters.get(this.sessions.get(client.getSessionId().toString()));
            cluster.ip = host;
            cluster.port = port;
            sharedData.masterControlServer.clusters.put(cluster.id, cluster);
            sharedData.clusterStorageHelper.save();
            boolean enabled = false;
            Exception exception = null;
            try {
                if (sharedData.masterControlServer.getFiles().size() > 0) {
                    sharedData.masterControlServer.tryEnable(this.sessions.get(client.getSessionId().toString()));
                }
                enabled = true;
            } catch (Exception e) {
                exception = e;
            }
            if (ackRequest.isAckRequested()) {
                if (enabled) {
                    sharedData.masterControlServer.onlineClusters.add(cluster);
                    ackRequest.sendAckData((Object) new Object[]{null, true});
                } else {
                    final String message = exception != null ? exception.getMessage() : "Failed to enable";
                    ackRequest.sendAckData((Object) new Object[]{new HashMap<String, String>() {
                        {
                            put("message", "Failed: " + message);
                        }
                    }});
                }
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
                if (sharedData.masterControlServer.onlineClusters
                        .stream().anyMatch(cluster -> cluster.id.equals(this.sessions.get(client.getSessionId().toString())))) {
                    Map<String, Object> request = (Map<String, Object>) data;
                    Long hits = (Long) request.get("hits");
                    Long bytes = (Long) request.get("bytes");
                    Cluster cluster = sharedData.masterControlServer.clusters.get(this.sessions.get(client.getSessionId().toString()));
                    cluster.hits += Math.min(cluster.pendingHits, hits);
                    cluster.traffics += Math.min(cluster.pendingTraffics, bytes);
                    cluster.pendingHits = 0L;
                    cluster.pendingTraffics = 0L;
                    ackRequest.sendAckData((Object) new Object[]{null, Utils.getISOTime()});
                } else {
                    ackRequest.sendAckData((Object) new Object[]{null, false});
                }
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
