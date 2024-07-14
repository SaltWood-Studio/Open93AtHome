import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;

public class FileObject {
    public String path;
    public String hash;
    public long size;
    public long lastModified;
    
    public FileObject(String path) throws FileNotFoundException {
        this.path = path;
        Path p = Path.of(SharedData.config.config.filePath, path);
        FileInputStream stream = new FileInputStream(p.toString());
        this.hash = computeHash(stream);
        this.size = p.toFile().length();
        this.lastModified = p.toFile().lastModified();
    }
    
    public FileObject(String path, String hash, long size, long mtime) {
        this.path = path;
        this.hash = hash;
        this.size = size;
        this.lastModified = mtime;
    }
    
    public static String computeHash(InputStream stream) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024];
            int len;
            while ((len = stream.read(buffer)) != -1) {
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
    
    public static String computeHash(byte[] data) {
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
    
    public boolean compareHash(byte[] bytes) {
        String inputHash = computeHash(bytes);
        return inputHash.equals(this.hash);
    }
}
