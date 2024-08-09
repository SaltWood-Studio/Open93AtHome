package top.saltwood.everythingAtHome;

import com.alibaba.fastjson2.annotation.JSONField;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    public int measureBandwidth;
    @JSONField(serialize = false)
    private Timer wardenTimer;
    public boolean isBanned;
    
    public Cluster(String id, String secret, String name, int bandwidth) {
        this.id = id;
        this.secret = secret;
        this.name = name;
        this.bandwidth = bandwidth;
        this.hits = 0L;
        this.traffics = 0L;
        this.pendingHits = 0L;
        this.pendingTraffics = 0L;
        this.isBanned = false;
    }
    
    public Long getTraffics() {
        return this.traffics;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Cluster cluster)) return false;
        return this.id.equals(cluster.id);
    }
    
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public void startWarden(List<FileObject> files) {
        this.isOnline = true;
        this.wardenTimer = new Timer(true);
        this.wardenTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    try {
                        Thread.sleep(3000);
                        FileObject file = Utils.random(files);
                        boolean isValid = doWardenOnce(file);
                        if (isValid) {
                            return;
                        }
                    } catch (Exception ignored) { }
                }
                stopWarden();
            }
        }, 0, 5 * 60 * 1000);
    }

    public void stopWarden() {
        if (this.wardenTimer == null) return;
        this.isOnline = false;
        this.wardenTimer.cancel();
        this.wardenTimer = null;
    }
    
    public boolean doWardenOnce(FileObject file) throws IOException {
        String sign = Utils.getSign(file, this);
        String url = Utils.getUrl(file, this, sign);
        return Utils.checkCluster(url, file);
    }
}
