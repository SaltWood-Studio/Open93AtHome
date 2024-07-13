public class Cluster {
    public String id;
    public String secret;
    public String name;
    public int bandwidth;
    public String ip;
    public int port;
    public Long traffic;
    
    public Cluster(String id, String secret, String name, int bandwidth) {
        this.id = id;
        this.secret = secret;
        this.name = name;
        this.bandwidth = bandwidth;
        this.traffic = 0L;
    }
}
