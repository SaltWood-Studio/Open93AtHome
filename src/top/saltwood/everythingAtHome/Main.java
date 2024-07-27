package top.saltwood.everythingAtHome;

import top.saltwood.everythingAtHome.modules.Config;
import top.saltwood.everythingAtHome.modules.cluster.ClusterJwt;
import top.saltwood.everythingAtHome.modules.cluster.Logger;
import top.saltwood.everythingAtHome.modules.server.CenterServer;
import top.saltwood.everythingAtHome.modules.server.SimpleHttpServer;
import top.saltwood.everythingAtHome.modules.server.SocketIOServer;

public class Main {
    
    public static void main(String[] args) throws Exception {
        Logger.logger.log("Starting Open93@Home");
        Logger.logger.log("version: " + Config.version);
        // 统一 key
        SimpleHttpServer.key = ClusterJwt.key;
        CenterServer centerServer = new CenterServer(); // 9300
        SimpleHttpServer httpServer = new SimpleHttpServer(); // 9388
        SocketIOServer socketIOServer = new SocketIOServer();
        SharedData sharedData = new SharedData(centerServer, httpServer, socketIOServer);
        centerServer.sharedData = sharedData;
        httpServer.sharedData = sharedData;
        socketIOServer.sharedData = sharedData;
        Logger.logger.log("Checking files...");
        centerServer.check();
        centerServer.update();
        httpServer.start(true);
        socketIOServer.start();
        
        sharedData.fileStorageHelper.save();
        
        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            socketIOServer.disconnectAll();
            sharedData.saveAll();
            httpServer.stop();
            socketIOServer.stop();
        }));
    }
}
