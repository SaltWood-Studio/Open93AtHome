package top.saltwood.everythingAtHome.modules.server;

import com.github.luben.zstd.Zstd;
import top.saltwood.everythingAtHome.Cluster;
import top.saltwood.everythingAtHome.FileObject;
import top.saltwood.everythingAtHome.SharedData;
import top.saltwood.everythingAtHome.Utils;
import top.saltwood.everythingAtHome.modules.AvroEncoder;
import top.saltwood.everythingAtHome.modules.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CenterServer {
    public final ConcurrentHashMap<String, FileObject> pathToFile;
    public final ConcurrentHashMap<String, FileObject> hashToFile;
    public final ConcurrentHashMap<String, Cluster> clusters;
    public SharedData sharedData;
    private byte[] avroBytes;
    
    public CenterServer() {
        this.avroBytes = new byte[1];
        this.pathToFile = new ConcurrentHashMap<>();
        this.hashToFile = new ConcurrentHashMap<>();
        this.clusters = new ConcurrentHashMap<>();
    }
    
    public static byte[] computeAvroBytes(Collection<FileObject> elements) throws IOException {
        AvroEncoder encoder = new AvroEncoder();
        encoder.setElements(elements.size());
        for (FileObject file : elements) {
            encoder.setString(file.path);
            encoder.setString(file.hash);
            encoder.setLong(file.size);
            encoder.setLong(file.lastModified);
        }
        encoder.setEnd();
        byte[] bytes = encoder.byteStream.toByteArray();
        encoder.byteStream.close();
        return Zstd.compress(bytes);
    }
    
    public List<FileObject> getFiles() {
        return this.sharedData.fileStorageHelper.getItem();
    }
    
    public void update() throws IOException {
        updateDictionary();
        refreshAvroBytes();
    }
    
    public void updateDictionary() {
        synchronized (this.pathToFile) {
            this.pathToFile.clear();
            for (FileObject file : this.sharedData.fileStorageHelper.getItem()) {
                this.pathToFile.put(file.path, file);
                this.hashToFile.put(file.hash, file);
            }
        }
    }
    
    public byte[] getAvroBytes() {
        return this.avroBytes;
    }
    
    public void refreshAvroBytes() throws IOException {
        this.avroBytes = computeAvroBytes(this.sharedData.fileStorageHelper.getItem());
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
        return Utils.getUrl(file, cluster, sign);
    }
    
    public Cluster chooseOneCluster() {
        // 从 this.onlineClusters 中随机选择一个并返回
        return Utils.weightedRandom(this.getOnlineClusters(), cluster -> cluster.measureBandwidth);
    }
    
    public void tryEnable(String id) throws Exception {
        Cluster cluster = this.clusters.get(id);
        if (cluster == null) throw new Exception("cluster not found");
        boolean isValid = true;
        Exception exception = null;
        
        // 测速
        double bandwidth = Utils.measureCluster(cluster, 20);
        if (bandwidth < 10) {
            throw new Exception("测速失败: 带宽小于 10Mbps，实际测量值为 " + String.format("%.2f", bandwidth) + "Mbps");
        }
        cluster.measureBandwidth = (int) bandwidth;
        
        for (int i = 0; i < 8; i++) {
            FileObject file = Utils.random(sharedData.fileStorageHelper.getItem());
            String sign;
            String url = null;
            if (file != null) {
                sign = Utils.getSign(file, cluster);
                url = Utils.getUrl(file, cluster, sign);
            }
            try {
                Thread.sleep(3000);
                isValid = Utils.checkCluster(url, file);
                if (!isValid) {
                    break;
                }
            } catch (Exception ex) {
                exception = ex;
                isValid = false;
                break;
            }
        }
        if (isValid) {
            cluster.isOnline = true;
        } else {
            if (exception != null) {
                throw new Exception("Unable to download files from the cluster: " + exception.getMessage());
            }
        }
    }
    
    public Stream<Cluster> getOnlineClusters() {
        return this.clusters.values().stream().filter(cluster -> cluster.isOnline);
    }
    
    public void check() {
        ArrayList<FileObject> invalidFiles = new ArrayList<>();
        for (FileObject fileObject : this.sharedData.fileStorageHelper.getItem()) {
            File file = Path.of(SharedData.config.getItem().filePath, fileObject.filePath).toFile();
            // 检查文件是否存在
            if (!file.exists()){
                Logger.logger.logLine("File not found: " + fileObject.filePath);
                // 从文件存储中删除
                invalidFiles.add(fileObject);
            }
        }
        invalidFiles.forEach(fileObject -> this.sharedData.fileStorageHelper.getItem().removeIf(f -> f.path.equals(fileObject.path)));
    }
}
