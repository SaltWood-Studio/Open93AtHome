import modules.cluster.ClusterJwt;

public class Main {
    
    public static void main(String[] args) throws Exception {
        SimpleHttpServer.key = ClusterJwt.key;
        
        CenterServer centerServer = new CenterServer(); // 9300
        SimpleHttpServer httpServer = new SimpleHttpServer(); // 9388
        SocketIOServer socketIOServer = new SocketIOServer();
        SharedData sharedData = new SharedData(centerServer, httpServer, socketIOServer);
        centerServer.sharedData = sharedData;
        httpServer.sharedData = sharedData;
        socketIOServer.sharedData = sharedData;
        centerServer.update();
        httpServer.start(true);
        socketIOServer.start();
        
        sharedData.fileStorageHelper.save();
        
        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            socketIOServer.ioServer.getAllClients().forEach(client -> {
                client.sendEvent("disconnect", "Stopping server.");
                client.disconnect();
            });
            sharedData.saveAll();
            httpServer.stop();
            socketIOServer.stop();
        }));
    }
}
