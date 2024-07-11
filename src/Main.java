import modules.cluster.ClusterJwt;

public class Main {
    
    public static void main(String[] args) {
        SimpleHttpServer.key = ClusterJwt.key;
        
        MasterControlServer masterServer = new MasterControlServer();
        SimpleHttpServer httpServer = new SimpleHttpServer(9388);
        EverythingAtHomeServer everythingServer = new EverythingAtHomeServer();
        SharedData sharedData = new SharedData(masterServer, httpServer, everythingServer);
        masterServer.sharedData = sharedData;
        httpServer.sharedData = sharedData;
        everythingServer.sharedData = sharedData;
        httpServer.start(true);
        everythingServer.start();
        
        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            httpServer.stop();
            everythingServer.stop();
        }));
    }
}
