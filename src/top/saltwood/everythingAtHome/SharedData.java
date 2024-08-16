package top.saltwood.everythingAtHome;

import top.saltwood.everythingAtHome.modules.server.CenterServer;
import top.saltwood.everythingAtHome.modules.server.SimpleHttpServer;
import top.saltwood.everythingAtHome.modules.server.SocketIOServer;
import top.saltwood.everythingAtHome.modules.storage.*;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Path;


public class SharedData {
    public static ConfigHelper config;
    public final CenterServer centerServer;
    public final SimpleHttpServer httpServer;
    public final SocketIOServer socketIOServer;
    // public TaskExecutor executor;
    public StorageHelper<Cluster> clusterStorageHelper;
    public StorageHelper<FileObject> fileStorageHelper;
    public StorageHelper<Token> tokenStorageHelper;
    public IBaseHelper<SecretKey> keyHelper;
    public ClusterStatisticsHelper statisticsHelper;

    public SharedData(CenterServer centerServer, SimpleHttpServer httpServer, SocketIOServer socketIOServer) throws Exception {
        config = new ConfigHelper("config.json");
        config.load();
        this.centerServer = centerServer;
        this.httpServer = httpServer;
        this.socketIOServer = socketIOServer;
        // this.executor = new TaskExecutor();
        this.clusterStorageHelper = new StorageHelper<>(Path.of(config.getItem().configDirectory, "clusters.dat").toString(), Cluster.class);
        this.clusterStorageHelper.load();
        for (Cluster cluster : clusterStorageHelper.getItem()) {
            this.centerServer.clusters.put(cluster.id, cluster);
        }
        this.statisticsHelper = new ClusterStatisticsHelper(this.centerServer.clusters.values());
        this.statisticsHelper.load();
        this.fileStorageHelper = new StorageHelper<>(Path.of(config.getItem().configDirectory, "files.dat").toString(), FileObject.class);
        fileStorageHelper.load();
        this.tokenStorageHelper = new StorageHelper<>(Path.of(config.getItem().configDirectory, "tokens.dat").toString(), Token.class);
        this.tokenStorageHelper.load();
        this.keyHelper = new SecretKeyHelper(config.getItem().configDirectory);
        this.keyHelper.load();
    }
    
    public void saveAll() {
        clusterStorageHelper.save();
        fileStorageHelper.save();
        tokenStorageHelper.save();
        try {
            keyHelper.save();
            statisticsHelper.save();
        } catch (Exception ignored) {}
        config.save();
        try {
            centerServer.update();
        } catch (IOException ignored) { }
    }
}
