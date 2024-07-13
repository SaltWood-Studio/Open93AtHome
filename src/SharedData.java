import modules.TaskExecutor;

import java.io.IOException;

public class SharedData {
    public MasterControlServer masterControlServer;
    public SimpleHttpServer httpServer;
    public EverythingAtHomeServer everythingAtHomeServer;
    public TaskExecutor executor;
    public StorageHelper<Cluster> clusterStorageHelper;
    public StorageHelper<FileObject> fileStorageHelper;
    
    public SharedData(MasterControlServer masterControlServer, SimpleHttpServer httpServer, EverythingAtHomeServer everythingAtHomeServer) throws Exception {
        this.masterControlServer = masterControlServer;
        this.httpServer = httpServer;
        this.everythingAtHomeServer = everythingAtHomeServer;
        this.executor = new TaskExecutor();
        this.clusterStorageHelper = new StorageHelper<>("cluster.dat", Cluster.class);
        this.clusterStorageHelper.load();
        for (Cluster cluster : clusterStorageHelper.elements) {
            this.masterControlServer.clusters.put(cluster.id, cluster);
        }
        this.fileStorageHelper = new StorageHelper<>("files.dat", FileObject.class);
    }
}
