import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import modules.cluster.ClusterJwt;

import java.io.IOException;

public class Main {
    
    public static void main(String[] args) throws Exception {
        SimpleHttpServer.key = ClusterJwt.key;
        
        MasterControlServer masterServer = new MasterControlServer();
        Cluster cluster = new Cluster("aaaaaaaa", "bbbbbbbb", "cccccccc");
        File[] files = new File[2];
        files[0] = new File("/1/1", "0000000000000000000000000000000000000000", 1, 1);
        files[1] = new File("/2/2", "0000000000000000000000000000000000000000", 2, 2);
        
        
        if (1 == 1) return;
        
        masterServer.setFiles(files);
        masterServer.clusters.put(cluster.id, cluster);
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
