import com.alibaba.fastjson2.annotation.JSONField;

import java.io.IOException;
import java.util.List;

public class Cluster {
    public String id;
    public String secret;
    public String name;
    public int bandwidth;
    public String ip;
    public int port;
    public Long hits;
    public Long traffics;
    @JSONField(serialize = false)
    public Long pendingHits;
    @JSONField(serialize = false)
    public Long pendingTraffics;
    @JSONField(serialize = false)
    public boolean isOnline = false;
    @JSONField(serialize = false)
    private Thread wardenThread;
    
    public Cluster(String id, String secret, String name, int bandwidth) {
        this.id = id;
        this.secret = secret;
        this.name = name;
        this.bandwidth = bandwidth;
        this.hits = 0L;
        this.traffics = 0L;
        this.pendingHits = 0L;
        this.pendingTraffics = 0L;
    }
    
    public Long getTraffics() {
        return this.traffics;
    }
    
    @Override
    public boolean equals(Object obj) {
        Cluster cluster = (Cluster) obj;
        if (cluster == null) return false;
        return this.id.equals(cluster.id);
    }
    
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
    
    public void startWarden(List<FileObject> files) {
        this.isOnline = true;
        this.wardenThread = new Thread(() -> {
            try {
                while (this.isOnline) {
                    Thread.sleep(15 * 60 * 1000);
                    if (!this.isOnline) {
                        break;
                    }
                    FileObject file = Utils.random(files);
                    boolean isValid = this.doWardenOnce(file);
                    if (!isValid) {
                        this.isOnline = false;
                        this.wardenThread = null;
                        break;
                    }
                }
            } catch (Exception e) {
            }
        });
    }
    
    public boolean doWardenOnce(FileObject file) throws IOException {
        String sign = Utils.getSign(file, this);
        String url = Utils.getUrl(file, this, sign);
        return Utils.checkCluster(url, file);
    }
}
