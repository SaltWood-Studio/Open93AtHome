package top.saltwood.everythingAtHome;

import top.saltwood.everythingAtHome.modules.Config;
import top.saltwood.everythingAtHome.modules.Logger;
import top.saltwood.everythingAtHome.modules.server.CenterServer;
import top.saltwood.everythingAtHome.modules.server.SimpleHttpServer;
import top.saltwood.everythingAtHome.modules.server.SocketIOServer;

public class Main {
    
    public static void main(String[] args) throws Exception {
        Logger.logger.logLine("Starting Open93@Home");
        Logger.logger.logLine("version: " + Config.version);
        CenterServer centerServer = new CenterServer(); // 9300
        SimpleHttpServer httpServer = new SimpleHttpServer(); // 9388
        SocketIOServer socketIOServer = new SocketIOServer();
        SharedData sharedData = new SharedData(centerServer, httpServer, socketIOServer);
        centerServer.sharedData = sharedData;
        httpServer.sharedData = sharedData;
        socketIOServer.sharedData = sharedData;
        Logger.logger.logLine("Checking files...");
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
