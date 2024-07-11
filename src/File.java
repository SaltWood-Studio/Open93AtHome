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
    
    public String getName(){
        return Arrays.stream(this.path.split("/")).reduce((first, second) -> second).get();
    }
}
