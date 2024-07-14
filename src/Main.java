import modules.cluster.ClusterJwt;

public class Main {
    
    public static void main(String[] args) throws Exception {
        SimpleHttpServer.key = ClusterJwt.key;
        
        MasterControlServer masterServer = new MasterControlServer();
        SimpleHttpServer httpServer = new SimpleHttpServer(9388);
        EverythingAtHomeServer everythingServer = new EverythingAtHomeServer();
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
            sharedData.fileStorageHelper.save();
            sharedData.clusterStorageHelper.save();
            httpServer.stop();
            everythingServer.stop();
        }));
    }
}
