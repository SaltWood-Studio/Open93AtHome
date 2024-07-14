import com.alibaba.fastjson2.annotation.JSONField;

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
}
