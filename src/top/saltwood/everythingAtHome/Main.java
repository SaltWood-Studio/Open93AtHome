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

        CenterServer centerServer = new CenterServer();
        SimpleHttpServer httpServer = new SimpleHttpServer(); // 9388
        SocketIOServer socketIOServer = new SocketIOServer(); // 9300

        SharedData sharedData = new SharedData(centerServer, httpServer, socketIOServer);
        SimpleHttpServer.key = sharedData.keyHelper.getItem();
        centerServer.sharedData = sharedData;
        httpServer.sharedData = sharedData;
        socketIOServer.sharedData = sharedData;

        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            socketIOServer.disconnectAll();
            sharedData.saveAll();
            httpServer.stop();
            socketIOServer.stop();
        }));

        Logger.logger.logLine("Checking files...");
        centerServer.check();
        centerServer.update();
        httpServer.start(true);
        socketIOServer.start();
        
        sharedData.fileStorageHelper.save();
    }
}
