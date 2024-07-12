import java.io.FileInputStream;
import java.security.MessageDigest;

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
    
    public String computeHash() {
        try {
            FileInputStream fis = new FileInputStream(path);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public String computeHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(data);
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean compareHash(byte[] bytes) {
        String inputHash = computeHash(bytes);
        return inputHash.equals(this.hash);
    }
}
