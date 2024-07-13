import modules.TaskExecutor;

public class SharedData {
    public MasterControlServer masterControlServer;
    public SimpleHttpServer httpServer;
    public EverythingAtHomeServer everythingAtHomeServer;
    public TaskExecutor executor;
    public StorageHelper<Cluster> clusterStorageHelper;
    
    public SharedData(MasterControlServer masterControlServer, SimpleHttpServer httpServer, EverythingAtHomeServer everythingAtHomeServer) {
        this.masterControlServer = masterControlServer;
        this.httpServer = httpServer;
        this.everythingAtHomeServer = everythingAtHomeServer;
        this.executor = new TaskExecutor();
        this.clusterStorageHelper = new StorageHelper<>("cluster.dat");
        this.clusterStorageHelper.load();
        for (Cluster cluster : clusterStorageHelper.elements) {
            this.masterControlServer.clusters.put(cluster.id, cluster);
        }
    }
}
