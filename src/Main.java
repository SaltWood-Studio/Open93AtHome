import modules.cluster.ClusterJwt;

public class Main {
    
    public static void main(String[] args) throws Exception {
        SimpleHttpServer.key = ClusterJwt.key;
        
        CenterServer masterServer = new CenterServer(); // 9300
        SimpleHttpServer httpServer = new SimpleHttpServer(); // 9388
        SocketIOServer everythingServer = new SocketIOServer();
        SharedData sharedData = new SharedData(masterServer, httpServer, everythingServer);
        masterServer.sharedData = sharedData;
        httpServer.sharedData = sharedData;
        everythingServer.sharedData = sharedData;
        masterServer.update();
        httpServer.start(true);
        everythingServer.start();
        
        sharedData.fileStorageHelper.save();
        
        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sharedData.saveAll();
            httpServer.stop();
            everythingServer.stop();
        }));
    }
}
