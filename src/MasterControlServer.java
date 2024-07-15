import com.github.luben.zstd.Zstd;
import modules.AvroEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MasterControlServer {
    public final ConcurrentHashMap<String, FileObject> pathToFile;
    public final ConcurrentHashMap<String, FileObject> hashToFile;
    public final ConcurrentHashMap<String, Cluster> clusters;
    public final ArrayList<Cluster> onlineClusters;
    public SharedData sharedData;
    private byte[] avroBytes;
    
    public MasterControlServer() {
        this.avroBytes = new byte[1];
        this.pathToFile = new ConcurrentHashMap<>();
        this.hashToFile = new ConcurrentHashMap<>();
        this.clusters = new ConcurrentHashMap<>();
        this.onlineClusters = new ArrayList<>();
    }
    
    public List<FileObject> getFiles() {
        return this.sharedData.fileStorageHelper.elements;
    }
    
    public void setFiles(List<FileObject> files) throws IOException {
        this.sharedData.fileStorageHelper.elements = files;
        update();
    }
    
    public void update() throws IOException {
        updateDictionary();
        refreshAvroBytes();
    }
    
    public void updateDictionary() {
        synchronized (this.pathToFile) {
            this.pathToFile.clear();
            for (FileObject file : this.sharedData.fileStorageHelper.elements) {
                this.pathToFile.put(file.path, file);
                this.hashToFile.put(file.hash, file);
            }
        }
    }
    
    public byte[] getAvroBytes() {
        synchronized (this.avroBytes) {
            return this.avroBytes;
        }
    }
    
    public void refreshAvroBytes() throws IOException {
        AvroEncoder encoder = new AvroEncoder();
        synchronized (this.sharedData.fileStorageHelper.elements) {
            encoder.setElements(this.sharedData.fileStorageHelper.elements.size());
            for (FileObject file : this.sharedData.fileStorageHelper.elements) {
                encoder.setString(file.path);
                encoder.setString(file.hash);
                encoder.setLong(file.size);
                encoder.setLong(file.lastModified);
            }
        }
        encoder.byteStream.close();
        synchronized (this.avroBytes) {
            this.avroBytes = Zstd.compress(encoder.byteStream.toByteArray());
        }
    }
    
    public String requestDownload(String path) {
        // 如果 this.files 中有 path 相同的文件
        if (this.pathToFile.containsKey(path)) {
            FileObject file = this.pathToFile.get(path);
            // 选择一个节点
            Cluster cluster = chooseOneCluster();
            if (cluster == null) return null;
            return requestDownload(file, cluster);
        } else {
            return null;
        }
    }
    
    public String requestDownload(FileObject file, Cluster cluster) {
        // 为这个请求计算 sign
        String sign = Utils.getSign(file, cluster);
        // 为选择到的节点加上流量
        cluster.pendingHits += 1;
        cluster.pendingTraffics += file.size;
        if (sign == null) return null;
        return "http://" + cluster.ip + ":" + cluster.port + "/download/" + file.hash + sign;
    }
    
    public Cluster chooseOneCluster() {
        // 从 this.onlineClusters 中随机选择一个并返回
        synchronized (this.onlineClusters) {
            if (this.onlineClusters.size() == 0) return null;
            return this.onlineClusters.get(new Random().nextInt(this.onlineClusters.size()));
        }
    }
    
    public void tryEnable(String id) throws Exception {
        Cluster cluster = this.clusters.get(id);
        
    }
}
