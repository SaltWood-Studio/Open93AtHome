package top.saltwood.everythingAtHome;

import top.saltwood.everythingAtHome.modules.server.CenterServer;
import top.saltwood.everythingAtHome.modules.server.SimpleHttpServer;
import top.saltwood.everythingAtHome.modules.server.SocketIOServer;

import java.io.IOException;


public class SharedData {
    public static ConfigHelper config;
    public final CenterServer centerServer;
    public final SimpleHttpServer httpServer;
    public final SocketIOServer socketIOServer;
    // public TaskExecutor executor;
    public StorageHelper<Cluster> clusterStorageHelper;
    public StorageHelper<FileObject> fileStorageHelper;
    public StorageHelper<Token> tokenStorageHelper;
    
    public SharedData(CenterServer centerServer, SimpleHttpServer httpServer, SocketIOServer socketIOServer) {
        this.centerServer = centerServer;
        this.httpServer = httpServer;
        this.socketIOServer = socketIOServer;
        // this.executor = new TaskExecutor();
        this.clusterStorageHelper = new StorageHelper<>("cluster.dat", Cluster.class);
        this.clusterStorageHelper.load();
        for (Cluster cluster : clusterStorageHelper.elements) {
            this.centerServer.clusters.put(cluster.id, cluster);
        }
        this.fileStorageHelper = new StorageHelper<>("files.dat", FileObject.class);
        fileStorageHelper.load();
        this.tokenStorageHelper = new StorageHelper<>("tokens.dat", Token.class);
        this.tokenStorageHelper.load();
        config = new ConfigHelper("config.json");
        config.load();
    }
    
    public void saveAll() {
        clusterStorageHelper.save();
        fileStorageHelper.save();
        tokenStorageHelper.save();
        config.save();
        try {
            centerServer.update();
        } catch (IOException ignored) { }
    }
}
