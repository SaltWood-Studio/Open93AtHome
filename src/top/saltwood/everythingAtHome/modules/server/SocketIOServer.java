package top.saltwood.everythingAtHome.modules.server;

import com.corundumstudio.socketio.ClientOperations;
import com.corundumstudio.socketio.Configuration;
import top.saltwood.everythingAtHome.Cluster;
import top.saltwood.everythingAtHome.SharedData;
import top.saltwood.everythingAtHome.Utils;
import top.saltwood.everythingAtHome.modules.Config;
import top.saltwood.everythingAtHome.modules.cluster.ClusterJwt;
import top.saltwood.everythingAtHome.modules.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SocketIOServer {
    private final ConcurrentHashMap<String, String> sessions;
    public SharedData sharedData;
    protected final com.corundumstudio.socketio.SocketIOServer ioServer;
    
    public SocketIOServer() {
        this(9300);
    }
    
    public SocketIOServer(int socketioPort) {
        this("", socketioPort);
    }
    
    public SocketIOServer(String host, int socketioPort) {
        // Configuration for the server
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(socketioPort);
        
        // Create a new SocketIOServer instance
        this.ioServer = new com.corundumstudio.socketio.SocketIOServer(config);
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
                if (id == null || this.sharedData.centerServer.clusters.get(id) == null) {
                    client.disconnect();
                    return;
                } else {
                    this.sessions.put(client.getSessionId().toString(), id);
                }
            }
            client.sendEvent("message", "Welcome to Open93@Home (v" + Config.version + ")! You can find us at https://github.com/SaltWood-Studio/Open93AtHome.");
            Logger.logger.logLine("Client connected: " + client.getSessionId());
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            Map<String, Object> dictionary = (Map<String, Object>) data;
            String host = (dictionary.get("host") != null) ? dictionary.get("host").toString() : "";
            int port = (dictionary.get("port") != null) ? Integer.parseInt(dictionary.get("port").toString()) : 0;
            Cluster cluster = sharedData.centerServer.clusters.get(this.sessions.get(client.getSessionId().toString()));
            cluster.ip = host;
            cluster.port = port;
            sharedData.centerServer.clusters.put(cluster.id, cluster);
            sharedData.clusterStorageHelper.save();
            boolean enabled = false;
            Exception exception = null;
            try {
                if (!sharedData.centerServer.getFiles().isEmpty()) {
                    sharedData.centerServer.tryEnable(this.sessions.get(client.getSessionId().toString()));
                    cluster.startWarden(sharedData.fileStorageHelper.getItem());
                }
                enabled = true;
            } catch (Exception e) {
                exception = e;
            }
            if (ackRequest.isAckRequested()) {
                if (enabled) {
                    if (sharedData.centerServer.getOnlineClusters().noneMatch(c -> c.id.equals(cluster.id))) {
                        sharedData.centerServer.clusters.get(cluster.id).isOnline = true;
                    }
                    ackRequest.sendAckData((Object) new Object[]{null, true});
                } else {
                    final String message = exception.getMessage();
                    ackRequest.sendAckData((Object) new Object[]{new HashMap<String, String>() {
                        {
                            put("message", "Failed to enable, error: " + message);
                        }
                    }});
                }
            }
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("disable", Object.class, (client, data, ackRequest) -> {
            String id = sessions.get(client.getSessionId().toString());
            this.sharedData.centerServer.clusters.values().stream().filter(cluster -> cluster.id.equals(id))
                    .forEach(cluster -> cluster.isOnline = false);
            this.sessions.remove(client.getSessionId().toString());
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData("disabled");
            }
        });
        
        // Event for receiving message from client
        this.ioServer.addEventListener("keep-alive", Object.class, (client, data, ackRequest) -> {
            if (ackRequest.isAckRequested()) {
                if (sharedData.centerServer.getOnlineClusters()
                        .anyMatch(cluster -> cluster.id.equals(this.sessions.get(client.getSessionId().toString())))) {
                    Map<String, Object> request = (Map<String, Object>) data;
                    Integer hits = (Integer) request.get("hits");
                    Integer bytes = (Integer) request.get("bytes");
                    Cluster cluster = sharedData.centerServer.clusters.get(this.sessions.get(client.getSessionId().toString()));
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
            String id = sessions.get(client.getSessionId().toString());
            this.sharedData.centerServer.clusters.values().stream().filter(cluster -> cluster.id.equals(id))
                    .forEach(cluster -> cluster.isOnline = false);
            this.sessions.remove(client.getSessionId().toString());
            Logger.logger.logLine("Client disconnected: " + client.getSessionId());
        });
    }
    
    public void start() {
        // Start the server
        ioServer.start();
        Logger.logger.logLine("EverythingAtHome server started.");
        Logger.logger.logLine("Socket.IO server started on port " + this.ioServer.getConfiguration().getPort());
    }
    
    public void stop() {
        // Stop the server
        ioServer.stop();
        Logger.logger.logLine("EverythingAtHome server stopped");
    }
    
    public void disconnectAll() {
        ioServer.getAllClients().forEach(ClientOperations::disconnect);
    }
}
