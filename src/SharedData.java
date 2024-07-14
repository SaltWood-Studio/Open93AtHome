import modules.TaskExecutor;


public class SharedData {
    public static ConfigHelper config;
    public MasterControlServer masterControlServer;
    public SimpleHttpServer httpServer;
    public EverythingAtHomeServer everythingAtHomeServer;
    public TaskExecutor executor;
    public StorageHelper<Cluster> clusterStorageHelper;
    public StorageHelper<FileObject> fileStorageHelper;
    public StorageHelper<Token> tokenStorageHelper;
    
    public SharedData(MasterControlServer masterControlServer, SimpleHttpServer httpServer, EverythingAtHomeServer everythingAtHomeServer) {
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
        fileStorageHelper.load();
        this.tokenStorageHelper = new StorageHelper<>("tokens.dat", Token.class);
        this.tokenStorageHelper.load();
        config = new ConfigHelper("config.dat");
        config.load();
    }
    
    public void saveAll() {
        clusterStorageHelper.save();
        fileStorageHelper.save();
        tokenStorageHelper.save();
        config.save();
    }
}
