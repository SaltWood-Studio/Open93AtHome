public class Cluster {
    public String id;
    public String secret;
    public String name;
    public boolean isOnline;
    public String ip;
    public int port;
    
    public Cluster(String id, String secret, String name) {
        this.id = id;
        this.secret = secret;
        this.name = name;
    }
}
