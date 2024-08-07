package top.saltwood.everythingAtHome;

import top.saltwood.everythingAtHome.modules.Logger;

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
    public String filePath;
    
    public FileObject(String path) throws FileNotFoundException {
        this.filePath = path;
        Path p = Path.of(SharedData.config.getItem().filePath, path);
        FileInputStream stream = new FileInputStream(p.toString());
        this.path = Utils.encodeUrl(path);
        this.hash = computeHash(stream);
        this.size = p.toFile().length();
        this.lastModified = p.toFile().lastModified();
    }
    
    public FileObject(String path, String hash, long size, long mtime) {
        this.filePath = path;
        this.path = Utils.encodeUrl(path);
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
            e.printStackTrace(Logger.logger);
            return null;
        }
    }
}
