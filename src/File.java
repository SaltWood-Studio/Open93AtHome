import java.util.Arrays;

public class File {
    public String path;
    public String hash;
    public long size;
    public long lastModified;
    
    public File(String path, String hash, long size, long mtime) {
        this.path = path;
        this.hash = hash;
        this.size = size;
        this.lastModified = mtime;
    }
    
    public String getName() {
        String[] paths = this.path.split("/");
        return paths[paths.length - 1];
    }
}
